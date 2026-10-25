package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.ExportFormat
import com.zaaam.zcut.core.model.ExportPreset
import java.util.Locale

/**
 * Pure export helpers: the output file name and the numbers the export screen
 * shows (estimated size, duration, container label). Kept free of Android types
 * so the values shown to the user can be unit-tested without a device.
 */
object ExportPlan {

    private const val EXTENSION = "mp4"
    private const val COVER_EXTENSION = "jpg"
    private const val MAX_NAME_LENGTH = 40
    private const val FALLBACK_NAME = "zcut-export"

    /**
     * Filesystem-safe base name. A draft name is user input, so path separators
     * and control characters are folded into `-` instead of reaching the disk.
     */
    fun safeName(raw: String): String {
        val cleaned = raw.trim()
            .replace(Regex("[^\\p{L}\\p{N}._-]+"), "-")
            .trim('-', '.', '_')
            .take(MAX_NAME_LENGTH)
        return cleaned.ifBlank { FALLBACK_NAME }
    }

    /** `nama-<timestamp>.mp4`; the timestamp keeps two exports of a draft apart. */
    fun fileName(projectName: String, timestampMs: Long): String =
        safeName(projectName) + "-" + stamp(timestampMs) + "." + EXTENSION

    /** Optional sidecar written by the "smart cover" toggle. */
    fun coverFileName(projectName: String, timestampMs: Long): String =
        safeName(projectName) + "-" + stamp(timestampMs) + "-cover." + COVER_EXTENSION

    /** Container label for the summary panel. */
    fun formatLabel(format: ExportFormat): String = when (format) {
        ExportFormat.MP4_H264_AAC -> "MP4 · H.264"
        ExportFormat.WEBM_VP9_OPUS -> "WebM · VP9"
        ExportFormat.GIF -> "GIF"
        ExportFormat.PNG_SEQUENCE -> "PNG"
    }

    /**
     * Video + audio bitrate x duration, the same estimate the mockup shows.
     * The device encoder picks the real bitrate, so this is a rough preview.
     */
    fun estimatedSizeBytes(preset: ExportPreset, durationMs: Long): Long {
        val seconds = durationMs.coerceAtLeast(0L) / 1000.0
        val bits = (preset.videoBitrate.coerceAtLeast(0) + preset.audioBitrate.coerceAtLeast(0)) * seconds
        return (bits / 8.0).toLong().coerceAtLeast(1L)
    }

    fun sizeLabel(bytes: Long): String {
        val safe = bytes.coerceAtLeast(0L)
        return when {
            safe >= 1_000_000_000L -> String.format(Locale.US, "%.1f GB", safe / 1_000_000_000.0)
            safe >= 1_000_000L -> String.format(Locale.US, "%.1f MB", safe / 1_000_000.0)
            safe >= 1_000L -> String.format(Locale.US, "%.0f KB", safe / 1_000.0)
            else -> "$safe B"
        }
    }

    /** `mm:ss`, rounded to the nearest second so 24.6 s reads as 00:25. */
    fun durationLabel(durationMs: Long): String {
        val totalSeconds = (durationMs.coerceAtLeast(0L) + 500L) / 1000L
        return String.format(Locale.US, "%02d:%02d", totalSeconds / 60L, totalSeconds % 60L)
    }

    private fun stamp(timestampMs: Long): Long = timestampMs.coerceAtLeast(0L)
}
