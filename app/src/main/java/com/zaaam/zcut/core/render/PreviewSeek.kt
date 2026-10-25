package com.zaaam.zcut.core.render

/**
 * Pure seek/range math for the preview player. Kept separate from Media3 so the
 * clamping and "play from the selected clip" rules are unit-testable on the JVM.
 */
object PreviewSeek {

    fun clamp(positionMs: Long, durationMs: Long): Long =
        positionMs.coerceIn(0L, durationMs.coerceAtLeast(0L))

    /**
     * Normalises a preview range. Both ends are required and the range must be
     * non-empty; anything else means "play the whole timeline".
     */
    fun range(startMs: Long?, endMs: Long?, durationMs: Long): LongRange? {
        if (startMs == null || endMs == null) return null
        val start = clamp(startMs, durationMs)
        val end = clamp(endMs, durationMs)
        return if (end > start) start..end else null
    }

    /**
     * Where playback should start when the play button is pressed. A finished
     * timeline restarts at zero; a selected range restarts at its own beginning.
     */
    fun playFrom(range: LongRange?, positionMs: Long, durationMs: Long): Long {
        val position = clamp(positionMs, durationMs)
        return when {
            range == null -> if (position >= durationMs) 0L else position
            position < range.first || position >= range.last -> range.first
            else -> position
        }
    }

    fun reachedEnd(range: LongRange?, positionMs: Long, durationMs: Long): Boolean {
        val end = range?.last ?: durationMs
        return positionMs >= end
    }

    /**
     * A manual seek outside the selected-clip range ends that range; otherwise
     * the ticker would immediately pull the playhead back to the range end.
     */
    fun rangeAfterSeek(range: LongRange?, positionMs: Long): LongRange? {
        if (range == null) return null
        // `last` is the exclusive end for playback (playFrom/reachedEnd), so a
        // seek exactly to it must drop the range too.
        return if (positionMs < range.first || positionMs >= range.last) null else range
    }
}
