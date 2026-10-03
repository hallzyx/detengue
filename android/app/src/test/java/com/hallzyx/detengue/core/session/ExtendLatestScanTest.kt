package com.hallzyx.detengue.core.session

import com.hallzyx.detengue.core.model.Detection
import com.hallzyx.detengue.core.model.NormalizedBox
import com.hallzyx.detengue.core.model.ObjectClass
import com.hallzyx.detengue.core.model.ObservedState
import com.hallzyx.detengue.core.rules.Condition
import com.hallzyx.detengue.core.rules.UserAnswer
import org.junit.Assert.assertEquals
import org.junit.Test

class ExtendLatestScanTest {
    private fun det(state: ObservedState, x: Float) =
        Detection(ObjectClass.BUCKET, 0.8f, NormalizedBox(x, 0.3f, 0.2f, 0.2f), state)

    @Test
    fun extendWithoutScansCreatesOne() {
        val store = InspectionStore(clock = { 0L })
        store.begin()
        val snapshot = store.extendLatestScan(listOf(det(ObservedState.OPEN, 0.1f)))
        assertEquals(1, snapshot.detections.size)
        assertEquals(1, store.session.value?.scans?.size)
    }

    @Test
    fun extendAppendsAndKeepsPreviousAnswers() {
        val store = InspectionStore(clock = { 0L })
        store.begin()
        store.recordScan(listOf(det(ObservedState.OPEN, 0.1f), det(ObservedState.OPEN, 0.6f)))
        store.answer(0, UserAnswer.HAS_WATER)

        val snapshot = store.extendLatestScan(listOf(det(ObservedState.WATER, 0.35f)))

        assertEquals(3, snapshot.detections.size)
        assertEquals(1, store.session.value?.scans?.size) // sigue siendo UN escaneo: no es un reescaneo
        assertEquals(Condition.RISK, snapshot.assessed[0].condition) // la respuesta Sí del objeto 0 se conservó
        assertEquals(Condition.REVIEW, snapshot.assessed[1].condition)
        assertEquals(Condition.RISK, snapshot.assessed[2].condition)
    }

    @Test
    fun extendingTwiceAccumulatesInOrder() {
        val session = InspectionSession(startedAtMillis = 0L)
            .withExtendedLatestScan(listOf(det(ObservedState.OPEN, 0.1f)))
            .withExtendedLatestScan(listOf(det(ObservedState.WATER, 0.5f), det(ObservedState.INVERTED, 0.8f)))
        assertEquals(listOf(ObservedState.OPEN, ObservedState.WATER, ObservedState.INVERTED), session.latest?.detections?.map { it.state })
    }
}
