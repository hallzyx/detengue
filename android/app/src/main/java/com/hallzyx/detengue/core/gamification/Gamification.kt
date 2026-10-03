package com.hallzyx.detengue.core.gamification

import com.hallzyx.detengue.core.database.InspectionSummary
import com.hallzyx.detengue.core.session.InspectionStatus
import java.time.Instant
import java.time.ZoneId

/**
 * Gamificación local y simple. Todo se calcula del historial que ya existe: no hay red, ni fotos, ni tablas nuevas.
 *
 * Premia ATENDER las alertas, no encontrar riesgo. "Alertas atendidas" = pendientes iniciales (según la IA) menos
 * pendientes finales (después del reescaneo Y de las respuestas de la persona).
 *
 * LIMITACIÓN CONOCIDA: como el conteo final incluye las respuestas, responder "No" a una alerta también la cuenta
 * como atendida, aunque no se haya hecho nada. Medir solo el efecto del reescaneo exigiría guardar un dato nuevo en Room.
 * Aceptable para un demo; no usar como métrica real de impacto.
 */
data class GamificationStats(
    val inspections: Int,
    val resolvedAlerts: Int,
    /** Semanas seguidas (lunes a domingo) con al menos una inspección completada. */
    val streakWeeks: Int,
    val activeThisWeek: Boolean,
) {
    val achievements: Set<Achievement> get() = Achievement.entries.filter { it.isUnlocked(this) }.toSet()

    companion object {
        val EMPTY = GamificationStats(inspections = 0, resolvedAlerts = 0, streakWeeks = 0, activeThisWeek = false)
    }
}

enum class Achievement(private val unlocked: (GamificationStats) -> Boolean) {
    FIRST_INSPECTION({ it.inspections >= 1 }),
    FIRST_RESOLVED({ it.resolvedAlerts >= 1 }),
    FIVE_RESOLVED({ it.resolvedAlerts >= 5 }),
    STREAK_4_WEEKS({ it.streakWeeks >= 4 }),
    ;

    fun isUnlocked(stats: GamificationStats): Boolean = unlocked(stats)
}

/** Qué se ganó al terminar una inspección (se muestra una sola vez al volver al inicio). */
data class InspectionOutcome(
    val resolvedNow: Int,
    val newAchievements: List<Achievement>,
    val streakWeeks: Int,
)

/**
 * La racha NO se rompe hasta que pasa una semana entera sin inspección: si esta semana aún no revisaste pero la
 * anterior sí, la racha sigue viva (se cuenta hacia atrás desde la semana pasada).
 */
fun computeStats(history: List<InspectionSummary>, nowMillis: Long, zone: ZoneId): GamificationStats {
    val completed = history.filter { it.status == InspectionStatus.COMPLETED }
    val resolved = completed.sumOf { (it.initialPending - (it.finalPending ?: it.initialPending)).coerceAtLeast(0) }
    val weeks = completed.map { weekIndex(it.startedAtMillis, zone) }.toSet()
    val current = weekIndex(nowMillis, zone)
    val active = current in weeks
    var week = if (active) current else current - 1
    var streak = 0
    while (week in weeks) {
        streak++
        week--
    }
    return GamificationStats(completed.size, resolved, streak, active)
}

fun newAchievements(before: GamificationStats, after: GamificationStats): List<Achievement> =
    Achievement.entries.filter { it.isUnlocked(after) && !it.isUnlocked(before) }

/** Índice de semana con inicio en lunes (1970-01-01 fue jueves, de ahí el +3). */
internal fun weekIndex(millis: Long, zone: ZoneId): Long {
    val epochDay = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().toEpochDay()
    return Math.floorDiv(epochDay + 3, 7L)
}
