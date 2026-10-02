package com.hallzyx.detengue.core.inference

import com.hallzyx.detengue.core.config.DetectionConfig
import com.hallzyx.detengue.core.model.Detection
import com.hallzyx.detengue.core.model.NormalizedBox
import com.hallzyx.detengue.core.model.ObjectClass

/**
 * Decodifica la salida de un detector estilo YOLO (Ultralytics) exportado a TFLite y aplica NMS.
 *
 * Soporta ambos layouts habituales, detectándolo por la forma del tensor:
 *  - canal-primero  `[1, 4 + nc, N]`
 *  - canal-último   `[1, N, 4 + nc]`
 * Cada ancla es `cx, cy, w, h, score_clase_0 … score_clase_nc-1`. Las coordenadas pueden venir
 * normalizadas (0..1) o en píxeles de la entrada; se detecta automáticamente.
 *
 * [classes] mapea el índice de clase del modelo a [ObjectClass]; un `null` marca una clase que la
 * app ignora (p. ej. una clase de apoyo usada solo en entrenamiento).
 *
 * Es lógica pura (sin Android) para poder probarla de forma determinista.
 */
class YoloPostprocessor(
    private val classes: List<ObjectClass?>,
    private val detectionThreshold: Float = DetectionConfig.DETECTION_THRESHOLD,
    private val nmsThreshold: Float = DetectionConfig.NMS_THRESHOLD,
    private val maxDetections: Int = DetectionConfig.MAX_DETECTIONS,
    private val maxCandidates: Int = DetectionConfig.MAX_CANDIDATES_BEFORE_NMS,
    private val minBoxSize: Float = DetectionConfig.MIN_BOX_SIZE,
) {
    private class Candidate(
        val objectClass: ObjectClass,
        val score: Float,
        val cx: Float,
        val cy: Float,
        val w: Float,
        val h: Float,
    )

    fun process(
        output: FloatArray,
        shape: IntArray,
        inputSize: Int,
        transform: LetterboxTransform,
    ): List<Detection> {
        require(shape.size >= 2) { "Forma de salida inesperada: ${shape.joinToString()}" }
        val d0 = shape[shape.size - 2]
        val d1 = shape[shape.size - 1]
        val channels = 4 + classes.size

        val channelFirst = when {
            d0 == channels -> true
            d1 == channels -> false
            else -> throw IllegalArgumentException(
                "La salida ${shape.joinToString("x")} no coincide con 4 + ${classes.size} clases",
            )
        }
        val anchors = if (channelFirst) d1 else d0
        require(output.size >= d0 * d1) { "Salida más corta de lo que indica su forma" }

        fun value(channel: Int, anchor: Int): Float =
            if (channelFirst) output[channel * anchors + anchor] else output[anchor * channels + channel]

        // 1) Candidatos por encima del umbral (mejor clase conocida de cada ancla).
        val candidates = ArrayList<Candidate>()
        for (a in 0 until anchors) {
            var bestScore = 0f
            var bestClass: ObjectClass? = null
            for (k in classes.indices) {
                val cls = classes[k] ?: continue
                val s = value(4 + k, a)
                if (s > bestScore) {
                    bestScore = s
                    bestClass = cls
                }
            }
            if (bestClass != null && bestScore >= detectionThreshold) {
                candidates += Candidate(
                    bestClass, bestScore,
                    value(0, a), value(1, a), value(2, a), value(3, a),
                )
            }
        }
        if (candidates.isEmpty()) return emptyList()

        candidates.sortByDescending { it.score }
        val limited = if (candidates.size > maxCandidates) candidates.subList(0, maxCandidates) else candidates

        // 2) ¿Coordenadas normalizadas o en píxeles?
        val maxCoord = limited.maxOf { maxOf(it.cx, it.cy, it.w, it.h) }
        val toPixels = if (maxCoord <= NORMALIZED_MAX) inputSize.toFloat() else 1f

        // 3) A coordenadas normalizadas del frame original (deshaciendo el letterbox).
        val boxed = ArrayList<Detection>(limited.size)
        for (c in limited) {
            val x1 = transform.unmapX((c.cx - c.w / 2f) * toPixels)
            val y1 = transform.unmapY((c.cy - c.h / 2f) * toPixels)
            val x2 = transform.unmapX((c.cx + c.w / 2f) * toPixels)
            val y2 = transform.unmapY((c.cy + c.h / 2f) * toPixels)
            val box = NormalizedBox(x1, y1, x2 - x1, y2 - y1)
            if (box.width < minBoxSize || box.height < minBoxSize) continue
            boxed += Detection(c.objectClass, c.score, box)
        }

        // 4) NMS por clase, voraz, ya ordenado por confianza.
        val kept = ArrayList<Detection>()
        for (d in boxed) {
            val duplicate = kept.any { it.objectClass == d.objectClass && it.box.iou(d.box) > nmsThreshold }
            if (!duplicate) {
                kept += d
                if (kept.size >= maxDetections) break
            }
        }
        return kept
    }

    private companion object {
        /** Si ninguna coordenada supera esto, el modelo emitió valores normalizados. */
        const val NORMALIZED_MAX = 1.5f
    }
}
