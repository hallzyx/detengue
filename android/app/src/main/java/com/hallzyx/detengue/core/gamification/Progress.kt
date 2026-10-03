package com.hallzyx.detengue.core.gamification

/** Avance hacia una insignia. [current] se recorta a [target] para que la barra nunca pase del 100 %. */
data class AchievementProgress(
    val achievement: Achievement,
    val current: Int,
    val target: Int,
) {
    val unlocked: Boolean get() = current >= target
    val remaining: Int get() = (target - current).coerceAtLeast(0)
    val fraction: Float get() = if (target <= 0) 1f else current.toFloat() / target
}

fun GamificationStats.progressOf(achievement: Achievement): AchievementProgress {
    val value = achievement.metric.valueIn(this).coerceIn(0, achievement.target)
    return AchievementProgress(achievement, value, achievement.target)
}

fun GamificationStats.allProgress(): List<AchievementProgress> = Achievement.entries.map { progressOf(it) }

/**
 * La siguiente meta que conviene mostrar: la bloqueada a la que le falta MENOS (lo más alcanzable ahora),
 * luego la de mayor avance y, si empatan, el orden de la enumeración. `null` si ya están todas.
 */
fun nextGoal(stats: GamificationStats): AchievementProgress? =
    stats.allProgress()
        .filterNot { it.unlocked }
        .sortedWith(compareBy<AchievementProgress> { it.remaining }.thenByDescending { it.fraction })
        .firstOrNull()

enum class StreakState {
    /** Sin racha: nunca se revisó o ya pasó una semana entera sin revisar. */
    NOT_STARTED,

    /** Esta semana ya tiene inspección. */
    ACTIVE,

    /** Hay racha, pero esta semana todavía falta revisar (sigue viva hasta el domingo). */
    AT_RISK,
}

fun streakState(stats: GamificationStats): StreakState = when {
    stats.activeThisWeek -> StreakState.ACTIVE
    stats.streakWeeks > 0 -> StreakState.AT_RISK
    else -> StreakState.NOT_STARTED
}

enum class WeekCell {
    /** Semana que forma parte de la racha actual. */
    DONE,

    /** Esta semana, todavía sin inspección. */
    PENDING,

    /** Fuera de la racha actual (no se afirma nada más de esa semana). */
    EMPTY,
}

/**
 * Tira de las últimas [slots] semanas, de la más antigua a la actual (la última celda es siempre esta semana).
 * Solo usa la racha y si esta semana ya se revisó: no inventa semanas que [GamificationStats] no conoce.
 */
fun weekStrip(stats: GamificationStats, slots: Int): List<WeekCell> {
    require(slots >= 1) { "slots debe ser >= 1" }
    val cells = MutableList(slots) { WeekCell.EMPTY }
    val doneSlots: Int
    if (stats.activeThisWeek) {
        doneSlots = minOf(stats.streakWeeks, slots)
    } else {
        cells[slots - 1] = WeekCell.PENDING
        doneSlots = minOf(stats.streakWeeks, slots - 1)
    }
    val lastDone = if (stats.activeThisWeek) slots - 1 else slots - 2
    for (i in 0 until doneSlots) cells[lastDone - i] = WeekCell.DONE
    return cells
}
