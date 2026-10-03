package com.hallzyx.detengue.core.gamification

import com.hallzyx.detengue.core.gamification.WeekCell.DONE
import com.hallzyx.detengue.core.gamification.WeekCell.EMPTY
import com.hallzyx.detengue.core.gamification.WeekCell.PENDING
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressTest {
    private fun stats(inspections: Int = 0, resolved: Int = 0, streak: Int = 0, active: Boolean = false) =
        GamificationStats(inspections, resolved, streak, active)

    @Test
    fun progressIsClampedToTarget() {
        val p = stats(resolved = 9).progressOf(Achievement.FIVE_RESOLVED)
        assertEquals(5, p.current)
        assertEquals(0, p.remaining)
        assertEquals(1f, p.fraction, 0f)
        assertTrue(p.unlocked)
    }

    @Test
    fun progressCountsWhatIsMissing() {
        val p = stats(resolved = 2).progressOf(Achievement.FIVE_RESOLVED)
        assertEquals(2, p.current)
        assertEquals(3, p.remaining)
        assertEquals(0.4f, p.fraction, 1e-6f)
        assertFalse(p.unlocked)
    }

    @Test
    fun progressUnlockedMatchesAchievementRule() {
        val samples = listOf(stats(), stats(1, 0, 1, true), stats(3, 4, 3, false), stats(9, 5, 4, true))
        for (s in samples) {
            for (a in Achievement.entries) assertEquals(a.isUnlocked(s), s.progressOf(a).unlocked)
        }
    }

    @Test
    fun nextGoalForNewUserIsFirstInspection() {
        assertEquals(Achievement.FIRST_INSPECTION, nextGoal(GamificationStats.EMPTY)?.achievement)
    }

    @Test
    fun nextGoalPrefersTheClosestOne() {
        // Falta 1 alerta para la primera vs 3 semanas de racha: la alerta está más cerca
        assertEquals(Achievement.FIRST_RESOLVED, nextGoal(stats(1, 0, 1, true))?.achievement)
        // Faltan 4 alertas vs 1 semana: la racha está más cerca
        assertEquals(Achievement.STREAK_4_WEEKS, nextGoal(stats(5, 1, 3, true))?.achievement)
    }

    @Test
    fun nextGoalTieBreaksByProgressThenOrder() {
        // Faltan 2 alertas (3/5 = 60 %) y 2 semanas (2/4 = 50 %): gana la de más avance
        assertEquals(Achievement.FIVE_RESOLVED, nextGoal(stats(3, 3, 2, true))?.achievement)
    }

    @Test
    fun nextGoalIsNullWhenEverythingIsUnlocked() {
        assertNull(nextGoal(stats(9, 5, 4, true)))
    }

    @Test
    fun streakStateReflectsThisWeek() {
        assertEquals(StreakState.NOT_STARTED, streakState(GamificationStats.EMPTY))
        assertEquals(StreakState.ACTIVE, streakState(stats(1, 0, 1, true)))
        assertEquals(StreakState.AT_RISK, streakState(stats(2, 0, 2, false)))
    }

    @Test
    fun weekStripForNewUserOnlyShowsThisWeekPending() {
        assertEquals(listOf(EMPTY, EMPTY, EMPTY, EMPTY, PENDING), weekStrip(GamificationStats.EMPTY, 5))
    }

    @Test
    fun weekStripActiveEndsInThisWeek() {
        assertEquals(listOf(EMPTY, EMPTY, DONE, DONE, DONE), weekStrip(stats(3, 0, 3, true), 5))
    }

    @Test
    fun weekStripAtRiskKeepsThisWeekPending() {
        assertEquals(listOf(EMPTY, EMPTY, DONE, DONE, PENDING), weekStrip(stats(2, 0, 2, false), 5))
    }

    @Test
    fun weekStripLongStreakFillsEverySlot() {
        assertEquals(List(5) { DONE }, weekStrip(stats(10, 0, 10, true), 5))
        assertEquals(listOf(DONE, DONE, DONE, DONE, PENDING), weekStrip(stats(10, 0, 10, false), 5))
    }

    @Test
    fun weekStripWithASingleSlotIsThisWeek() {
        assertEquals(listOf(PENDING), weekStrip(stats(4, 0, 4, false), 1))
        assertEquals(listOf(DONE), weekStrip(stats(4, 0, 4, true), 1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun weekStripRejectsZeroSlots() {
        weekStrip(GamificationStats.EMPTY, 0)
    }

    @Test
    fun sweptZonesIsAbsentByDefault() {
        // El dato de barrido por zonas aún no existe: no se inventa
        assertNull(GamificationStats.EMPTY.sweptZones)
        assertNull(stats(3, 2, 1, true).sweptZones)
    }
}
