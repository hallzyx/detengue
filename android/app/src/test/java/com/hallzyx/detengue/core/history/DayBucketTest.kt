package com.hallzyx.detengue.core.history

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class DayBucketTest {
    private val lima = ZoneId.of("America/Lima")
    private fun at(day: Int, hour: Int) = ZonedDateTime.of(2026, 10, day, hour, 0, 0, 0, lima).toInstant().toEpochMilli()

    @Test
    fun sameDayIsToday() {
        assertEquals(DayBucket.TODAY, dayBucket(at(2, 9), at(2, 17), lima))
    }

    @Test
    fun previousDayIsYesterday() {
        assertEquals(DayBucket.YESTERDAY, dayBucket(at(1, 23), at(2, 0), lima))
    }

    @Test
    fun olderIsOther() {
        assertEquals(DayBucket.OTHER, dayBucket(at(29, 10), at(2, 10), lima))
    }
}
