package com.hallzyx.detengue

import android.app.Application
import com.hallzyx.detengue.core.config.DetectionConfig
import com.hallzyx.detengue.core.database.DetengueDatabase
import com.hallzyx.detengue.core.database.InspectionRepository
import com.hallzyx.detengue.core.diagnostics.Diagnostics
import com.hallzyx.detengue.core.inference.DetectorHolder
import com.hallzyx.detengue.core.session.InspectionStore
import com.hallzyx.detengue.core.session.toRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

class DetengueApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val diagnostics = Diagnostics()
    val detectorHolder: DetectorHolder by lazy { DetectorHolder(this, diagnostics) }

    /** Revisión en curso (en memoria). Se persiste en Room al terminarla. */
    val inspectionStore = InspectionStore()

    private val database by lazy { DetengueDatabase.create(this) }
    val inspectionRepository: InspectionRepository by lazy { InspectionRepository(database.inspectionDao()) }

    override fun onCreate() {
        super.onCreate()
        // Carga + calentamiento del modelo en segundo plano: el primer escaneo no paga el arranque.
        detectorHolder.start()
    }

    /**
     * Cierra la revisión y guarda su resumen local (clase, estado, confianza, hora). Nunca imágenes.
     * Si no hubo ningún escaneo no se guarda nada.
     */
    fun finishInspection() {
        val session = inspectionStore.finish() ?: return
        val record = session.toRecord(
            id = UUID.randomUUID().toString(),
            completedAtMillis = System.currentTimeMillis(),
            modelVersion = DetectionConfig.MODEL_VERSION,
            newDetectionId = { UUID.randomUUID().toString() },
        ) ?: return
        appScope.launch { inspectionRepository.save(record) }
    }
}
