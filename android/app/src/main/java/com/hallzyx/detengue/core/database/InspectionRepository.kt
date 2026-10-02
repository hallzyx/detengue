package com.hallzyx.detengue.core.database

import com.hallzyx.detengue.core.session.InspectionRecord
import com.hallzyx.detengue.core.session.InspectionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Fila del historial (PRD §35): "3 → 1 pendientes". */
data class InspectionSummary(
    val id: String,
    val startedAtMillis: Long,
    val initialPending: Int,
    val finalPending: Int?,
    val status: InspectionStatus,
)

class InspectionRepository(private val dao: InspectionDao) {

    val history: Flow<List<InspectionSummary>> = dao.observeAll().map { list ->
        list.map {
            InspectionSummary(
                id = it.id,
                startedAtMillis = it.startedAt,
                initialPending = it.initialReviewCount,
                finalPending = it.finalReviewCount,
                status = runCatching { InspectionStatus.valueOf(it.status) }.getOrDefault(InspectionStatus.IN_PROGRESS),
            )
        }
    }

    suspend fun save(record: InspectionRecord) {
        dao.insertWithDetections(
            inspection = InspectionEntity(
                id = record.id,
                startedAt = record.startedAtMillis,
                completedAt = record.completedAtMillis,
                initialReviewCount = record.initialReviewCount,
                finalReviewCount = record.finalReviewCount,
                status = record.status.name,
                modelVersion = record.modelVersion,
            ),
            detections = record.detections.map {
                DetectionEntity(
                    id = it.id,
                    inspectionId = record.id,
                    objectClass = it.objectClass,
                    observedState = it.observedState,
                    confidence = it.confidence,
                    condition = it.condition,
                    actionCode = it.actionCode,
                    createdAt = it.createdAtMillis,
                )
            },
        )
    }

    suspend fun clear() = dao.deleteAll()
}
