package com.hallzyx.detengue

import android.app.Application
import com.hallzyx.detengue.core.inference.DetectorHolder
import com.hallzyx.detengue.core.session.InspectionStore

class DetengueApplication : Application() {
    val detectorHolder: DetectorHolder by lazy { DetectorHolder(this) }

    /** Revisión en curso (en memoria). La Fase 4 la persiste en Room al terminar. */
    val inspectionStore = InspectionStore()

    override fun onCreate() {
        super.onCreate()
        // Carga + calentamiento del modelo en segundo plano: el primer escaneo no paga el arranque.
        detectorHolder.start()
    }
}
