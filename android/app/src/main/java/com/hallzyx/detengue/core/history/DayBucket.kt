package com.hallzyx.detengue.core.history

import java.time.Instant
import java.time.ZoneId

/** Cómo se rotula el día de una revisión en el historial: "Hoy", "Ayer" o una fecha. */
enum class DayBucket { TODAY, YESTERDAY, OTHER }

fun dayBucket(startedAtMillis: Long, nowMillis: Long, zone: ZoneId): DayBucket {
    val day = Instant.ofEpochMilli(startedAtMillis).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    return when (day) {
        today -> DayBucket.TODAY
        today.minusDays(1) -> DayBucket.YESTERDAY
        else -> DayBucket.OTHER
    }
}
