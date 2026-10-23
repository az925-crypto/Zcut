package com.zaaam.zcut.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeTimeTest {

    private val now = 10_000_000_000L

    @Test
    fun underOneMinuteIsJustNow() {
        assertEquals("baru saja", RelativeTime.format(now, now))
        assertEquals("baru saja", RelativeTime.format(now, now - 59_999L))
    }

    @Test
    fun minutesHoursDaysWeeksMonths() {
        assertEquals("1 mnt lalu", RelativeTime.format(now, now - 60_000L))
        assertEquals("59 mnt lalu", RelativeTime.format(now, now - 59L * 60_000L))
        assertEquals("1 jam lalu", RelativeTime.format(now, now - 3_600_000L))
        assertEquals("23 jam lalu", RelativeTime.format(now, now - 23L * 3_600_000L))
        assertEquals("1 hari lalu", RelativeTime.format(now, now - 24L * 3_600_000L))
        assertEquals("6 hari lalu", RelativeTime.format(now, now - 6L * 86_400_000L))
        assertEquals("1 mgg lalu", RelativeTime.format(now, now - 7L * 86_400_000L))
        assertEquals("4 mgg lalu", RelativeTime.format(now, now - 29L * 86_400_000L))
        assertEquals("1 bln lalu", RelativeTime.format(now, now - 30L * 86_400_000L))
        assertEquals("2 bln lalu", RelativeTime.format(now, now - 60L * 86_400_000L))
    }

    @Test
    fun futureTimestampsFallBackToJustNow() {
        assertEquals("baru saja", RelativeTime.format(now, now + 5_000L))
    }

    @Test
    fun unitBoundariesRoundDownCorrectly() {
        assertEquals("59 mnt lalu", RelativeTime.format(now, now - 3_599_999L))
        assertEquals("23 jam lalu", RelativeTime.format(now, now - 86_399_999L))
        assertEquals("6 hari lalu", RelativeTime.format(now, now - 604_799_999L))
    }
}

class FileSizeFormatTest {

    @Test
    fun formatsBytes() {
        assertEquals("0 B", FileSizeFormat.format(0L))
        assertEquals("512 B", FileSizeFormat.format(512L))
        assertEquals("999 B", FileSizeFormat.format(999L))
    }

    @Test
    fun formatsLargerUnits() {
        assertEquals("1.0 KB", FileSizeFormat.format(1_000L))
        assertEquals("1.5 KB", FileSizeFormat.format(1_500L))
        assertEquals("95.0 KB", FileSizeFormat.format(95_000L))
        assertEquals("1.0 MB", FileSizeFormat.format(1_000_000L))
        assertEquals("2.5 MB", FileSizeFormat.format(2_500_000L))
        assertEquals("1.0 GB", FileSizeFormat.format(1_000_000_000L))
        assertEquals("2.0 GB", FileSizeFormat.format(2_000_000_000L))
    }

    @Test
    fun roundsUpAcrossUnitBoundary() {
        // Would print "1000.0 KB" if the unit were chosen before rounding.
        assertEquals("1.0 MB", FileSizeFormat.format(999_999L))
        assertEquals("999.9 KB", FileSizeFormat.format(999_940L))
    }
}
