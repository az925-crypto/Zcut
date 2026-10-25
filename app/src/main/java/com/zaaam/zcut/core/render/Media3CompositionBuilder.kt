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
                if (item.speed != 1f) setSpeed(ConstantSpeedProvider(item.speed))
            }
            .build()
    }

    private fun sourceUri(source: String): Uri =
        if (source.startsWith("/")) Uri.fromFile(File(source)) else Uri.parse(source)

    private class ConstantSpeedProvider(private val speed: Float) : SpeedProvider {
        override fun getSpeed(timeUs: Long): Float = speed

        override fun getNextSpeedChangeTimeUs(timeUs: Long): Long = C.TIME_UNSET
    }
}
