package com.hallzyx.detengue.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class InspectionDao {
    @Insert
    abstract suspend fun insertInspection(inspection: InspectionEntity)

    @Insert
    abstract suspend fun insertDetections(detections: List<DetectionEntity>)

    /** Atómico: o se guarda la inspección completa o no se guarda nada. */
    @Transaction
    open suspend fun insertWithDetections(inspection: InspectionEntity, detections: List<DetectionEntity>) {
        insertInspection(inspection)
        insertDetections(detections)
    }

    @Query("SELECT * FROM inspections ORDER BY startedAt DESC")
    abstract fun observeAll(): Flow<List<InspectionEntity>>

    /** Borra todo el historial; las detecciones caen en cascada. */
    @Query("DELETE FROM inspections")
    abstract suspend fun deleteAll()
}
