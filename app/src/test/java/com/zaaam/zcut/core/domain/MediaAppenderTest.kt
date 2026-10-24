package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.ClipType
import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.core.model.MediaKind
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.model.TrackType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaAppenderTest {

    private fun asset(
        id: String,
        kind: MediaKind,
        durationMs: Long = 0L,
        uri: String = "content://test/$id"
    ): MediaAsset = MediaAsset(
        id = id,
        uri = uri,
        displayName = "$id.mp4",
        mimeType = if (kind == MediaKind.AUDIO) "audio/mp4" else "video/mp4",
        kind = kind,
        durationMs = durationMs
    )

    @Test
    fun visualAssetsArePlacedSequentiallyOnTheVideoTrack() {
        val project = MediaAppender.append(
            ProjectFactory.create("Timeline"),
            listOf(
                asset("v1", MediaKind.VIDEO, 2_000L),
                asset("i1", MediaKind.IMAGE),
                asset("g1", MediaKind.GIF)
            )
        )

        val clips = project.timeline.tracks.first { it.type == TrackType.VIDEO }.clips

        assertEquals(3, clips.size)
        assertEquals(0L, clips[0].startMs)
        assertEquals(2_000L, clips[0].durationMs)
        assertEquals(2_000L, clips[1].startMs)
        assertEquals(MediaAppender.DEFAULT_IMAGE_DURATION_MS, clips[1].durationMs)
        assertEquals(5_000L, clips[2].startMs)
        assertEquals(8_000L, project.timeline.durationMs)
    }

    @Test
    fun audioAssetsUseTheirOwnTrackAndDoNotDelayVideo() {
        val project = MediaAppender.append(
            ProjectFactory.create("Timeline"),
            listOf(
                asset("a1", MediaKind.AUDIO, 1_500L),
                asset("v1", MediaKind.VIDEO, 1_000L),
                asset("a2", MediaKind.AUDIO, 500L)
            )
        )

        val audio = project.timeline.tracks.first { it.type == TrackType.AUDIO }.clips
        val video = project.timeline.tracks.first { it.type == TrackType.VIDEO }.clips

        assertEquals(2, audio.size)
        assertEquals(0L, audio[0].startMs)
        assertEquals(1_500L, audio[1].startMs)
        assertEquals(1, video.size)
        assertEquals(0L, video.single().startMs)
        assertEquals(2_000L, project.timeline.durationMs)
    }

    @Test
    fun appendingAgainContinuesAfterTheLastClip() {
        val first = MediaAppender.append(
            ProjectFactory.create("Timeline"),
            listOf(asset("v1", MediaKind.VIDEO, 1_000L))
        )
        val second = MediaAppender.append(
            first,
            listOf(asset("v2", MediaKind.VIDEO, 2_500L))
        )

        val clips = second.timeline.tracks.first { it.type == TrackType.VIDEO }.clips

        assertEquals(2, clips.size)
        assertEquals(1_000L, clips[1].startMs)
        assertEquals(3_500L, second.timeline.durationMs)
    }

    @Test
    fun duplicateAndUnusableAssetsAreIgnored() {
        val first = MediaAppender.append(
            ProjectFactory.create("Timeline"),
            listOf(asset("v1", MediaKind.VIDEO, 1_000L))
        )
        val broken = asset("broken", MediaKind.VIDEO, 1_000L, uri = "")

        val second = MediaAppender.append(first, listOf(asset("v1", MediaKind.VIDEO), broken))

        assertEquals(1, second.assets.size)
        assertEquals(1, second.timeline.tracks.first { it.type == TrackType.VIDEO }.clips.size)
    }

    @Test
    fun missingTracksAreCreatedForBareProjects() {
        val bare = Project(name = "Bare")

        val project = MediaAppender.append(
            bare,
            listOf(
                asset("v1", MediaKind.VIDEO, 1_000L),
                asset("a1", MediaKind.AUDIO, 1_000L)
            )
        )

        assertTrue(project.timeline.tracks.any { it.type == TrackType.VIDEO })
        assertTrue(project.timeline.tracks.any { it.type == TrackType.AUDIO })
        assertEquals(2, project.assets.size)
    }

    @Test
    fun clipTypesFollowTheAssetKind() {
        assertEquals(ClipType.VIDEO, MediaAppender.clipTypeFor(MediaKind.VIDEO))
        assertEquals(ClipType.VIDEO, MediaAppender.clipTypeFor(MediaKind.LIVE_PHOTO))
        assertEquals(ClipType.IMAGE, MediaAppender.clipTypeFor(MediaKind.IMAGE))
        assertEquals(ClipType.IMAGE, MediaAppender.clipTypeFor(MediaKind.GIF))
        assertEquals(ClipType.AUDIO, MediaAppender.clipTypeFor(MediaKind.AUDIO))
    }

    @Test
    fun durationFallsBackByKindWhenMetadataIsMissing() {
        assertEquals(2_000L, MediaAppender.durationFor(asset("v1", MediaKind.VIDEO, 2_000L)))
        assertEquals(
            MediaAppender.DEFAULT_VIDEO_DURATION_MS,
            MediaAppender.durationFor(asset("v2", MediaKind.VIDEO))
        )
        assertEquals(
            MediaAppender.DEFAULT_AUDIO_DURATION_MS,
            MediaAppender.durationFor(asset("a1", MediaKind.AUDIO))
        )
        assertEquals(
            MediaAppender.DEFAULT_IMAGE_DURATION_MS,
            MediaAppender.durationFor(asset("i1", MediaKind.IMAGE))
        )
        assertEquals(
            MediaAppender.DEFAULT_IMAGE_DURATION_MS,
            MediaAppender.durationFor(asset("g1", MediaKind.GIF))
        )
    }
}
