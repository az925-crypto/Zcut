package com.zaaam.zcut.feature.editor

import com.zaaam.zcut.core.model.Track
import com.zaaam.zcut.core.model.TrackType
import kotlin.math.ceil

/**
 * Pure geometry for the timeline canvas. Kept out of the composable so the
 * ruler/lane math is unit-testable on the JVM.
 */
object EditorTimeline {

    /** Horizontal density of the canvas. 64dp per second reads well on phones. */
    const val PX_PER_SECOND_DP = 64f

    /** Very short clips still need a tappable target. */
    const val MIN_CLIP_WIDTH_DP = 8f

    /** A long timeline must not compose thousands of ruler labels. */
    const val MAX_RULER_LABELS = 120

    const val LABEL_COLUMN_DP = 44f
    const val RULER_HEIGHT_DP = 26f
    const val LANE_HEIGHT_DP = 34f
    const val LANE_GAP_DP = 4f

    private val trackOrder = listOf(
        TrackType.VIDEO,
        TrackType.OVERLAY,
        TrackType.TEXT,
        TrackType.CAPTION,
        TrackType.AUDIO,
        TrackType.EFFECT,
        TrackType.STICKER
    )

    fun orderedTracks(tracks: List<Track>): List<Track> =
        tracks.sortedBy { track ->
            val index = trackOrder.indexOf(track.type)
            if (index < 0) trackOrder.size else index
        }

    fun label(type: TrackType): String = when (type) {
        TrackType.VIDEO -> "V1"
        TrackType.OVERLAY -> "OV"
        TrackType.TEXT -> "T1"
        TrackType.CAPTION -> "CC"
        TrackType.AUDIO -> "A1"
        TrackType.EFFECT -> "FX"
        TrackType.STICKER -> "ST"
    }

    fun contentWidthDp(durationMs: Long, minWidthDp: Float): Float =
        maxOf(minWidthDp, offsetDp(durationMs))

    fun offsetDp(timeMs: Long): Float =
        timeMs.coerceAtLeast(0L) / 1_000f * PX_PER_SECOND_DP

    fun widthDp(durationMs: Long): Float =
        maxOf(MIN_CLIP_WIDTH_DP, durationMs.coerceAtLeast(0L) / 1_000f * PX_PER_SECOND_DP)

    fun msAt(xDp: Float): Long =
        (xDp.coerceAtLeast(0f) / PX_PER_SECOND_DP * 1_000f).toLong()

    /** Ruler label density: 1 s ticks for short cuts, 5 s for long ones. */
    fun labelIntervalSeconds(durationMs: Long): Int {
        val totalSeconds = ceil(durationMs.coerceAtLeast(0L) / 1_000.0)
            .toLong()
            .coerceAtLeast(1L)
        val base = when {
            durationMs <= 30_000L -> 1
            durationMs <= 90_000L -> 2
            else -> 5
        }
        // ponytail: fixed zoom until M4 adds pinch-zoom. Cap the label count so
        // an hour-long draft does not compose thousands of Text nodes.
        val fitAll = ceil(totalSeconds.toDouble() / MAX_RULER_LABELS).toInt()
        return maxOf(base, fitAll)
    }

    fun canvasHeightDp(trackCount: Int): Float =
        RULER_HEIGHT_DP + trackCount.coerceAtLeast(0) * (LANE_HEIGHT_DP + LANE_GAP_DP)
}
