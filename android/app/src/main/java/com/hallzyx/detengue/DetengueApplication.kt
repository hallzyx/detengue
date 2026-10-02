package com.hallzyx.detengue

import android.app.Application
import com.hallzyx.detengue.core.inference.DetectorHolder

class DetengueApplication : Application() {
    val detectorHolder: DetectorHolder by lazy { DetectorHolder(this) }

    override fun onCreate() {
        super.onCreate()
        // Carga + calentamiento del modelo en segundo plano: el primer escaneo no paga el arranque.
        detectorHolder.start()
    }
}
