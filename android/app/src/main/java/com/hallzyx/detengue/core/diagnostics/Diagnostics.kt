package com.hallzyx.detengue.core.diagnostics

import com.hallzyx.detengue.core.model.InferenceTimings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Métricas técnicas locales (PRD §54). Solo para la pantalla de diagnóstico; no salen del dispositivo. */
class Diagnostics {
    val latency = LatencyStats()

    private val _last = MutableStateFlow<InferenceTimings?>(null)
    val last: StateFlow<InferenceTimings?> = _last

    fun record(timings: InferenceTimings) {
        latency.record(timings.totalMs)
        _last.value = timings
    }
}
