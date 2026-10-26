package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.model.TrackType

/**
 * Turns a [VideoTemplate] plus a user's media into a real draft (cut-same).
 *
 * Pure JVM: the whole mapping is unit-testable without Android, and it never
 * invents media — a slot with no selected asset simply stays empty.
 */
object TemplateEditor {

    /** The source window a slot asks for. */
    data class Fit(val sourceStartMs: Long, val durationMs: Long)

    /**
     * Fits a source into a slot: a longer source is centre-cropped (the same
     * rule the artifact's `fitToSlot` uses), a shorter one is used whole, and an
     * unknown duration (0) falls back to the slot length.
     */
    fun fitToSlot(sourceDurationMs: Long, slotDurationMs: Long): Fit {
        val source = sourceDurationMs.coerceAtLeast(0L)
        val slot = slotDurationMs.coerceAtLeast(1L)
        val used = if (source in 1 until slot) source else slot
        val start = ((source - used) / 2).coerceAtLeast(0L)
        return Fit(sourceStartMs = start, durationMs = used)
    }

    /**
     * Clones [template] into a fresh draft and fills its slots in order with the
     * usable visual [assets]. Extra assets are ignored; missing ones leave a gap
     * rather than a placeholder clip pretending to be media.
     */
    fun apply(template: VideoTemplate, assets: List<MediaAsset>, name: String): Project {
        val base = ProjectFactory.create(
            name = name.ifBlank { template.name },
            aspectRatio = template.ratio,
            frameRate = template.frameRate
        )
        val usable = assets.filter { it.isUsable && it.isVisual }.take(template.slotCount)
        if (usable.isEmpty()) return base

        val videoTrack = base.timeline.tracks.first { it.type == TrackType.VIDEO }
        val starts = template.slotStartsMs()
        var timeline = base.timeline
        usable.forEachIndexed { index, asset ->
            val fit = fitToSlot(
                sourceDurationMs = MediaAppender.durationFor(asset),
                slotDurationMs = template.slots[index].durationMs
            )
            timeline = TimelineEditor.addClip(
                timeline,
                videoTrack.id,
                Clip(
                    assetId = asset.id,
                    type = MediaAppender.clipTypeFor(asset.kind),
                    startMs = starts[index],
                    durationMs = fit.durationMs,
                    sourceStartMs = fit.sourceStartMs,
                    sourceDurationMs = fit.durationMs
                )
            )
        }

        val grid = template.bpm
            ?.let { BeatDetector.grid(it, timeline.durationMs) }
            .orEmpty()
        return base.copy(
            assets = usable,
            timeline = timeline,
            settings = base.settings.copy(
                beatGridMs = grid,
                snapToBeat = grid.isNotEmpty()
            )
        )
    }
}