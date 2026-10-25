package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.ClipType
import com.zaaam.zcut.core.model.TextConfig
import com.zaaam.zcut.core.model.Timeline
import com.zaaam.zcut.core.model.Track
import com.zaaam.zcut.core.model.TrackType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TextEditorTest {

    private fun timeline(): Timeline = Timeline(
        tracks = listOf(
            Track(type = TrackType.VIDEO),
            Track(type = TrackType.TEXT)
        )
    )

    private fun config(text: String = "Halo") = TextConfig(text = text)

    @Test
    fun addTextCreatesClipOnExistingTextTrack() {
        val updated = TextEditor.addText(timeline(), config(), atMs = 0L)
        val textTrack = updated.tracks.first { it.type == TrackType.TEXT }

        assertEquals(1, textTrack.clips.size)
        assertEquals(ClipType.TEXT, textTrack.clips.single().type)
        assertEquals("Halo", textTrack.clips.single().text?.text)
        assertEquals(TextEditor.DEFAULT_DURATION_MS, updated.durationMs)
    }

    @Test
    fun addTextCreatesTextTrackWhenDraftHasNone() {
        val withoutText = Timeline(tracks = listOf(Track(type = TrackType.VIDEO)))

        val updated = TextEditor.addText(withoutText, config(), atMs = 500L)

        val textTrack = updated.tracks.firstOrNull { it.type == TrackType.TEXT }
        assertNotNull(textTrack)
        assertEquals(1, textTrack!!.clips.size)
        assertEquals(500L, textTrack.clips.single().startMs)
    }

    @Test
    fun addTextPushesRightWhenPlayheadIsOccupied() {
        val first = TextEditor.addText(timeline(), config("A"), atMs = 0L)
        val second = TextEditor.addText(first, config("B"), atMs = 0L)
        val clips = TextEditor.textClips(second)

        assertEquals(2, clips.size)
        assertEquals(0L, clips[0].startMs)
        assertEquals(TextEditor.DEFAULT_DURATION_MS, clips[1].startMs)
    }

    @Test
    fun addTextClampsNegativeStartAndTinyDuration() {
        val updated = TextEditor.addText(
            timeline = timeline(),
            config = config(),
            atMs = -500L,
            durationMs = 10L
        )
        val clip = TextEditor.textClips(updated).single()

        assertEquals(0L, clip.startMs)
        assertEquals(TextEditor.MIN_DURATION_MS, clip.durationMs)
    }

    @Test
    fun addTextRejectsBlankText() {
        val updated = TextEditor.addText(timeline(), config("   "), atMs = 0L)

        assertTrue(TextEditor.textClips(updated).isEmpty())
    }

    @Test
    fun setTextOnlyEditsTextClips() {
        val videoClip = Clip(
            id = "video",
            type = ClipType.VIDEO,
            startMs = 0L,
            durationMs = 1_000L,
            sourceDurationMs = 1_000L
        )
        val withVideo = Timeline(
            tracks = listOf(
                Track(type = TrackType.VIDEO, clips = listOf(videoClip)),
                Track(type = TrackType.TEXT)
            )
        )
        val untouched = TextEditor.setText(withVideo, "video", config("Nope"))
        assertEquals(withVideo, untouched)

        val withText = TextEditor.addText(withVideo, config("Lama"), atMs = 0L)
        val clipId = TextEditor.textClips(withText).single().id
        val updated = TextEditor.setText(withText, clipId, config("Baru"))

        assertEquals("Baru", TextEditor.textClips(updated).single().text?.text)
    }

    @Test
    fun activeAtUsesHalfOpenRange() {
        val timeline = TextEditor.addText(timeline(), config(), atMs = 1_000L, durationMs = 2_000L)

        assertEquals(1, TextEditor.activeAt(timeline, 1_000L).size)
        assertEquals(1, TextEditor.activeAt(timeline, 2_999L).size)
        assertTrue(TextEditor.activeAt(timeline, 3_000L).isEmpty())
        assertTrue(TextEditor.activeAt(timeline, 999L).isEmpty())
    }

    @Test
    fun labelUsesFirstNonBlankLine() {
        val timeline = TextEditor.addText(
            timeline(),
            config("\n  Judul malam  \nbaris kedua"),
            atMs = 0L
        )

        assertEquals("Judul malam", TextEditor.label(TextEditor.textClips(timeline).single()))
        assertEquals(
            "Teks",
            TextEditor.label(
                Clip(
                    type = ClipType.TEXT,
                    startMs = 0L,
                    durationMs = 1_000L,
                    text = TextConfig("   ")
                )
            )
        )
    }

    @Test
    fun sanitizeClampsAbsurdValuesAndKeepsTheClip() {
        val withText = TextEditor.addText(timeline(), config("aman"), atMs = 0L)
        val clip = TextEditor.textClips(withText).single()
        val corrupted = withText.copy(
            tracks = withText.tracks.map { track ->
                if (track.type == TrackType.TEXT) {
                    track.copy(
                        clips = listOf(
                            clip.copy(
                                text = TextConfig(
                                    text = "x".repeat(2_000),
                                    fontFamily = "comic",
                                    fontSizeSp = Float.NaN,
                                    letterSpacingEm = 5f,
                                    lineHeight = 0.1f,
                                    strokeWidth = 99f
                                )
                            )
                        )
                    )
                } else {
                    track
                }
            }
        )

        val fixed = TextEditor.textClips(TextEditor.sanitize(corrupted)).single().text!!
        assertEquals(TextEditor.MAX_TEXT_LENGTH, fixed.text.length)
        assertEquals(TextFonts.SANS, fixed.fontFamily)
        assertEquals(42f, fixed.fontSizeSp, 0.001f)
        assertEquals(1f, fixed.letterSpacingEm, 0.001f)
        assertEquals(0.8f, fixed.lineHeight, 0.001f)
        assertEquals(20f, fixed.strokeWidth, 0.001f)
    }

    @Test
    fun removeTextLeavesOtherTracksAlone() {
        val withText = TextEditor.addText(timeline(), config(), atMs = 0L)
        val clipId = TextEditor.textClips(withText).single().id

        val removed = TextEditor.removeText(withText, clipId)

        assertNull(removed.timelineClip(clipId))
        assertEquals(2, removed.tracks.size)
    }

    private fun Timeline.timelineClip(id: String): Clip? = clip(id)
}
