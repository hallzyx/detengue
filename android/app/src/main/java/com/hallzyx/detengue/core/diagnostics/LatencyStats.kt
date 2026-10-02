package com.hallzyx.detengue.core.diagnostics

import kotlin.math.ceil

/**
 * Ventana deslizante de latencias para reportar p50/p95 reales (PRD §27, §54).
 * Los objetivos del PRD no se "fingen": esto mide lo que de verdad ocurre en el dispositivo.
 */
class LatencyStats(private val capacity: Int = 50) {
    init {
        require(capacity > 0)
    }

    private val values = ArrayDeque<Long>()

    @Synchronized
    fun record(ms: Long) {
        if (values.size == capacity) values.removeFirst()
        values.addLast(ms)
    }

    @get:Synchronized
    val count: Int get() = values.size

    /** Percentil por el método del rango más cercano. `null` si aún no hay muestras. */
    @Synchronized
    fun percentile(p: Double): Long? {
        if (values.isEmpty()) return null
        require(p in 0.0..100.0)
        val sorted = values.sorted()
        val rank = ceil(p / 100.0 * sorted.size).toInt().coerceIn(1, sorted.size)
        return sorted[rank - 1]
    }

    val p50: Long? get() = percentile(50.0)
    val p95: Long? get() = percentile(95.0)
}
