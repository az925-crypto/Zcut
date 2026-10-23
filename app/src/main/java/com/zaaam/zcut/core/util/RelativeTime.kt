package com.zaaam.zcut.core.util

import java.util.Locale

/**
 * Relative "edited" labels for draft cards. Pure integer maths (no locale or
 * timezone formatting) so unit tests are deterministic.
 */
object RelativeTime {

    private const val MINUTE = 60_000L
    private const val HOUR = 60L * MINUTE
    private const val DAY = 24L * HOUR
    private const val WEEK = 7L * DAY
    private const val MONTH = 30L * DAY

    fun format(nowMs: Long, thenMs: Long): String {
        val delta = nowMs - thenMs
        return when {
            delta < MINUTE -> "baru saja"
            delta < HOUR -> "${delta / MINUTE} mnt lalu"
            delta < DAY -> "${delta / HOUR} jam lalu"
            delta < WEEK -> "${delta / DAY} hari lalu"
            delta < MONTH -> "${delta / WEEK} mgg lalu"
            else -> "${(delta / MONTH).coerceAtLeast(1L)} bln lalu"
        }
    }

    fun formatFileSize(bytes: Long): String = FileSizeFormat.format(bytes)
}

/** Human readable byte sizes, e.g. `1.5 MB`. */
object FileSizeFormat {
    private val UNITS = arrayOf("B", "KB", "MB", "GB", "TB")

    fun format(bytes: Long): String {
        if (bytes < 1_000L) return "$bytes B"
        var value = bytes.toDouble()
        var unit = 0
        // Advance on the rounded value so 999_999 reads "1.0 MB" instead of
        // "1000.0 KB".
        while (unit < UNITS.lastIndex && round1(value) >= 1000.0) {
            value /= 1000.0
            unit++
        }
        return String.format(Locale.US, "%.1f %s", value, UNITS[unit])
    }

    private fun round1(value: Double): Double = Math.round(value * 10.0) / 10.0
}
