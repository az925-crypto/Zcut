package com.zaaam.zcut.core.render

import com.zaaam.zcut.core.model.AudioConfig
import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.ClipType
import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.core.model.MediaKind
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.model.SpeedConfig
import com.zaaam.zcut.core.model.Timeline
import com.zaaam.zcut.core.model.Track
import com.zaaam.zcut.core.model.TrackType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewPlanTest {

    private fun asset(
        id: String,
        kind: MediaKind = MediaKind.VIDEO,
        hasAudio: Boolean = kind == MediaKind.VIDEO,
        localPath: String? = "/media/$id",
        durationMs: Long = 10_000L
    ) = MediaAsset(
        id = id,
        uri = "content://$id",
        displayName = id,
        kind = kind,
        durationMs = durationMs,
        hasAudio = hasAudio,
        localPath = localPath
    )

    private fun clip(
        id: String,
        assetId: String?,
        startMs: Long,
        durationMs: Long,
        type: ClipType = ClipType.VIDEO,
        sourceStartMs: Long = 0L,
        sourceDurationMs: Long = 0L,
        speed: Float = 1f,
        muted: Boolean = false
    ) = Clip(
        id = id,
        assetId = assetId,
        type = type,
        startMs = startMs,
        durationMs = durationMs,
        sourceStartMs = sourceStartMs,
        sourceDurationMs = sourceDurationMs,
        speed = SpeedConfig(speed),
        audio = AudioConfig(muted = muted)
    )

    private fun project(
        tracks: List<Track>,
        assets: List<MediaAsset>,
        durationMs: Long = 0L,
        frameRate: Int = 30
    ) = Project(
        name = "Preview",
        frameRate = frameRate,
        timeline = Timeline(tracks = tracks, durationMs = durationMs),
        assets = assets
    )

    // Production always passes the app's filesDir; "/" keeps fixture paths valid.
    private fun buildPlan(project: Project, allowedLocalRoot: String = "/"): PreviewPlan =
        PreviewPlanBuilder.build(project, allowedLocalRoot)

    @Test
    fun mapsVideoAndAudioWithSourceRangesAndSpeed() {
        val video = asset("v1")
        val audio = asset("a1", kind = MediaKind.AUDIO)
        val plan = buildPlan(
            project(
                tracks = listOf(
                    Track(
                        type = TrackType.VIDEO,
                        clips = listOf(
                            clip(
                                id = "c1",
                                assetId = "v1",
                                startMs = 0L,
                                durationMs = 4_000L,
                                sourceStartMs = 1_000L,
                                sourceDurationMs = 2_000L,
                                speed = 0.5f
                            )
                        )
                    ),
                    Track(
                        type = TrackType.AUDIO,
                        clips = listOf(
                            clip(
                                id = "c2",
                                assetId = "a1",
                                startMs = 500L,
                                durationMs = 1_500L,
                                type = ClipType.AUDIO,
                                sourceStartMs = 250L,
                                sourceDurationMs = 1_500L
                            )
                        )
                    )
                ),
                assets = listOf(video, audio),
                durationMs = 6_000L
            )
        )

        assertEquals(1, plan.videoItems.size)
        val item = plan.videoItems.single()
        assertEquals("c1", item.clipId)
        assertEquals("/media/v1", item.source)
        assertEquals(0L, item.timelineStartMs)
        assertEquals(4_000L, item.durationMs)
        assertEquals(1_000L, item.sourceStartMs)
        assertEquals(2_000L, item.sourceDurationMs)
        assertEquals(0.5f, item.speed)
        assertFalse(item.isImage)
        assertFalse(item.removeAudio)

        assertEquals(1, plan.audioItems.size)
        assertEquals("c2", plan.audioItems.single().clipId)
        assertEquals(500L, plan.audioItems.single().timelineStartMs)
        assertTrue(plan.hasVideo)
        assertTrue(plan.hasAudio)
        assertTrue(plan.isPlayable)
        assertEquals(6_000L, plan.durationMs)
    }

    @Test
    fun skipsMissingAndOverlappingClipsButKeepsGaps() {
        val video = asset("v1")
        val plan = buildPlan(
            project(
                tracks = listOf(
                    Track(
                        type = TrackType.VIDEO,
                        clips = listOf(
                            clip("c1", "v1", startMs = 0L, durationMs = 1_000L),
                            clip("missing", "gone", startMs = 1_000L, durationMs = 1_000L),
                            clip("overlap", "v1", startMs = 500L, durationMs = 1_000L),
                            clip("c4", "v1", startMs = 2_000L, durationMs = 1_000L)
                        )
                    )
                ),
                assets = listOf(video),
                durationMs = 6_000L
            )
        )

        assertEquals(listOf("c1", "c4"), plan.videoItems.map { it.clipId })
        assertEquals(0L, plan.videoItems[0].timelineStartMs)
        assertEquals(2_000L, plan.videoItems[1].timelineStartMs)
        assertEquals(6_000L, plan.durationMs)
    }

    @Test
    fun mutedAudioClipLeavesAGapAndMutedTrackIsIgnored() {
        val audio = asset("a1", kind = MediaKind.AUDIO)
        val plan = buildPlan(
            project(
                tracks = listOf(
                    Track(
                        type = TrackType.AUDIO,
                        isMuted = true,
                        clips = listOf(
                            clip("ignored", "a1", 0L, 1_000L, ClipType.AUDIO)
                        )
                    ),
                    Track(
                        type = TrackType.AUDIO,
                        clips = listOf(
                            clip("c1", "a1", 0L, 1_000L, ClipType.AUDIO),
                            clip(
                                "muted",
                                "a1",
                                1_000L,
                                1_000L,
                                ClipType.AUDIO,
                                muted = true
                            ),
                            clip("c3", "a1", 2_000L, 1_000L, ClipType.AUDIO)
                        )
                    )
                ),
                assets = listOf(audio),
                durationMs = 3_000L
            )
        )

        assertEquals(listOf("c1", "c3"), plan.audioItems.map { it.clipId })
        assertEquals(2_000L, plan.audioItems[1].timelineStartMs)
        assertFalse(plan.hasVideo)
        assertTrue(plan.hasAudio)
        assertEquals(3_000L, plan.durationMs)
    }

    @Test
    fun fallsBackToOverlayWhenVideoMediaIsMissing() {
        val image = asset("img", kind = MediaKind.IMAGE, hasAudio = false)
        val plan = buildPlan(
            project(
                tracks = listOf(
                    Track(
                        type = TrackType.VIDEO,
                        clips = listOf(clip("lost", "gone", 0L, 1_000L))
                    ),
                    Track(
                        type = TrackType.OVERLAY,
                        clips = listOf(
                            clip("img1", "img", 0L, 2_000L, ClipType.IMAGE)
                        )
                    )
                ),
                assets = listOf(image)
            )
        )

        assertEquals(1, plan.videoItems.size)
        assertEquals("img1", plan.videoItems.single().clipId)
        assertTrue(plan.videoItems.single().isImage)
        assertTrue(plan.hasVideo)
    }

    @Test
    fun imageDurationUsesSpeedAndRemovesAudio() {
        val image = asset("img", kind = MediaKind.IMAGE, hasAudio = false)
        val plan = buildPlan(
            project(
                tracks = listOf(
                    Track(
                        type = TrackType.VIDEO,
                        clips = listOf(
                            clip(
                                "img1",
                                "img",
                                startMs = 0L,
                                durationMs = 3_000L,
                                type = ClipType.IMAGE,
                                sourceDurationMs = 3_000L,
                                speed = 1.5f
                            )
                        )
                    )
                ),
                assets = listOf(image)
            )
        )

        val item = plan.videoItems.single()
        assertTrue(item.isImage)
        assertTrue(item.removeAudio)
        assertEquals(4_500L, item.sourceDurationMs)
        assertFalse(plan.hasAudio)
    }

    @Test
    fun prefersPrivateCopyAndExtendsDurationToLastClip() {
        val video = asset("v1", localPath = "/private/v1.mp4")
        val plan = buildPlan(
            project(
                tracks = listOf(
                    Track(
                        type = TrackType.VIDEO,
                        clips = listOf(clip("c1", "v1", startMs = 1_000L, durationMs = 4_000L))
                    )
                ),
                assets = listOf(video),
                durationMs = 2_000L,
                frameRate = 5
            )
        )

        assertEquals("/private/v1.mp4", plan.videoItems.single().source)
        assertEquals(5_000L, plan.durationMs)
        assertEquals(15, plan.frameRate)
    }

    @Test
    fun mutedVideoTrackSilencesItsAudio() {
        val video = asset("v1", hasAudio = true)
        val plan = buildPlan(
            project(
                tracks = listOf(
                    Track(
                        type = TrackType.VIDEO,
                        isMuted = true,
                        clips = listOf(clip("c1", "v1", startMs = 0L, durationMs = 1_000L))
                    )
                ),
                assets = listOf(video)
            )
        )

        assertTrue(plan.videoItems.single().removeAudio)
        assertFalse(plan.hasAudio)
    }

    @Test
    fun nextVisualItemFindsCurrentOrFollowingItem() {
        val video = asset("v1")
        val image = asset("img", kind = MediaKind.IMAGE, hasAudio = false)
        val plan = buildPlan(
            project(
                tracks = listOf(
                    Track(
                        type = TrackType.VIDEO,
                        clips = listOf(
                            clip("img1", "img", startMs = 0L, durationMs = 3_000L, type = ClipType.IMAGE),
                            clip("c2", "v1", startMs = 3_000L, durationMs = 2_000L)
                        )
                    )
                ),
                assets = listOf(video, image),
                durationMs = 5_000L
            )
        )

        assertTrue(plan.nextVisualItem(0L)?.isImage == true)
        assertTrue(plan.nextVisualItem(2_999L)?.isImage == true)
        assertFalse(plan.nextVisualItem(3_000L)?.isImage == true)
        assertNull(plan.nextVisualItem(5_000L))
    }

    @Test
    fun rejectsMediaOutsideAllowedRootAndNetworkUris() {
        val outside = MediaAsset(
            id = "outside",
            uri = "/data/user/0/other/files/x.mp4",
            displayName = "outside",
            kind = MediaKind.VIDEO,
            localPath = "/data/user/0/other/files/x.mp4"
        )
        val remote = MediaAsset(
            id = "remote",
            uri = "https://example.com/video.mp4",
            displayName = "remote",
            kind = MediaKind.VIDEO
        )
        val inside = asset("inside", localPath = "/data/user/0/com.zaaam.zcut/files/media/ok.mp4")
        val plan = buildPlan(
            project(
                tracks = listOf(
                    Track(
                        type = TrackType.VIDEO,
                        clips = listOf(
                            clip("c1", "outside", 0L, 1_000L),
                            clip("c2", "remote", 1_000L, 1_000L),
                            clip("c3", "inside", 2_000L, 1_000L)
                        )
                    )
                ),
                assets = listOf(outside, remote, inside)
            ),
            allowedLocalRoot = "/data/user/0/com.zaaam.zcut/files"
        )

        assertEquals(listOf("c3"), plan.videoItems.map { it.clipId })
    }

    @Test
    fun fallsBackToContentUriWhenLocalCopyIsRejected() {
        val asset = MediaAsset(
            id = "v1",
            uri = "content://media/external/video/1",
            displayName = "v1",
            kind = MediaKind.VIDEO,
            localPath = "/tmp/not-app-media.mp4"
        )
        val plan = buildPlan(
            project(
                tracks = listOf(
                    Track(
                        type = TrackType.VIDEO,
                        clips = listOf(clip("c1", "v1", 0L, 1_000L))
                    )
                ),
                assets = listOf(asset)
            ),
            allowedLocalRoot = "/data/user/0/com.zaaam.zcut/files"
        )

        assertEquals("content://media/external/video/1", plan.videoItems.single().source)
    }

    @Test
    fun rejectsParentTraversalAndFileUris() {
        val traversal = MediaAsset(
            id = "traversal",
            uri = "file:///data/user/0/com.zaaam.zcut/shared_prefs/zcut_preferences.preferences_pb",
            displayName = "traversal",
            kind = MediaKind.VIDEO,
            localPath = "/data/user/0/com.zaaam.zcut/files/../shared_prefs/zcut_preferences.preferences_pb"
        )
        val fileUri = MediaAsset(
            id = "file",
            uri = "file:///data/user/0/com.zaaam.zcut/files/media/private.mp4",
            displayName = "file",
            kind = MediaKind.VIDEO
        )
        val plan = buildPlan(
            project(
                tracks = listOf(
                    Track(
                        type = TrackType.VIDEO,
                        clips = listOf(
                            clip("c1", "traversal", 0L, 1_000L),
                            clip("c2", "file", 1_000L, 1_000L)
                        )
                    )
                ),
                assets = listOf(traversal, fileUri)
            ),
            allowedLocalRoot = "/data/user/0/com.zaaam.zcut/files"
        )

        assertTrue(plan.videoItems.isEmpty())
    }

    @Test
    fun trailingGapCoversTimelineTailPerSequence() {
        val video = asset("v1")
        val audio = asset("a1", kind = MediaKind.AUDIO)
        val plan = buildPlan(
            project(
                tracks = listOf(
                    Track(
                        type = TrackType.VIDEO,
                        clips = listOf(clip("c1", "v1", 0L, 1_000L))
                    ),
                    Track(
                        type = TrackType.AUDIO,
                        clips = listOf(clip("a1", "a1", 0L, 2_000L, ClipType.AUDIO))
                    )
                ),
                assets = listOf(video, audio),
                durationMs = 5_000L
            )
        )

        assertEquals(5_000L, plan.durationMs)
        assertEquals(4_000L, plan.trailingGapMs(plan.videoItems))
        assertEquals(3_000L, plan.trailingGapMs(plan.audioItems))
    }

    @Test
    fun emptyProjectIsNotPlayable() {
        val plan = buildPlan(project(tracks = emptyList(), assets = emptyList()))

        assertFalse(plan.hasVideo)
        assertFalse(plan.hasAudio)
        assertFalse(plan.isPlayable)
        assertEquals(0L, plan.durationMs)
    }
}
