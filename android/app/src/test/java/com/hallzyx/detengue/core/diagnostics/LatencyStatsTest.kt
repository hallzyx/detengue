package com.hallzyx.detengue.core.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LatencyStatsTest {
    @Test
    fun noSamplesMeansNoPercentiles() {
        val s = LatencyStats()
        assertNull(s.p50)
        assertNull(s.p95)
        assertEquals(0, s.count)
    }

    @Test
    fun nearestRankPercentiles() {
        val s = LatencyStats()
        (1..20).forEach { s.record(it * 10L) } // 10..200
        assertEquals(100L, s.p50)
        assertEquals(190L, s.p95)
        assertEquals(200L, s.percentile(100.0))
    }

    @Test
    fun singleSampleIsEveryPercentile() {
        val s = LatencyStats()
        s.record(482)
        assertEquals(482L, s.p50)
        assertEquals(482L, s.p95)
    }

    @Test
    fun windowKeepsOnlyTheMostRecentSamples() {
        val s = LatencyStats(capacity = 3)
        listOf(1000L, 10L, 20L, 30L).forEach { s.record(it) }
        assertEquals(3, s.count)
        assertEquals(30L, s.percentile(100.0)) // el 1000 ya salió de la ventana
    }
}
