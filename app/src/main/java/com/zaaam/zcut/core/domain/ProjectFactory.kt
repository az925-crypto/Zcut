package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.AspectRatio
import com.zaaam.zcut.core.model.ExportPreset
import com.zaaam.zcut.core.model.MediaAsset
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
    ): Project = MediaAppender.append(create(name, aspectRatio, frameRate), assets)
}
