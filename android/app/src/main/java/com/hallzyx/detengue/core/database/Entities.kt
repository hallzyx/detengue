package com.hallzyx.detengue.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** PRD §29. Los instantes se guardan como epoch millis. Jamás se guarda la imagen. */
@Entity(tableName = "inspections")
data class InspectionEntity(
    @PrimaryKey val id: String,
    val startedAt: Long,
    val completedAt: Long?,
    val initialReviewCount: Int,
    val finalReviewCount: Int?,
    val status: String,
    val modelVersion: String,
)

@Entity(
    tableName = "detections",
    foreignKeys = [
        ForeignKey(
            entity = InspectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["inspectionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("inspectionId")],
)
data class DetectionEntity(
    @PrimaryKey val id: String,
    val inspectionId: String,
    val objectClass: String,
    val observedState: String,
    val confidence: Float,
    val condition: String,
    val actionCode: String?,
    val createdAt: Long,
)
