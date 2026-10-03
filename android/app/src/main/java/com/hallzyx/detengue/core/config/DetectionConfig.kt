package com.hallzyx.detengue.core.config

/**
 * Umbrales y constantes del pipeline, centralizados (PRD §44).
 * No hardcodear estos valores en otras partes de la app.
 */
object DetectionConfig {
    /** Confianza mínima de clase para considerar un objeto detectado. */
    const val DETECTION_THRESHOLD = 0.40f

    /** IoU por encima del cual dos cajas de la misma clase se consideran duplicadas. */
    const val NMS_THRESHOLD = 0.45f

    /** Confianza de condición necesaria para afirmar RISK / SAFE (Fase 3). Por debajo → REVIEW. */
    const val RISK_THRESHOLD = 0.70f

    /** Por debajo de esta confianza de objeto el resultado es UNCERTAIN (Fase 3). */
    const val UNCERTAIN_THRESHOLD = 0.50f

    const val MAX_DETECTIONS = 20

    /** Candidatos que se conservan antes de NMS para acotar el coste en gama baja. */
    const val MAX_CANDIDATES_BEFORE_NMS = 100

    /** Cajas más pequeñas que esto (fracción del frame) se descartan por ruido. */
    const val MIN_BOX_SIZE = 0.01f

    /** Versión que se registra con cada inspección. Subirla cada vez que cambie el modelo (ver MODEL_CARD). */
    const val MODEL_VERSION = "detengue-0.5.0"

    const val MODEL_ASSET = "detengue_detector_int8.tflite"
    const val LABELS_ASSET = "detengue_labels.txt"
}
