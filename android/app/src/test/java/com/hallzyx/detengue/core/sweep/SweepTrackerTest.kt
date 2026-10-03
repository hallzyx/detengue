package com.hallzyx.detengue.core.sweep

import com.hallzyx.detengue.core.model.Detection
import com.hallzyx.detengue.core.model.NormalizedBox
import com.hallzyx.detengue.core.model.ObjectClass
import com.hallzyx.detengue.core.model.ObservedState
import com.hallzyx.detengue.core.rules.Condition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SweepTrackerTest {
    private fun det(state: ObservedState, conf: Float, x: Float, y: Float = 0.4f, objectClass: ObjectClass = ObjectClass.BUCKET) =
        Detection(objectClass, conf, NormalizedBox(x, y, 0.2f, 0.2f), state)

    @Test
    fun objectSeenInASingleFrameIsNotAFinding() {
        val tracker = SweepTracker<Int>(minFrames = 2)
        tracker.update(listOf(det(ObservedState.WATER, 0.9f, 0.1f)), payload = 1)
        assertTrue(tracker.findings().isEmpty())
    }

    @Test
    fun sameObjectMovingBetweenFramesIsOneFindingWithItsBestFrame() {
        val tracker = SweepTracker<Int>(minFrames = 2)
        tracker.update(listOf(det(ObservedState.WATER, 0.75f, 0.10f)), payload = 1)
        tracker.update(listOf(det(ObservedState.WATER, 0.92f, 0.14f)), payload = 2) // se movió un poco, mejor confianza
        tracker.update(listOf(det(ObservedState.WATER, 0.80f, 0.18f)), payload = 3)
        val findings = tracker.findings()
        assertEquals(1, findings.size)
        assertEquals(2, findings[0].payload)
        assertEquals(3, findings[0].framesSeen)
        assertEquals(Condition.RISK, findings[0].condition)
    }

    @Test
    fun farApartObjectsAreSeparateFindings() {
        val tracker = SweepTracker<Int>(minFrames = 2)
        repeat(2) { tracker.update(listOf(det(ObservedState.WATER, 0.9f, 0.05f), det(ObservedState.OPEN, 0.8f, 0.70f)), payload = it) }
        assertEquals(2, tracker.findings().size)
    }

    @Test
    fun safeAndUncertainObjectsAreNotFindings() {
        val tracker = SweepTracker<Int>(minFrames = 2)
        repeat(3) {
            tracker.update(
                listOf(det(ObservedState.INVERTED, 0.9f, 0.05f), det(ObservedState.WATER, 0.45f, 0.70f)),
                payload = it,
            )
        }
        assertTrue(tracker.findings().isEmpty())
    }

    @Test
    fun riskComesBeforeReviewEvenWithLowerConfidence() {
        val tracker = SweepTracker<Int>(minFrames = 2)
        repeat(2) {
            tracker.update(listOf(det(ObservedState.OPEN, 0.95f, 0.05f), det(ObservedState.WATER, 0.75f, 0.70f)), payload = it)
        }
        assertEquals(listOf(Condition.RISK, Condition.REVIEW), tracker.findings().map { it.condition })
    }

    @Test
    fun differentClassesAtTheSamePlaceAreDifferentTracks() {
        val tracker = SweepTracker<Int>(minFrames = 2)
        repeat(2) {
            tracker.update(
                listOf(
                    det(ObservedState.WATER, 0.9f, 0.3f, objectClass = ObjectClass.BUCKET),
                    det(ObservedState.UNKNOWN, 0.9f, 0.3f, objectClass = ObjectClass.WATER_TANK),
                ),
                payload = it,
            )
        }
        assertEquals(2, tracker.findings().size)
    }

    @Test
    fun findingsAreCapped() {
        val tracker = SweepTracker<Int>(minFrames = 1, maxFindings = 3)
        tracker.update((0 until 6).map { det(ObservedState.WATER, 0.9f - it * 0.01f, x = it * 0.15f, y = 0.1f) }, payload = 1)
        assertEquals(3, tracker.findings().size)
    }
}
