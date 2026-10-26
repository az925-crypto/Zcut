package com.zaaam.zcut.core.domain

/**
 * Splits a transcript into caption lines.
 *
 * Word splitting of the analysed artifact (`AsrWordsPerLineConfig`,
 * `GenerateSubtitleData`) breaks lines by character budget, not only by pauses;
 * this is the same rule expressed as a pure function so it can be tested and
 * reused by both the local recogniser and SRT import.
 *
 * Everything here is deterministic and Android-free.
 */
object CaptionSegmenter {

    /** Line budget used by the approval mockup's caption strip. */
    const val DEFAULT_MAX_CHARS = 32
    const val DEFAULT_MAX_DURATION_MS = 5_000L

    /** One word with a measured time window (from a recogniser or SRT). */
    data class Word(
        val text: String,
        val startMs: Long,
        val endMs: Long
    )

    /** One caption line with its time window. */
    data class Line(
        val text: String,
        val startMs: Long,
        val endMs: Long
    )

    /**
     * Greedy word wrap: a line never exceeds [maxChars] unless a single word is
     * longer than the budget (then it stands alone rather than being cut).
     */
    fun splitIntoLines(text: String, maxChars: Int = DEFAULT_MAX_CHARS): List<String> {
        val budget = maxChars.coerceAtLeast(1)
        val words = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return emptyList()

        val lines = mutableListOf<String>()
        val current = StringBuilder()
        words.forEach { word ->
            val projected = if (current.isEmpty()) word.length else current.length + 1 + word.length
            if (current.isNotEmpty() && projected > budget) {
                lines += current.toString()
                current.setLength(0)
            }
            if (current.isNotEmpty()) current.append(' ')
            current.append(word)
        }
        if (current.isNotEmpty()) lines += current.toString()
        return lines
    }

    /**
     * Turns a phrase with one measured window into lines. Timing is distributed
     * across the lines *proportionally to their length*: a phrase-level result
     * carries no per-word timings, so an even split would be a bigger lie than a
     * length-weighted estimate, and this is documented as an estimate.
     */
    fun segment(
        text: String,
        startMs: Long,
        endMs: Long,
        maxChars: Int = DEFAULT_MAX_CHARS
    ): List<Line> {
        val lines = splitIntoLines(text, maxChars)
        if (lines.isEmpty()) return emptyList()
        val start = startMs.coerceAtLeast(0L)
        val end = endMs.coerceAtLeast(start)
        val totalChars = lines.sumOf { it.length }.coerceAtLeast(1)
        var cursor = start
        return lines.mapIndexed { index, line ->
            val isLast = index == lines.lastIndex
            val span = if (isLast) {
                (end - cursor).coerceAtLeast(0L)
            } else {
                ((end - start).toDouble() * line.length / totalChars).toLong()
                    .coerceAtLeast(1L)
            }
            val lineStart = cursor
            val lineEnd = (lineStart + span).coerceAtMost(end)
            cursor = lineEnd
            Line(line, lineStart, lineEnd)
        }
    }

    /**
     * The word-level rule from the feature spec: flush when the line would grow
     * past [maxChars] or when the line has already been on screen for more than
     * [maxDurationMs].
     */
    fun fromWords(
        words: List<Word>,
        maxChars: Int = DEFAULT_MAX_CHARS,
        maxDurationMs: Long = DEFAULT_MAX_DURATION_MS
    ): List<Line> {
        if (words.isEmpty()) return emptyList()
        val lines = mutableListOf<Line>()
        var buffer = mutableListOf<Word>()

        fun flush() {
            if (buffer.isEmpty()) return
            lines += Line(
                text = buffer.joinToString(" ") { it.text },
                startMs = buffer.first().startMs,
                endMs = buffer.last().endMs
            )
            buffer = mutableListOf()
        }

        val budget = maxChars.coerceAtLeast(1)
        words.forEach { word ->
            val projected = buffer.joinToString(" ") { it.text }.length + 1 + word.text.length
            val tooLong = buffer.isNotEmpty() && projected > budget
            val tooSlow = buffer.isNotEmpty() &&
                word.endMs - buffer.first().startMs > maxDurationMs
            if (tooLong || tooSlow) flush()
            buffer += word
        }
        flush()
        return lines
    }
}