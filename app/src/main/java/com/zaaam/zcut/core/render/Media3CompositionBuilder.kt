package com.zaaam.zcut.core.render

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.audio.SpeedProvider
import androidx.media3.common.util.ExperimentalApi
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import com.zaaam.zcut.core.domain.SpeedCurve
import com.zaaam.zcut.core.model.SpeedPoint
import java.io.File

/**
 * Translates the pure [PreviewPlan] into a Media3 composition. Thin on purpose:
 * every decision that can be wrong (which clips, gaps, source ranges) lives in
 * [PreviewPlanBuilder] and is covered by JVM tests.
 */
@OptIn(ExperimentalApi::class, UnstableApi::class)
object Media3CompositionBuilder {

    fun build(plan: PreviewPlan): Composition {
        // A skipped trailing clip (missing asset, overlap, muted audio) would
        // otherwise make a sequence shorter than the timeline: CompositionPlayer
        // clamps seeks to its own duration and the playhead would jump backwards.
        // Pad every sequence to the timeline end so the player can reach it.
        val sequences = buildList {
            if (plan.videoItems.isNotEmpty()) {
                add(
                    sequence(
                        plan.videoItems,
                        visual = true,
                        frameRate = plan.frameRate,
                        trailingGapMs = plan.trailingGapMs(plan.videoItems)
                    )
                )
            }
            if (plan.audioItems.isNotEmpty()) {
                add(
                    sequence(
                        plan.audioItems,
                        visual = false,
                        frameRate = plan.frameRate,
                        trailingGapMs = plan.trailingGapMs(plan.audioItems)
                    )
                )
            }
        }
        require(sequences.isNotEmpty()) { "Preview plan has no playable items" }
        return Composition.Builder(sequences).build()
    }

    private fun sequence(
        items: List<PreviewItem>,
        visual: Boolean,
        frameRate: Int,
        trailingGapMs: Long = 0L
    ): EditedMediaItemSequence {
        val trackTypes = if (visual) {
            setOf(C.TRACK_TYPE_VIDEO, C.TRACK_TYPE_AUDIO)
        } else {
            setOf(C.TRACK_TYPE_AUDIO)
        }
        val builder = EditedMediaItemSequence.Builder(trackTypes)
        var cursorMs = 0L
        for (item in items) {
            val gapMs = item.timelineStartMs - cursorMs
            if (gapMs > 0L) {
                builder.addGap(gapMs * 1_000L)
            }
            builder.addItem(editedItem(item, visual, frameRate))
            cursorMs = item.timelineStartMs + item.durationMs
        }
        if (trailingGapMs > 0L) {
            builder.addGap(trailingGapMs * 1_000L)
        }
        return builder.build()
    }

    private fun editedItem(
        item: PreviewItem,
        visual: Boolean,
        frameRate: Int
    ): EditedMediaItem {
        val mediaItem = MediaItem.Builder()
            .apply {
                setUri(sourceUri(item.source))
                if (item.isImage) {
                    setImageDurationMs(item.sourceDurationMs)
                } else {
                    setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(item.sourceStartMs)
                            .setEndPositionMs(item.sourceStartMs + item.sourceDurationMs)
                            .build()
                    )
                }
            }
            .build()

        // CompositionPlayer subtracts the clipping start from durationUs, so it
        // must describe the full source up to the clip end, not the clip length.
        val fullDurationMs = if (item.isImage) {
            item.sourceDurationMs
        } else {
            item.sourceStartMs + item.sourceDurationMs
        }
        return EditedMediaItem.Builder(mediaItem)
            .setDurationUs(fullDurationMs * 1_000L)
            .apply {
                if (!visual) setRemoveVideo(true)
                if (item.removeAudio) setRemoveAudio(true)
                if (item.isImage) setFrameRate(frameRate)
                when {
                    item.curve.size >= 2 -> setSpeed(
                        CurveSpeedProvider(item.curve, item.sourceDurationMs, item.speed)
                    )
                    item.speed != 1f -> setSpeed(ConstantSpeedProvider(item.speed))
                }
            }
            .build()
    }

    private fun sourceUri(source: String): Uri =
        if (source.startsWith("/")) Uri.fromFile(File(source)) else Uri.parse(source)

    private class ConstantSpeedProvider(private val speed: Float) : SpeedProvider {
        override fun getSpeed(timeUs: Long): Float = speed

        override fun getNextSpeedChangeTimeUs(timeUs: Long): Long = C.TIME_UNSET
    }

    /**
     * Maps Media3's per-stream timestamps (0 at the first clipped frame) back to
     * the clip's normalised source curve. Media3 samples video per frame; the
     * 100 ms step keeps audio following a ramp instead of holding the segment's
     * starting speed for its whole length.
     *
     * ponytail: the preview integrates the curve per frame while the timeline
     * stores the closed-form duration; a ramp can differ by a few ms. Export
     * (M21) will use the same provider, so the two stay aligned there.
     */
    private class CurveSpeedProvider(
        curve: List<SpeedPoint>,
        private val sourceDurationMs: Long,
        private val fallbackSpeed: Float
    ) : SpeedProvider {

        private val points = SpeedCurve.normalized(curve, fallbackSpeed)

        override fun getSpeed(timeUs: Long): Float {
            if (points.isEmpty() || sourceDurationMs <= 0L) return fallbackSpeed
            val fraction = (timeUs / 1_000.0 / sourceDurationMs).toFloat()
            return SpeedCurve.speedAtNormalized(points, fraction, fallbackSpeed)
        }

        override fun getNextSpeedChangeTimeUs(timeUs: Long): Long {
            if (points.isEmpty() || sourceDurationMs <= 0L) return C.TIME_UNSET
            val fraction = (timeUs / 1_000.0 / sourceDurationMs).toFloat()
            val boundaryUs = points
                .firstOrNull { it.atFraction > fraction + CURVE_FRACTION_EPSILON }
                ?.let { (it.atFraction * sourceDurationMs * 1_000.0).toLong() }
                ?: C.TIME_UNSET
            val sampledUs = timeUs + CURVE_SAMPLE_US
            return if (boundaryUs == C.TIME_UNSET) sampledUs else minOf(boundaryUs, sampledUs)
        }

        private companion object {
            const val CURVE_SAMPLE_US = 100_000L
            const val CURVE_FRACTION_EPSILON = 0.0001f
        }
    }
}
