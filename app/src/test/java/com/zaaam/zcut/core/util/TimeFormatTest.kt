package com.zaaam.zcut.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {

    @Test
    fun formatsSubMinuteDurations() {
        assertEquals("0:00", TimeFormat.clock(0L))
        assertEquals("0:00", TimeFormat.clock(999L))
        assertEquals("0:01", TimeFormat.clock(1_000L))
        assertEquals("0:59", TimeFormat.clock(59_999L))
    }

    @Test
    fun formatsMinutesAndSeconds() {
        assertEquals("1:00", TimeFormat.clock(60_000L))
        assertEquals("1:01", TimeFormat.clock(61_000L))
        assertEquals("12:34", TimeFormat.clock(754_000L))
        assertEquals("59:59", TimeFormat.clock(3_599_000L))
    }

    @Test
    fun switchesToHoursAtOneHour() {
        assertEquals("1:00:00", TimeFormat.clock(3_600_000L))
        assertEquals("1:02:03", TimeFormat.clock(3_723_000L))
        assertEquals("10:00:00", TimeFormat.clock(36_000_000L))
    }

    @Test
    fun clampsNegativeInput() {
        assertEquals("0:00", TimeFormat.clock(-1L))
        assertEquals("0:00", TimeFormat.clock(Long.MIN_VALUE))
    }
}
