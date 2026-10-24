package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.ClipType
import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.core.model.MediaKind
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.model.Track
import com.zaaam.zcut.core.model.TrackType

/**
 * Appends freshly imported media to a project: visual assets go to the main
 * video track, audio to the audio track, each after the previous clip. Pure JVM
 * so the placement rules are unit-testable.
 */
object MediaAppender {

    /** A still image has no intrinsic duration; CapCut-style default is 3 s. */
    const val DEFAULT_IMAGE_DURATION_MS = 3_000L

    /** Audio with unreadable metadata still gets a usable, audible clip. */
    const val DEFAULT_AUDIO_DURATION_MS = 1_000L

    /** Video with unreadable metadata must not collapse to a zero-length clip. */
    const val DEFAULT_VIDEO_DURATION_MS = 1_000L

    fun append(project: Project, assets: List<MediaAsset>): Project {
        val known = project.assets.mapTo(HashSet()) { it.id }
        val fresh = assets.filter { it.isUsable && known.add(it.id) }
        if (fresh.isEmpty()) return project

        var tracks = project.timeline.tracks
        tracks = ensureTrack(tracks, TrackType.VIDEO)
        tracks = ensureTrack(tracks, TrackType.AUDIO)
        var timeline = project.timeline.copy(tracks = tracks)

        fresh.forEach { asset ->
            val trackType = if (asset.kind == MediaKind.AUDIO) TrackType.AUDIO else TrackType.VIDEO
            val track = timeline.tracks.first { it.type == trackType }
            val startMs = track.clips.maxOfOrNull { it.endMs } ?: 0L
            val durationMs = durationFor(asset)
            timeline = TimelineEditor.addClip(
                timeline,
                track.id,
                Clip(
                    assetId = asset.id,
                    type = clipTypeFor(asset.kind),
                    startMs = startMs,
                    durationMs = durationMs,
                    sourceDurationMs = durationMs
                )
            )
        }

        return project.copy(
            assets = project.assets + fresh,
            timeline = timeline
        )
    }

    fun clipTypeFor(kind: MediaKind): ClipType = when (kind) {
        MediaKind.VIDEO, MediaKind.LIVE_PHOTO -> ClipType.VIDEO
        MediaKind.IMAGE, MediaKind.GIF -> ClipType.IMAGE
        MediaKind.AUDIO -> ClipType.AUDIO
    }

    fun durationFor(asset: MediaAsset): Long {
        if (asset.durationMs > 0L) return asset.durationMs
        return when (asset.kind) {
            MediaKind.AUDIO -> DEFAULT_AUDIO_DURATION_MS
            MediaKind.VIDEO -> DEFAULT_VIDEO_DURATION_MS
            else -> DEFAULT_IMAGE_DURATION_MS
        }
    }

    private fun ensureTrack(tracks: List<Track>, type: TrackType): List<Track> =
        if (tracks.any { it.type == type }) tracks else tracks + Track(type = type)
}
