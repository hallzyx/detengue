package com.hallzyx.detengue.core.model

import kotlin.math.max
import kotlin.math.min

/** Caja con coordenadas normalizadas 0..1 respecto al frame original, origen arriba-izquierda (PRD §30). */
data class NormalizedBox(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
) {
    val right: Float get() = x + width
    val bottom: Float get() = y + height
    val area: Float get() = max(0f, width) * max(0f, height)

    fun iou(other: NormalizedBox): Float {
        val interW = min(right, other.right) - max(x, other.x)
        val interH = min(bottom, other.bottom) - max(y, other.y)
        if (interW <= 0f || interH <= 0f) return 0f
        val inter = interW * interH
        val union = area + other.area - inter
        return if (union <= 0f) 0f else inter / union
    }
}

/**
 * Un objeto localizado por el modelo. [confidence] es la confianza de la clase completa
 * (objeto + [state]). La interpretación de riesgo es responsabilidad del motor de reglas.
 */
data class Detection(
    val objectClass: ObjectClass,
    val confidence: Float,
    val box: NormalizedBox,
    val state: ObservedState = ObservedState.UNKNOWN,
)

/** Tiempos por etapa, en milisegundos (PRD §54). */
data class InferenceTimings(
    val preprocessMs: Long,
    val inferenceMs: Long,
    val postprocessMs: Long,
) {
    val totalMs: Long get() = preprocessMs + inferenceMs + postprocessMs
}

data class DetectionResult(
    val detections: List<Detection>,
    val timings: InferenceTimings,
    val frameWidth: Int,
    val frameHeight: Int,
)
