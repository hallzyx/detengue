package com.hallzyx.detengue.core.session

import com.hallzyx.detengue.core.model.Detection
import com.hallzyx.detengue.core.model.NormalizedBox
import com.hallzyx.detengue.core.model.ObjectClass
import com.hallzyx.detengue.core.model.ObservedState
import com.hallzyx.detengue.core.rules.UserAnswer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InspectionRecordTest {

    private fun review() = Detection(ObjectClass.BUCKET, 0.9f, NormalizedBox(0.1f, 0.1f, 0.3f, 0.3f))
    private fun safe() = Detection(
        ObjectClass.BOTTLE, 0.92f, NormalizedBox(0.5f, 0.5f, 0.2f, 0.2f), ObservedState.CLOSED,
    )

    private fun record(session: InspectionSession): InspectionRecord? {
        var n = 0
        return session.toRecord("insp-1", completedAtMillis = 5_000L, modelVersion = "v-test") { "det-${n++}" }
    }

    @Test
    fun sessionWithoutScansProducesNothing() {
        assertNull(record(InspectionSession(startedAtMillis = 1_000L)))
    }

    @Test
    fun beforeAndAfterCountsAreStored() {
        val session = InspectionSession(1_000L)
            .withScan(ScanSnapshot(listOf(review(), review(), review())))
            .withScan(ScanSnapshot(listOf(review(), safe())))
        val r = record(session)!!
        assertEquals(3, r.initialReviewCount)
        assertEquals(1, r.finalReviewCount)
        assertEquals(InspectionStatus.COMPLETED, r.status)
        assertEquals(1_000L, r.startedAtMillis)
        assertEquals(5_000L, r.completedAtMillis)
        assertEquals("v-test", r.modelVersion)
    }

    @Test
    fun onlyTheLatestScanDetectionsAreStoredWithoutImages() {
        val session = InspectionSession(1_000L)
            .withScan(ScanSnapshot(listOf(review(), review(), review())))
            .withScan(ScanSnapshot(listOf(review(), safe())))
        val r = record(session)!!
        assertEquals(2, r.detections.size)
        assertEquals(listOf("det-0", "det-1"), r.detections.map { it.id })

        val bucket = r.detections[0]
        assertEquals("bucket", bucket.objectClass)
        assertEquals("", bucket.observedState)
        assertEquals("REVIEW", bucket.condition)
        assertEquals("CHECK_STANDING_WATER", bucket.actionCode)

        val bottle = r.detections[1]
        assertEquals("closed", bottle.observedState)
        assertEquals("SAFE", bottle.condition)
        assertNull(bottle.actionCode)
    }

    @Test
    fun manualAnswersCountInTheFinalButNotInTheInitialCount() {
        val session = InspectionSession(1_000L)
            .withScan(ScanSnapshot(listOf(review(), review())))
            .withAnswer(0, UserAnswer.NO_WATER)
        val r = record(session)!!
        assertEquals(2, r.initialReviewCount)
        assertEquals(1, r.finalReviewCount)
        assertEquals("SAFE", r.detections[0].condition)
    }
}
