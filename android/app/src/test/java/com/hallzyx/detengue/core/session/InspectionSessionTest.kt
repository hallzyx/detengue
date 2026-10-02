package com.hallzyx.detengue.core.session

import com.hallzyx.detengue.core.model.Detection
import com.hallzyx.detengue.core.model.NormalizedBox
import com.hallzyx.detengue.core.model.ObjectClass
import com.hallzyx.detengue.core.model.ObservedState
import com.hallzyx.detengue.core.rules.UserAnswer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InspectionSessionTest {

    private fun review() = Detection(ObjectClass.BUCKET, 0.9f, NormalizedBox(0.1f, 0.1f, 0.3f, 0.3f))
    private fun safe() = Detection(
        ObjectClass.BOTTLE, 0.9f, NormalizedBox(0.5f, 0.5f, 0.2f, 0.2f), ObservedState.CLOSED,
    )

    private fun scan(vararg d: Detection) = ScanSnapshot(d.toList())
    private fun empty() = InspectionSession(startedAtMillis = 0)

    @Test
    fun freshSessionHasNoComparison() {
        val s = empty()
        assertNull(s.latest)
        assertNull(s.initialPendingCount)
        assertNull(s.comparison)
    }

    @Test
    fun firstScanAloneHasNoComparison() {
        val s = empty().withScan(scan(review(), review(), review()))
        assertEquals(3, s.initialPendingCount)
        assertNull(s.comparison)
    }

    @Test
    fun rescanShowsBeforeAndAfter() {
        val s = empty()
            .withScan(scan(review(), review(), review()))
            .withScan(scan(review(), safe()))
        val c = s.comparison!!
        assertEquals(3, c.before)
        assertEquals(1, c.after)
        assertEquals(2, c.fixed)
        assertFalse(c.completed)
    }

    @Test
    fun rescanWithNothingPendingCompletesTheReview() {
        val s = empty()
            .withScan(scan(review(), review()))
            .withScan(scan(safe()))
        val c = s.comparison!!
        assertEquals(0, c.after)
        assertTrue(c.completed)
        assertEquals(2, c.fixed)
    }

    @Test
    fun emptyFirstScanIsNotUsedAsBaseline() {
        val s = empty()
            .withScan(scan()) // mal encuadre: nada detectado
            .withScan(scan(review(), review(), review()))
        assertEquals(3, s.initialPendingCount)
        assertNull(s.comparison) // todavía no hay reescaneo posterior a la base

        val after = s.withScan(scan(review()))
        assertEquals(3, after.comparison!!.before)
        assertEquals(1, after.comparison!!.after)
    }

    @Test
    fun morePendingThanBeforeNeverReportsNegativeFixes() {
        val s = empty().withScan(scan(review())).withScan(scan(review(), review(), review()))
        assertEquals(0, s.comparison!!.fixed)
    }

    @Test
    fun answerNoWaterReducesPendingButNotTheAutomaticBaseline() {
        val s = empty()
            .withScan(scan(review(), review()))
            .withAnswer(0, UserAnswer.NO_WATER)
        assertEquals(1, s.latest!!.pendingCount)
        assertEquals(2, s.latest!!.automaticPendingCount)
        assertEquals(2, s.initialPendingCount) // la base no se infla con respuestas manuales
    }

    @Test
    fun answersOnlyAffectTheLatestScan() {
        val s = empty()
            .withScan(scan(review()))
            .withScan(scan(review(), review()))
            .withAnswer(1, UserAnswer.HAS_WATER)
        assertTrue(s.scans[0].answers.isEmpty())
        assertEquals(UserAnswer.HAS_WATER, s.scans[1].answers[1])
    }

    @Test
    fun invalidAnswerIdIsIgnored() {
        val s = empty().withScan(scan(review()))
        assertEquals(s, s.withAnswer(5, UserAnswer.NO_WATER))
        assertEquals(empty(), empty().withAnswer(0, UserAnswer.NO_WATER))
    }
}
