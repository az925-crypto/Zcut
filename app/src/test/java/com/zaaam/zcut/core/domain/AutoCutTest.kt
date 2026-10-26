package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.ClipType
import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.core.model.MediaKind
import com.zaaam.zcut.core.model.TrackType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class AutoCutTest {

    private val sampleRate = 8_000

    /** [seconds] of full-scale-free sine, used as "loud" material. */
    private fun tone(seconds: Double, amplitude: Double = 0.5): FloatArray {
        val count = (seconds * sampleRate).toInt()
        return FloatArray(count) { index ->
            (amplitude * sin(2 * PI * 440.0 * index / sampleRate)).toFloat()
        }
    }

    private fun silence(seconds: Double): FloatArray = FloatArray((seconds * sampleRate).toInt())

    private fun asset(
        id: String = "v1",
        kind: MediaKind = MediaKind.VIDEO,
        durationMs: Long = 30_000L
    ): MediaAsset = MediaAsset(
        id = id,
        uri = "content://test/$id",
        displayName = "$id.mp4",
        mimeType = "video/mp4",
        kind = kind,
        durationMs = durationMs,
        localPath = "/workspace/$id.mp4"
    )

    private fun window(startMs: Long, score: Int, kind: AutoCut.Kind) =
        AutoCut.Window(startMs = startMs, durationMs = 1_000L, score = score, kind = kind)

    @Test
    fun silenceReadsAsTheFloorLevel() {
        val envelope = AutoCut.energyEnvelope(silence(1.0), sampleRate)

        assertEquals(4, envelope.size)
        envelope.forEach { assertEquals(AutoCut.SILENCE_DBFS, it, 0.001f) }
    }

    @Test
    fun aToneReadsLouderThanSilence() {
        val envelope = AutoCut.energyEnvelope(silence(1.0) + tone(1.0), sampleRate)

        assertEquals(8, envelope.size)
        val quiet = envelope.take(4).maxOrNull()!!
        val loud = envelope.drop(4).minOrNull()!!
        assertEquals(AutoCut.SILENCE_DBFS, quiet, 0.001f)
        assertTrue("a -6 dBFS sine should be well above the floor", loud > -10f)
    }

    @Test
    fun aDegenerateInputYieldsNoEnvelope() {
        assertEquals(0, AutoCut.energyEnvelope(FloatArray(0), sampleRate).size)
        assertEquals(0, AutoCut.energyEnvelope(tone(1.0), 0).size)
    }

    @Test
    fun candidatesSlideOneFrameAtATimeAndKeepTheTargetLength() {
        val envelope = FloatArray(40) { -20f }

        val candidates = AutoCut.candidates(envelope, targetDurationMs = 2_000L)

        // 40 frames of 250 ms: starts 0..32, each 8 frames long.
        assertEquals(33, candidates.size)
        assertEquals(0L, candidates.first().startMs)
        assertEquals(8_000L, candidates.last().startMs)
        candidates.forEach { assertEquals(2_000L, it.durationMs) }
    }

    @Test
    fun aSourceShorterThanTheTargetBecomesOneWindow() {
        val envelope = FloatArray(4) { -20f }

        val candidates = AutoCut.candidates(envelope, targetDurationMs = 15_000L)

        assertEquals(1, candidates.size)
        assertEquals(1_000L, candidates.single().durationMs)
    }

    @Test
    fun theScoreFollowsTheLevel() {
        val quiet = AutoCut.candidates(FloatArray(8) { AutoCut.SILENCE_DBFS }, 2_000L)
        val loud = AutoCut.candidates(FloatArray(8) { AutoCut.LOUD_DBFS }, 2_000L)

        assertEquals(0, quiet.single().score)
        assertEquals(100, loud.single().score)
    }

    @Test
    fun kindFollowsTheMean() {
        val envelope = FloatArray(40) { -20f }
        envelope[0] = -5f
        envelope[20] = -45f

        val candidates = AutoCut.candidates(envelope, 250L)

        assertEquals(AutoCut.Kind.LOUD, candidates.first { it.startMs == 0L }.kind)
        assertEquals(AutoCut.Kind.QUIET, candidates.first { it.startMs == 5_000L }.kind)
        assertEquals(AutoCut.Kind.STEADY, candidates.first { it.startMs == 2_000L }.kind)
    }

    @Test
    fun selectPicksAtMostThreeAndKeepsPlaybackOrder() {
        val candidates = listOf(
            window(3_000L, 50, AutoCut.Kind.STEADY),
            window(0L, 90, AutoCut.Kind.LOUD),
            window(8_000L, 70, AutoCut.Kind.STEADY),
            window(6_000L, 99, AutoCut.Kind.LOUD)
        )

        val picked = AutoCut.select(candidates)

        assertEquals(3, picked.size)
        assertEquals(listOf(0L, 6_000L, 8_000L), picked.map { it.startMs })
    }

    @Test
    fun selectNeverReturnsTwoOverlappingWindows() {
        val overlapping = listOf(
            window(0L, 90, AutoCut.Kind.LOUD),
            window(0L, 80, AutoCut.Kind.STEADY),
            window(500L, 70, AutoCut.Kind.STEADY),
            window(5_000L, 60, AutoCut.Kind.QUIET)
        )

        val picked = AutoCut.select(overlapping, max = 4)

        assertEquals(listOf(0L, 5_000L), picked.map { it.startMs })
    }

    @Test
    fun theStyleNudgeBreaksATieTowardsThePreferredKind() {
        val loud = window(0L, 80, AutoCut.Kind.LOUD)
        val steady = window(4_000L, 80, AutoCut.Kind.STEADY)

        val balanced = AutoCut.select(listOf(loud, steady), max = 1)
        val cinematic = AutoCut.select(listOf(loud, steady), max = 1, prefer = AutoCut.Kind.STEADY)

        assertEquals(loud, balanced.single())
        assertEquals(steady, cinematic.single())
    }

    @Test
    fun draftTurnsWindowsIntoClipsWithTheirSourceOffsets() {
        val asset = asset()

        val project = AutoCut.draft(
            asset = asset,
            windows = listOf(
                AutoCut.Window(12_400L, 15_000L, 92, AutoCut.Kind.LOUD),
                AutoCut.Window(20_000L, 15_000L, 87, AutoCut.Kind.STEADY)
            ),
            name = "Shorts"
        )

        val clips = project.timeline.tracks.first { it.type == TrackType.VIDEO }.clips
        assertEquals(2, clips.size)

        assertEquals(0L, clips[0].startMs)
        assertEquals(15_000L, clips[0].durationMs)
        assertEquals(12_400L, clips[0].sourceStartMs)
        assertEquals(ClipType.VIDEO, clips[0].type)

        // The second window would run past the 30 s asset, so it is shortened.
        assertEquals(15_000L, clips[1].startMs)
        assertEquals(10_000L, clips[1].durationMs)
        assertEquals(20_000L, clips[1].sourceStartMs)

        assertEquals(listOf(asset.id), project.assets.map { it.id })
        assertEquals("Shorts", project.name)
    }

    @Test
    fun aWindowPastTheEndOfTheAssetIsDropped() {
        val project = AutoCut.draft(
            asset = asset(durationMs = 10_000L),
            windows = listOf(AutoCut.Window(30_000L, 15_000L, 90, AutoCut.Kind.LOUD)),
            name = "Shorts"
        )

        val clips = project.timeline.tracks.first { it.type == TrackType.VIDEO }.clips
        assertTrue("nothing to place means no clip at all", clips.isEmpty())
        assertTrue(project.assets.isEmpty())
    }

    @Test
    fun anUnknownAssetLengthKeepsTheWindowWhole() {
        val project = AutoCut.draft(
            asset = asset(durationMs = 0L),
            windows = listOf(AutoCut.Window(4_000L, 15_000L, 90, AutoCut.Kind.LOUD)),
            name = "Shorts"
        )

        val clips = project.timeline.tracks.first { it.type == TrackType.VIDEO }.clips
        assertEquals(15_000L, clips.single().durationMs)
        assertEquals(4_000L, clips.single().sourceStartMs)
    }

    @Test
    fun audioCannotBecomeAClipperDraft() {
        val project = AutoCut.draft(
            asset = asset(kind = MediaKind.AUDIO),
            windows = listOf(AutoCut.Window(0L, 5_000L, 90, AutoCut.Kind.LOUD)),
            name = "Shorts"
        )

        assertTrue(project.timeline.tracks.none { it.clips.isNotEmpty() })
    }

    @Test
    fun aBlankNameFallsBackToTheFileName() {
        val project = AutoCut.draft(
            asset = asset(id = "vlog"),
            windows = listOf(AutoCut.Window(0L, 5_000L, 90, AutoCut.Kind.LOUD)),
            name = "   "
        )

        assertEquals("vlog.mp4", project.name)
    }
}