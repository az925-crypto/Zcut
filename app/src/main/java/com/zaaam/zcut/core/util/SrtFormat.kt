package com.zaaam.zcut.core.util

import com.zaaam.zcut.core.domain.CaptionEditor
import com.zaaam.zcut.core.model.CaptionCue
import com.zaaam.zcut.core.model.Transcript
import com.zaaam.zcut.core.model.TranscriptSource
import java.util.UUID

/**
 * SubRip (SRT) read/write. Plain text, no Android types, so import/export is
 * unit-tested on the JVM. The internal subtitle format of the analysed artifact
 * was never verified (see the F15 spec), so this uses the standard, documented
 * SRT shape rather than guessing a private one.
 */
object SrtFormat {

    private val TIME_LINE = Regex(
        """(\d{1,2}):(\d{2}):(\d{2})[,.](\d{1,3})\s*-->\s*(\d{1,2}):(\d{2}):(\d{2})[,.](\d{1,3})"""
    )

    fun write(transcript: Transcript): String {
        val cues = CaptionEditor.ordered(transcript)
        if (cues.isEmpty()) return ""
        return buildString {
            cues.forEachIndexed { index, cue ->
                if (index > 0) append('\n')
                append(index + 1).append('\n')
                append(timestamp(cue.startMs)).append(" --> ").append(timestamp(cue.endMs)).append('\n')
                append(cue.text.trim()).append('\n')
            }
        }
    }

    /** `00:00:02,400` */
    fun timestamp(ms: Long): String {
        val total = ms.coerceAtLeast(0L)
        val millis = total % 1_000L
        val seconds = (total / 1_000L) % 60L
        val minutes = (total / 60_000L) % 60L
        val hours = total / 3_600_000L
        return "%02d:%02d:%02d,%03d".format(
            java.util.Locale.US,
            hours,
            minutes,
            seconds,
            millis
        )
    }

    /**
     * Tolerant reader: it accepts `,` or `.` before the milliseconds, `CRLF`,
     * missing/duplicated indexes and stray blank lines. A block without a valid
     * time line is skipped rather than aborting the whole import.
     */
    fun parse(text: String): Transcript {
        val cues = mutableListOf<CaptionCue>()
        val blocks = text.replace("\r\n", "\n").replace('\r', '\n').split("\n\n")
        blocks.forEach { block ->
            val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
            val timeIndex = lines.indexOfFirst { TIME_LINE.containsMatchIn(it) }
            if (timeIndex < 0) return@forEach
            val match = TIME_LINE.find(lines[timeIndex]) ?: return@forEach
            val startMs = millis(match.groupValues, 1)
            val endMs = millis(match.groupValues, 5)
            val body = lines.drop(timeIndex + 1).joinToString("\n").trim()
            if (body.isEmpty()) return@forEach
            cues += CaptionCue(
                id = UUID.randomUUID().toString(),
                startMs = startMs,
                endMs = endMs,
                text = body.take(CaptionEditor.MAX_LINE_LENGTH),
                style = null
            )
        }
        return Transcript(
            cues = cues,
            source = TranscriptSource.IMPORTED_SRT
        )
    }

    private fun millis(groups: List<String>, from: Int): Long {
        val hours = groups[from].toLongOrNull() ?: 0L
        val minutes = groups[from + 1].toLongOrNull() ?: 0L
        val seconds = groups[from + 2].toLongOrNull() ?: 0L
        val fraction = groups[from + 3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
        return hours * 3_600_000L + minutes * 60_000L + seconds * 1_000L + fraction
    }
}