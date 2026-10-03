package com.hallzyx.detengue.core.sweep

import com.hallzyx.detengue.core.config.DetectionConfig
import com.hallzyx.detengue.core.model.Detection
import com.hallzyx.detengue.core.model.NormalizedBox
import com.hallzyx.detengue.core.rules.Condition
import com.hallzyx.detengue.core.rules.RiskRuleEngine

/** Un objeto que merece revisión, con el mejor fotograma en que se vio ([payload]). */
data class SweepFinding<T>(
    val detection: Detection,
    val payload: T,
    val framesSeen: Int,
    val condition: Condition,
)

private class Track<T>(var best: Detection, var payload: T, var last: NormalizedBox) {
    var seen = 1
}

/**
 * Junta las detecciones de varios fotogramas de un barrido en objetos únicos.
 *
 * - Una detección continúa un "track" si es de la misma clase y su caja se solapa lo bastante con la anterior
 *   (la cámara se mueve, por eso el umbral es bajo).
 * - Un objeto solo cuenta si se vio en varios fotogramas ([minFrames]): una aparición suelta suele ser un falso positivo.
 * - De cada objeto se conserva el fotograma de mayor confianza, para mostrarlo en la galería.
 * - Solo se reportan los que piden atención (REVIEW o RISK): los seguros o dudosos no son un hallazgo.
 *
 * [T] es lo que se guarda por fotograma (en la app, el Bitmap; en los tests, un entero). Sin dependencias de Android.
 */
class SweepTracker<T>(
    private val minFrames: Int = DetectionConfig.SWEEP_MIN_FRAMES,
    private val matchIou: Float = DetectionConfig.SWEEP_MATCH_IOU,
    private val maxFindings: Int = DetectionConfig.SWEEP_MAX_FINDINGS,
    private val engine: RiskRuleEngine = RiskRuleEngine(),
) {
    private val tracks = mutableListOf<Track<T>>()

    fun update(detections: List<Detection>, payload: T) {
        val claimed = mutableSetOf<Track<T>>() // un track no puede absorber dos detecciones del mismo fotograma
        for (detection in detections.sortedByDescending { it.confidence }) {
            val match = tracks
                .filter { it !in claimed && it.best.objectClass == detection.objectClass }
                .maxByOrNull { it.last.iou(detection.box) }
                ?.takeIf { it.last.iou(detection.box) >= matchIou }
            if (match == null) {
                claimed += Track(detection, payload, detection.box).also { tracks += it }
            } else {
                claimed += match
                match.seen++
                match.last = detection.box
                if (detection.confidence > match.best.confidence) {
                    match.best = detection
                    match.payload = payload
                }
            }
        }
    }

    /** Hallazgos hasta ahora: primero los de RIESGO, y dentro de cada grupo los más confiables. */
    fun findings(): List<SweepFinding<T>> = tracks
        .filter { it.seen >= minFrames }
        .map { SweepFinding(it.best, it.payload, it.seen, engine.assess(it.best).condition) }
        .filter { it.condition.needsAttention }
        .sortedWith(compareByDescending<SweepFinding<T>> { it.condition == Condition.RISK }.thenByDescending { it.detection.confidence })
        .take(maxFindings)
}
