package com.hallzyx.detengue.core.session

/** Estado de una inspección persistida (PRD §29). */
enum class InspectionStatus { IN_PROGRESS, COMPLETED }

/** Detección persistible: solo metadatos. Nunca la imagen (PRD §67). */
data class DetectionRecord(
    val id: String,
    val objectClass: String,
    val observedState: String,
    val confidence: Float,
    val condition: String,
    val actionCode: String?,
    val createdAtMillis: Long,
)

data class InspectionRecord(
    val id: String,
    val startedAtMillis: Long,
    val completedAtMillis: Long?,
    val initialReviewCount: Int,
    val finalReviewCount: Int?,
    val status: InspectionStatus,
    val modelVersion: String,
    val detections: List<DetectionRecord>,
)

/**
 * Convierte una revisión terminada en lo que se guarda localmente.
 *
 * - `initialReviewCount`: pendientes de la primera referencia según la IA (sin respuestas manuales).
 * - `finalReviewCount`: pendientes del último escaneo, con las respuestas de la persona.
 * - Se guardan las detecciones del ÚLTIMO escaneo (el estado final de la revisión).
 *
 * Devuelve `null` si no hubo ningún escaneo: no hay nada que registrar.
 */
fun InspectionSession.toRecord(
    id: String,
    completedAtMillis: Long,
    modelVersion: String,
    newDetectionId: () -> String,
): InspectionRecord? {
    val last = latest ?: return null
    val assessed = last.assessed
    return InspectionRecord(
        id = id,
        startedAtMillis = startedAtMillis,
        completedAtMillis = completedAtMillis,
        initialReviewCount = initialPendingCount ?: 0,
        finalReviewCount = last.pendingCount,
        status = InspectionStatus.COMPLETED,
        modelVersion = modelVersion,
        detections = assessed.map {
            DetectionRecord(
                id = newDetectionId(),
                objectClass = it.detection.objectClass.code,
                observedState = it.detection.state.code,
                confidence = it.detection.confidence,
                condition = it.condition.name,
                actionCode = it.actionCode?.name,
                createdAtMillis = completedAtMillis,
            )
        },
    )
}
