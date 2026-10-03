package com.hallzyx.detengue.core.gamification

import com.hallzyx.detengue.core.database.InspectionSummary
import com.hallzyx.detengue.core.session.InspectionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class GamificationTest {
    private val lima = ZoneId.of("America/Lima")

    // 2026-10-05 es lunes. Semana A = 5-11 oct; semana anterior = 28 sep-4 oct; y así hacia atrás.
    private fun at(month: Int, day: Int) = ZonedDateTime.of(2026, month, day, 10, 0, 0, 0, lima).toInstant().toEpochMilli()

    private fun inspection(
        month: Int,
        day: Int,
        initial: Int = 2,
        final: Int? = 0,
        status: InspectionStatus = InspectionStatus.COMPLETED,
    ) = InspectionSummary(id = "$month-$day", startedAtMillis = at(month, day), initialPending = initial, finalPending = final, status = status)

    @Test
    fun emptyHistoryHasNoStats() {
        assertEquals(GamificationStats.EMPTY, computeStats(emptyList(), at(10, 8), lima))
    }

    @Test
    fun resolvedAlertsAreInitialMinusFinal() {
        val stats = computeStats(listOf(inspection(10, 6, initial = 3, final = 1), inspection(10, 7, initial = 2, final = 0)), at(10, 8), lima)
        assertEquals(4, stats.resolvedAlerts)
        assertEquals(2, stats.inspections)
    }

    @Test
    fun resolvedNeverGoesNegativeAndMissingFinalCountsAsZero() {
        val stats = computeStats(listOf(inspection(10, 6, initial = 1, final = 3), inspection(10, 7, initial = 2, final = null)), at(10, 8), lima)
        assertEquals(0, stats.resolvedAlerts)
    }

    @Test
    fun inProgressInspectionsDoNotCount() {
        val stats = computeStats(listOf(inspection(10, 6, status = InspectionStatus.IN_PROGRESS)), at(10, 8), lima)
        assertEquals(0, stats.inspections)
        assertEquals(0, stats.streakWeeks)
    }

    @Test
    fun twoInspectionsInTheSameWeekAreOneWeekOfStreak() {
        val stats = computeStats(listOf(inspection(10, 5), inspection(10, 11)), at(10, 12), lima)
        // el domingo 11 sigue en la misma semana que el lunes 5; el lunes 12 ya es otra
        assertEquals(1, stats.streakWeeks)
        assertFalse(stats.activeThisWeek)
    }

    @Test
    fun consecutiveWeeksBuildAStreak() {
        val history = listOf(inspection(9, 14), inspection(9, 22), inspection(9, 30), inspection(10, 6))
        val stats = computeStats(history, at(10, 8), lima)
        assertEquals(4, stats.streakWeeks)
        assertTrue(stats.activeThisWeek)
    }

    @Test
    fun streakSurvivesAWeekWithoutInspectionYet() {
        // Esta semana (5-11 oct) aún no se revisó, pero la anterior sí: la racha sigue viva
        val stats = computeStats(listOf(inspection(9, 22), inspection(9, 30)), at(10, 8), lima)
        assertEquals(2, stats.streakWeeks)
        assertFalse(stats.activeThisWeek)
    }

    @Test
    fun streakBreaksAfterAFullWeekWithoutInspection() {
        // Última inspección el 22 sep; hoy es 8 oct: pasó la semana del 28 sep entera sin revisar
        val stats = computeStats(listOf(inspection(9, 22)), at(10, 8), lima)
        assertEquals(0, stats.streakWeeks)
    }

    @Test
    fun weekStartsOnMonday() {
        // domingo 4 oct y lunes 5 oct caen en semanas distintas
        val stats = computeStats(listOf(inspection(10, 4), inspection(10, 5)), at(10, 6), lima)
        assertEquals(2, stats.streakWeeks)
    }

    @Test
    fun achievementsUnlockFromStats() {
        assertTrue(GamificationStats.EMPTY.achievements.isEmpty())
        assertEquals(setOf(Achievement.FIRST_INSPECTION), GamificationStats(1, 0, 1, true).achievements)
        assertEquals(
            setOf(Achievement.FIRST_INSPECTION, Achievement.FIRST_RESOLVED, Achievement.FIVE_RESOLVED, Achievement.STREAK_4_WEEKS),
            GamificationStats(9, 5, 4, true).achievements,
        )
    }

    @Test
    fun newAchievementsAreOnlyThoseNotYetUnlocked() {
        val before = GamificationStats(1, 0, 1, true)
        val after = GamificationStats(2, 1, 1, true)
        assertEquals(listOf(Achievement.FIRST_RESOLVED), newAchievements(before, after))
        assertTrue(newAchievements(after, after).isEmpty())
    }

    @Test
    fun achievementsAreUnlockedExactlyAtTheirTarget() {
        for (a in Achievement.entries) {
            val below = statsWith(a.metric, a.target - 1)
            val at = statsWith(a.metric, a.target)
            assertFalse(a.isUnlocked(below))
            assertTrue(a.isUnlocked(at))
        }
    }

    @Test
    fun outcomeCarriesStatsAfterAndNewBadges() {
        val before = GamificationStats(0, 0, 0, false)
        val after = GamificationStats(1, 2, 1, true)
        val outcome = inspectionOutcome(before, after, resolvedNow = 2)
        assertEquals(2, outcome.resolvedNow)
        assertEquals(1, outcome.streakWeeks)
        assertEquals(after, outcome.stats)
        assertEquals(listOf(Achievement.FIRST_INSPECTION, Achievement.FIRST_RESOLVED), outcome.newAchievements)
    }

    @Test
    fun outcomeNeverReportsNegativeResolved() {
        assertEquals(0, inspectionOutcome(GamificationStats.EMPTY, GamificationStats.EMPTY, resolvedNow = -3).resolvedNow)
    }

    private fun statsWith(metric: GoalMetric, value: Int) = when (metric) {
        GoalMetric.INSPECTIONS -> GamificationStats(value, 0, 0, false)
        GoalMetric.RESOLVED_ALERTS -> GamificationStats(0, value, 0, false)
        GoalMetric.STREAK_WEEKS -> GamificationStats(0, 0, value, false)
    }
}
