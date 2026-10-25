package com.zaaam.zcut.core.util

/**
 * Clock labels for the timeline and draft cards. Pure arithmetic so it can be
 * unit-tested on the JVM without Android.
 */
object TimeFormat {

    /** `0:05`, `1:23`, `1:02:03`. Negative input is clamped to zero. */
    fun clock(durationMs: Long): String {
        val totalSeconds = durationMs.coerceAtLeast(0L) / 1_000L
        val seconds = totalSeconds % 60L
        val minutes = (totalSeconds / 60L) % 60L
        val hours = totalSeconds / 3_600L
        return if (hours > 0L) {
            "${hours}:${pad(minutes)}:${pad(seconds)}"
        } else {
            "${minutes}:${pad(seconds)}"
        }
    }

    /** `0:05.240` — same shape as [clock] with millisecond precision. */
    fun clockMillis(durationMs: Long): String {
        val total = durationMs.coerceAtLeast(0L)
        val millis = total % 1_000L
        val totalSeconds = total / 1_000L
        val seconds = totalSeconds % 60L
        val minutes = (totalSeconds / 60L) % 60L
        val hours = totalSeconds / 3_600L
        val base = if (hours > 0L) {
            "${hours}:${pad(minutes)}:${pad(seconds)}"
        } else {
            "${minutes}:${pad(seconds)}"
        }
        return "$base.${millis.toString().padStart(3, '0')}"
    }

    private fun pad(value: Long): String = if (value < 10L) "0$value" else value.toString()
}
