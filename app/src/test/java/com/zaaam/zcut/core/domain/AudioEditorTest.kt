package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.AudioConfig
import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.ClipType
import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.core.model.MediaKind
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.model.Timeline
import com.zaaam.zcut.core.model.Track
import com.zaaam.zcut.core.model.TrackType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioEditorTest {

    private fun asset(
        id: String = "a1",
        kind: MediaKind = MediaKind.AUDIO,
        durationMs: Long = 5_000L
    ) = MediaAsset(
        id = id,
        displayName = "lagu.mp3",
        mimeType = "audio/mpeg",
        kind = kind,
        durationMs = durationMs,
        localPath = "/media/$id.mp3"
    )

    private fun timeline(vararg clips: Clip): Timeline = Timeline(
        tracks = listOf(
            Track(type = TrackType.VIDEO),
            Track(type = TrackType.AUDIO, clips = clips.toList())
        )
    )

    private fun audioClip(
        id: String = "c1",
        startMs: Long = 0L,
        durationMs: Long = 5_000L,
        audio: AudioConfig = AudioConfig()
    ) = Clip(
        id = id,
        assetId = "a1",
        type = ClipType.AUDIO,
        startMs = startMs,
        durationMs = durationMs,
        sourceDurationMs = durationMs,
        audio = audio
    )

    @Test
    fun setVolumeClampsAndIgnoresNonAudioClips() {
        val video = Clip(id = "v1", type = ClipType.VIDEO, startMs = 0L, durationMs = 1_000L)
        val base = timeline(video, audioClip())

        val loud = AudioEditor.setVolume(base, "c1", 5f)
        assertEquals(AudioEditor.MAX_VOLUME, loud.clip("c1")!!.audio.volume, 0.001f)

        val untouched = AudioEditor.setVolume(base, "v1", 0.5f)
        assertEquals(AudioConfig(), untouched.clip("v1")!!.audio)
    }

    @Test
    fun setVolumeIgnoresNaN() {
        val base = timeline(audioClip(audio = AudioConfig(volume = 0.8f)))

        val updated = AudioEditor.setVolume(base, "c1", Float.NaN)

        assertEquals(0.8f, updated.clip("c1")!!.audio.volume, 0.001f)
    }

    @Test
    fun fadesAreCappedAtTheClipDuration() {
        val base = timeline(audioClip(durationMs = 1_200L))

        val updated = AudioEditor.setFades(base, "c1", fadeInMs = 9_000L, fadeOutMs = -5L)
        val audio = updated.clip("c1")!!.audio

        assertEquals(1_200L, audio.fadeInMs)
        assertEquals(0L, audio.fadeOutMs)
    }

    @Test
    fun pitchIsClampedToTheSupportedRange() {
        val base = timeline(audioClip())

        val updated = AudioEditor.setPitch(base, "c1", 9f)

        assertEquals(AudioEditor.MAX_PITCH, updated.clip("c1")!!.audio.pitch, 0.001f)
    }

    @Test
    fun fadeGainRampsInAndOut() {
        // 2 s clip, 500 ms fade in, 500 ms fade out.
        assertEquals(0f, AudioEditor.fadeGain(0L, 2_000L, 500L, 500L), 0.001f)
        assertEquals(1f, AudioEditor.fadeGain(1_000L, 2_000L, 500L, 500L), 0.001f)
        assertEquals(0f, AudioEditor.fadeGain(2_000L, 2_000L, 500L, 500L), 0.001f)
        assertEquals(0.5f, AudioEditor.fadeGain(250L, 2_000L, 500L, 500L), 0.01f)
    }

    @Test
    fun fadeGainIsOneWithoutFades() {
        assertEquals(1f, AudioEditor.fadeGain(123L, 5_000L, 0L, 0L), 0.001f)
    }

    @Test
    fun muteWinsOverStoredVolume() {
        val config = AudioConfig(volume = 1.5f, muted = true)
        assertEquals(0f, AudioEditor.effectiveVolume(config), 0.001f)
        assertEquals(1.5f, AudioEditor.effectiveVolume(config.copy(muted = false)), 0.001f)
    }

    @Test
    fun sanitizeClampsEnvelopeWithoutDroppingClips() {
        val broken = timeline(
            audioClip(
                durationMs = 800L,
                audio = AudioConfig(volume = 99f, fadeInMs = 10_000L, pitch = 0f)
            )
        )

        val clips = AudioEditor.audioClips(AudioEditor.sanitize(broken))

        assertEquals(1, clips.size)
        val audio = clips.single().audio
        assertEquals(AudioEditor.MAX_VOLUME, audio.volume, 0.001f)
        assertEquals(800L, audio.fadeInMs)
        assertEquals(AudioEditor.MIN_PITCH, audio.pitch, 0.001f)
    }

    @Test
    fun insertAudioAddsAssetAndClipOnTheAudioTrack() {
        val project = Project(
            name = "P",
            timeline = Timeline(tracks = listOf(Track(type = TrackType.VIDEO)))
        )

        val updated = AudioEditor.insertAudio(project, asset(durationMs = 4_000L), atMs = 1_000L)
        val clips = AudioEditor.audioClips(updated.timeline)

        assertEquals(1, clips.size)
        assertEquals(1_000L, clips.single().startMs)
        assertEquals(4_000L, clips.single().durationMs)
        assertTrue(updated.assets.any { it.id == "a1" })
        assertNotNull(updated.timeline.tracks.firstOrNull { it.type == TrackType.AUDIO })
    }

    @Test
    fun insertAudioPushesRightWhenTheSlotIsTaken() {
        val existing = audioClip(id = "c1", startMs = 0L, durationMs = 3_000L)
        val project = Project(name = "P", timeline = timeline(existing))
            .copy(assets = listOf(asset()))

        val updated = AudioEditor.insertAudio(project, asset(id = "a2"), atMs = 1_000L)
        val clips = AudioEditor.audioClips(updated.timeline)

        assertEquals(2, clips.size)
        assertEquals(3_000L, clips[1].startMs)
    }

    @Test
    fun insertAudioRejectsNonAudioAssets() {
        val project = Project(name = "P", timeline = timeline())

        val updated = AudioEditor.insertAudio(
            project,
            asset(id = "v1", kind = MediaKind.VIDEO),
            atMs = 0L
        )

        assertEquals(project, updated)
    }

    @Test
    fun insertAudioDoesNotDuplicateAKnownAsset() {
        val known = asset(id = "a1")
        val project = Project(name = "P", timeline = timeline()).copy(assets = listOf(known))

        val updated = AudioEditor.insertAudio(project, known, atMs = 0L)

        assertEquals(1, updated.assets.size)
        assertEquals(1, AudioEditor.audioClips(updated.timeline).size)
    }
}
