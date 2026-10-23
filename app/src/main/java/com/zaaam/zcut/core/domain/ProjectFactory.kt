package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.AspectRatio
import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.ClipType
import com.zaaam.zcut.core.model.ExportPreset
import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.core.model.MediaKind
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.model.ProjectSettings
import com.zaaam.zcut.core.model.Timeline
import com.zaaam.zcut.core.model.Track
import com.zaaam.zcut.core.model.TrackType

object ProjectFactory {
    fun create(
        name: String,
        aspectRatio: AspectRatio = AspectRatio.PORTRAIT_9_16,
        frameRate: Int = 30
    ): Project {
        val now = System.currentTimeMillis()
        val safeFrameRate = frameRate.coerceIn(15, 120)
        return Project(
            name = name.trim().ifBlank { "Untitled" },
            createdAtMs = now,
            updatedAtMs = now,
            aspectRatio = aspectRatio,
            frameRate = safeFrameRate,
            timeline = Timeline(tracks = defaultTracks()),
            settings = ProjectSettings(
                exportPreset = ExportPreset(frameRate = safeFrameRate)
            )
        )
    }

    fun defaultTracks(): List<Track> = listOf(
        Track(type = TrackType.VIDEO),
        Track(type = TrackType.OVERLAY),
        Track(type = TrackType.TEXT),
        Track(type = TrackType.CAPTION),
        Track(type = TrackType.AUDIO)
    )

    fun fromAssets(
        name: String,
        assets: List<MediaAsset>,
        aspectRatio: AspectRatio = AspectRatio.PORTRAIT_9_16,
        frameRate: Int = 30
    ): Project {
        val base = create(name, aspectRatio, frameRate)
        val videoTrack = base.timeline.tracks.first { it.type == TrackType.VIDEO }
        val audioTrack = base.timeline.tracks.first { it.type == TrackType.AUDIO }
        var videoCursorMs = 0L
        var audioCursorMs = 0L
        val videoClips = mutableListOf<Clip>()
        val audioClips = mutableListOf<Clip>()

        assets.forEach { asset ->
            when (asset.kind) {
                MediaKind.AUDIO -> {
                    val duration = asset.durationMs.takeIf { it > 0L } ?: 1_000L
                    audioClips += Clip(
                        assetId = asset.id,
                        type = ClipType.AUDIO,
                        startMs = audioCursorMs,
                        durationMs = duration,
                        sourceDurationMs = duration
                    )
                    audioCursorMs += duration
                }

                MediaKind.VIDEO, MediaKind.IMAGE, MediaKind.GIF, MediaKind.LIVE_PHOTO -> {
                    val duration = asset.durationMs.takeIf { it > 0L } ?: 3_000L
                    videoClips += Clip(
                        assetId = asset.id,
                        type = if (asset.kind == MediaKind.IMAGE) ClipType.IMAGE else ClipType.VIDEO,
                        startMs = videoCursorMs,
                        durationMs = duration,
                        sourceDurationMs = duration
                    )
                    videoCursorMs += duration
                }
            }
        }

        val timeline = base.timeline.copy(
            tracks = base.timeline.tracks.map { track ->
                when (track.id) {
                    videoTrack.id -> track.copy(clips = videoClips)
                    audioTrack.id -> track.copy(clips = audioClips)
                    else -> track
                }
            },
            durationMs = TimelineEditor.durationMs(videoClips + audioClips)
        )
        return base.copy(timeline = timeline)
    }
}
