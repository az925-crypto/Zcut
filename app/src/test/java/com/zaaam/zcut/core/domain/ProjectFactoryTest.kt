package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.core.model.MediaKind
import com.zaaam.zcut.core.model.TrackType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectFactoryTest {

    private fun asset(kind: MediaKind, durationMs: Long, name: String): MediaAsset =
        MediaAsset(
            uri = "content://test/$name",
            displayName = name,
            mimeType = if (kind == MediaKind.AUDIO) "audio/mp4" else "video/mp4",
            kind = kind,
            durationMs = durationMs
        )

    @Test
    fun createClampsFrameRateAndMatchesExportPreset() {
        val project = ProjectFactory.create("Test", frameRate = 240)

        assertEquals(120, project.frameRate)
        assertEquals(120, project.settings.exportPreset.frameRate)
        assertTrue(project.timeline.tracks.any { it.type == TrackType.CAPTION })
    }

    @Test
    fun audioAssetsArePlacedSequentially() {
        val project = ProjectFactory.fromAssets(
            name = "Audio",
            assets = listOf(
                asset(MediaKind.AUDIO, 1_000L, "a.m4a"),
                asset(MediaKind.AUDIO, 2_000L, "b.m4a")
            )
        )
        val audio = project.timeline.tracks.first { it.type == TrackType.AUDIO }.clips

        assertEquals(2, audio.size)
        assertEquals(0L, audio[0].startMs)
        assertEquals(1_000L, audio[1].startMs)
        assertEquals(3_000L, project.timeline.durationMs)
    }

    @Test
    fun missingAudioDurationUsesSafeFallback() {
        val project = ProjectFactory.fromAssets(
            name = "Audio",
            assets = listOf(asset(MediaKind.AUDIO, 0L, "unknown.m4a"))
        )
        val clip = project.timeline.tracks.first { it.type == TrackType.AUDIO }.clips.single()

        assertEquals(1_000L, clip.durationMs)
        assertEquals(1_000L, project.timeline.durationMs)
    }

    @Test
    fun bogusAssetDurationDoesNotPoisonTheTrack() {
        val project = ProjectFactory.fromAssets(
            name = "Audio",
            assets = listOf(
                asset(MediaKind.AUDIO, Long.MAX_VALUE, "huge.m4a"),
                asset(MediaKind.AUDIO, 1_000L, "after.m4a")
            )
        )
        val audio = project.timeline.tracks.first { it.type == TrackType.AUDIO }.clips

        // The cursor saturates instead of wrapping, and the clip that would have
        // started past Long.MAX_VALUE is dropped rather than stored invalid.
        assertEquals(1, audio.size)
        assertEquals(0L, audio.single().startMs)
        assertEquals(Long.MAX_VALUE, project.timeline.durationMs)
    }
}
