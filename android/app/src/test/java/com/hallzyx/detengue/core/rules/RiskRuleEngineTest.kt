package com.hallzyx.detengue.core.rules

import com.hallzyx.detengue.core.model.Detection
import com.hallzyx.detengue.core.model.NormalizedBox
import com.hallzyx.detengue.core.model.ObjectClass
import com.hallzyx.detengue.core.model.ObservedState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskRuleEngineTest {

    private val engine = RiskRuleEngine()

    private fun det(cls: ObjectClass, conf: Float, state: ObservedState = ObservedState.UNKNOWN) =
        Detection(cls, conf, NormalizedBox(0.1f, 0.1f, 0.3f, 0.3f), state)

    private fun assess(cls: ObjectClass, conf: Float, state: ObservedState, answer: UserAnswer? = null) =
        engine.assess(det(cls, conf, state), answer)

    @Test
    fun confidentWaterIsRiskWithEmptyAction() {
        val a = assess(ObjectClass.BUCKET, 0.90f, ObservedState.WATER)
        assertEquals(Condition.RISK, a.condition)
        assertEquals(ActionCode.EMPTY_AND_INVERT, a.actionCode)
    }

    @Test
    fun lessConfidentWaterOnlyAsksForReview() {
        val a = assess(ObjectClass.BUCKET, 0.60f, ObservedState.WATER)
        assertEquals(Condition.REVIEW, a.condition)
        assertEquals(ActionCode.CHECK_STANDING_WATER, a.actionCode)
    }

    @Test
    fun closedOrInvertedAreSafeOnlyWhenConfident() {
        assertEquals(Condition.SAFE, assess(ObjectClass.BOTTLE, 0.90f, ObservedState.CLOSED).condition)
        assertEquals(Condition.SAFE, assess(ObjectClass.BUCKET, 0.85f, ObservedState.INVERTED).condition)
        assertEquals(Condition.REVIEW, assess(ObjectClass.BOTTLE, 0.60f, ObservedState.CLOSED).condition)
    }

    @Test
    fun lowConfidenceIsUncertainWhateverTheState() {
        for (state in ObservedState.entries) {
            val a = assess(ObjectClass.BUCKET, 0.45f, state)
            assertEquals(Condition.UNCERTAIN, a.condition)
            assertEquals(ActionCode.MANUAL_REVIEW, a.actionCode)
        }
    }

    @Test
    fun unknownStateAlwaysFallsBackToReview() {
        val a = assess(ObjectClass.BUCKET, 0.95f, ObservedState.UNKNOWN)
        assertEquals(Condition.REVIEW, a.condition)
        assertEquals(ActionCode.CHECK_STANDING_WATER, a.actionCode)
    }

    @Test
    fun uncoveredTankOrDrumIsRiskButOpenBucketIsOnlyReview() {
        assertEquals(Condition.RISK, assess(ObjectClass.WATER_TANK, 0.90f, ObservedState.OPEN).condition)
        assertEquals(ActionCode.KEEP_TANK_COVERED, assess(ObjectClass.DRUM, 0.90f, ObservedState.OPEN).actionCode)
        assertEquals(Condition.REVIEW, assess(ObjectClass.BUCKET, 0.90f, ObservedState.OPEN).condition)
    }

    @Test
    fun actionDependsOnTheObject() {
        assertEquals(ActionCode.REMOVE_POT_WATER, assess(ObjectClass.PLANT_POT, 0.9f, ObservedState.WATER).actionCode)
        assertEquals(ActionCode.REFRESH_PET_WATER, assess(ObjectClass.PET_BOWL, 0.9f, ObservedState.WATER).actionCode)
        assertEquals(ActionCode.PROTECT_TIRE, assess(ObjectClass.TIRE, 0.9f, ObservedState.WATER).actionCode)
    }

    @Test
    fun userAnswerOverridesTheModel() {
        // La IA ve una botella cerrada con confianza, pero la persona dice que tiene agua.
        val yes = assess(ObjectClass.BOTTLE, 0.90f, ObservedState.CLOSED, UserAnswer.HAS_WATER)
        assertEquals(Condition.RISK, yes.condition)
        assertEquals(ActionCode.EMPTY_AND_INVERT, yes.actionCode)

        val no = assess(ObjectClass.BUCKET, 0.90f, ObservedState.UNKNOWN, UserAnswer.NO_WATER)
        assertEquals(Condition.SAFE, no.condition)

        val unsure = assess(ObjectClass.BUCKET, 0.90f, ObservedState.UNKNOWN, UserAnswer.UNSURE)
        assertEquals(Condition.REVIEW, unsure.condition)
    }

    @Test
    fun onlyReviewAndRiskNeedAttention() {
        assertTrue(Condition.REVIEW.needsAttention)
        assertTrue(Condition.RISK.needsAttention)
        assertFalse(Condition.SAFE.needsAttention)
        assertFalse(Condition.UNCERTAIN.needsAttention)
    }

    @Test
    fun assessAllKeepsIndexesAndAppliesAnswers() {
        val list = listOf(
            det(ObjectClass.BUCKET, 0.9f),
            det(ObjectClass.BOTTLE, 0.9f, ObservedState.CLOSED),
        )
        val result = engine.assessAll(list, mapOf(0 to UserAnswer.NO_WATER))
        assertEquals(listOf(0, 1), result.map { it.id })
        assertEquals(Condition.SAFE, result[0].condition)
        assertEquals(UserAnswer.NO_WATER, result[0].userAnswer)
        assertEquals(Condition.SAFE, result[1].condition)
    }
}
