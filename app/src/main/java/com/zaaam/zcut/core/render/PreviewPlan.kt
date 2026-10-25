package com.zaaam.zcut.core.render

import com.zaaam.zcut.core.model.ClipType
import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.core.model.MediaKind
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.model.Track
import com.zaaam.zcut.core.model.TrackType
import java.io.File
import kotlin.math.roundToLong

/**
 * One playable segment of the preview composition.
 *
 * [timelineStartMs] is the position on the project timeline, so gaps left by
 * missing or skipped clips survive into the Media3 sequence. [sourceStartMs] and
 * [sourceDurationMs] describe the range read from the private media copy.
 */
data class PreviewItem(
    val clipId: String,
    val assetId: String,
    val source: String,
    val timelineStartMs: Long,
    val durationMs: Long,
    val sourceStartMs: Long,
    val sourceDurationMs: Long,
    val speed: Float,
    val isImage: Boolean,
    val removeAudio: Boolean
)

/**
 * Pure mapping from a draft to the two sequences Media3 can preview in M3: the
 * main visual track and the first audio track. Text, captions, stickers,
 * effects, overlays and additional audio tracks are not composited yet; those
 * arrive with their own milestones.
 */
data class PreviewPlan(
    val videoItems: List<PreviewItem>,
    val audioItems: List<PreviewItem>,
    val durationMs: Long,
    val frameRate: Int
) {
    val hasVideo: Boolean get() = videoItems.isNotEmpty()
    val hasAudio: Boolean get() = audioItems.isNotEmpty() || videoItems.any { !it.removeAudio }
    val isPlayable: Boolean get() = hasVideo || hasAudio

    /** The visual item on screen at [positionMs], or the next one after a gap. */
    fun nextVisualItem(positionMs: Long): PreviewItem? =
        videoItems.firstOrNull { positionMs < it.timelineStartMs + it.durationMs }

    /** Empty time after the last item of [items], up to this plan's end. */
    fun trailingGapMs(items: List<PreviewItem>): Long {
        val lastEndMs = items.maxOfOrNull { it.timelineStartMs + it.durationMs } ?: 0L
        return (durationMs - lastEndMs).coerceAtLeast(0L)
    }
}

object PreviewPlanBuilder {

    fun build(project: Project, allowedLocalRoot: String): PreviewPlan {
        // An empty root canonicalises to "/" and would silently allow every
        // absolute path, so fail loudly instead of weakening the gate.
        require(allowedLocalRoot.isNotBlank()) { "allowedLocalRoot must not be blank" }
        val assets = project.assets.associateBy { it.id }
        // Media3 can only play one visual sequence in M3. Prefer the main video
        // track, then fall back to overlay-only drafts; a track whose clips all
        // lost their media must not hide a playable overlay layer.
        val videoItems = project.timeline.tracks
            .asSequence()
            .filter { it.type == TrackType.VIDEO || it.type == TrackType.OVERLAY }
            .sortedBy { if (it.type == TrackType.VIDEO) 0 else 1 }
            .map { buildItems(it, assets, visual = true, allowedLocalRoot) }
            .firstOrNull { it.isNotEmpty() }
            .orEmpty()
        // Same rule for audio: the first non-muted track that actually yields
        // playable items wins. Later audio tracks are mixed in M12.
        val audioItems = project.timeline.tracks
            .asSequence()
            .filter { it.type == TrackType.AUDIO && !it.isMuted }
            .map { buildItems(it, assets, visual = false, allowedLocalRoot) }
            .firstOrNull { it.isNotEmpty() }
            .orEmpty()

        val lastItemEnd = (videoItems + audioItems)
            .maxOfOrNull { it.timelineStartMs + it.durationMs }
            ?: 0L
        return PreviewPlan(
            videoItems = videoItems,
            audioItems = audioItems,
            durationMs = maxOf(project.timeline.durationMs, lastItemEnd).coerceAtLeast(0L),
            frameRate = project.frameRate.coerceIn(15, 120)
        )
    }

    private fun buildItems(
        track: Track,
        assets: Map<String, MediaAsset>,
        visual: Boolean,
        allowedLocalRoot: String
    ): List<PreviewItem> {
        val items = mutableListOf<PreviewItem>()
        var cursorMs = 0L
        for (clip in track.clips.sortedBy { it.startMs }) {
            if (clip.durationMs <= 0L) continue
            if (clip.type != ClipType.VIDEO &&
                clip.type != ClipType.IMAGE &&
                clip.type != ClipType.AUDIO
            ) {
                continue
            }
            val asset = clip.assetId?.let(assets::get) ?: continue
            if (visual && !asset.isVisual) continue
            if (!visual && asset.kind != MediaKind.AUDIO && !asset.isVisual) continue

            val source = localSource(asset, allowedLocalRoot) ?: continue
            val startMs = clip.startMs.coerceAtLeast(0L)
            // A single Media3 sequence cannot play two clips at once. The later
            // clip is skipped until the timeline editor (M4) prevents overlaps.
            if (startMs < cursorMs) continue

            val isImage = asset.kind == MediaKind.IMAGE ||
                asset.kind == MediaKind.GIF ||
                asset.kind == MediaKind.LIVE_PHOTO
            val removeAudio = if (visual) {
                isImage || track.isMuted || clip.audio.muted || !asset.hasAudio
            } else {
                clip.audio.muted
            }
            // A muted audio clip still occupies its place on the timeline. It is
            // skipped here; the next item's start adds the gap back.
            if (!visual && removeAudio) continue

            val speed = clip.speed.safeSpeed
            // A still image has no intrinsic source length: its chosen clip
            // duration is the source. Speed scales presentation, not the source.
            val sourceDurationMs = if (isImage) {
                (clip.durationMs.toDouble() * speed).roundToLong().coerceAtLeast(1L)
            } else {
                clip.effectiveSourceDurationMs
                    .takeIf { it > 0L }
                    ?: (clip.durationMs.toDouble() * speed).roundToLong().coerceAtLeast(1L)
            }

            items += PreviewItem(
                clipId = clip.id,
                assetId = asset.id,
                source = source,
                timelineStartMs = startMs,
                durationMs = clip.durationMs,
                sourceStartMs = clip.sourceStartMs.coerceAtLeast(0L),
                sourceDurationMs = sourceDurationMs,
                speed = speed,
                isImage = isImage,
                removeAudio = removeAudio
            )
            cursorMs = startMs + clip.durationMs
        }
        return items
    }

    /**
     * Only app-local media may reach Media3. The private copy must live under
     * the supplied app root (canonicalised, so `..` and symlinks cannot escape);
     * the fallback URI must be a picker `content://` URI. Network and `file://`
     * sources are rejected even if a damaged draft names one.
     */
    private fun localSource(asset: MediaAsset, allowedLocalRoot: String): String? {
        val local = asset.localPath?.takeIf { it.isNotBlank() }
        if (local != null && local.startsWith("/") && local.isUnder(allowedLocalRoot)) {
            return local
        }
        return asset.uri.takeIf { it.isNotBlank() && it.startsWith("content://") }
    }

    private fun String.isUnder(root: String): Boolean = runCatching {
        val rootPath = File(root).canonicalFile
        val candidate = File(this).canonicalFile
        candidate == rootPath ||
            candidate.path.startsWith(rootPath.path.trimEnd(File.separatorChar) + File.separator)
    }.getOrDefault(false)
}
