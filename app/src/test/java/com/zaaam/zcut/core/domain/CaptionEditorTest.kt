package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.CaptionCue
import com.zaaam.zcut.core.model.TextConfig
import com.zaaam.zcut.core.model.TextStylePreset
import com.zaaam.zcut.core.model.Transcript
import com.zaaam.zcut.core.model.TranscriptSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptionEditorTest {

    @Test
    fun addCueKeepsTextAndOrdersByStart() {
        val first = CaptionEditor.addCue(Transcript(), "kedua", 5_000L, 6_000L)
        val both = CaptionEditor.addCue(first, "pertama", 1_000L, 2_000L)

        assertEquals(listOf("pertama", "kedua"), both.cues.map { it.text })
        assertEquals(TranscriptSource.MANUAL, both.source)
    }

    @Test
    fun addCueRejectsBlankText() {
        val transcript = CaptionEditor.addCue(Transcript(), "   ", 0L, 1_000L)

        assertTrue(transcript.cues.isEmpty())
    }

    @Test
    fun addCueEnforcesAMinimumDuration() {
        val transcript = CaptionEditor.addCue(Transcript(), "halo", 1_000L, 1_050L)

        val cue = transcript.cues.single()
        assertEquals(1_000L, cue.startMs)
        assertEquals(1_000L + CaptionEditor.MIN_DURATION_MS, cue.endMs)
    }

    @Test
    fun aHugeStartCannotOverflowIntoAnInvertedWindow() {
        // A hand-edited draft on disk can carry an absurd start; clamping it must
        // keep the window ordered instead of overflowing into a negative span.
        val transcript = CaptionEditor.addCue(
            Transcript(),
            "x",
            Long.MAX_VALUE - 1L,
            Long.MAX_VALUE
        )

        val cue = transcript.cues.single()
        assertTrue(cue.endMs >= cue.startMs)
        assertTrue(cue.endMs - cue.startMs >= CaptionEditor.MIN_DURATION_MS)
    }

    @Test
    fun updateCueEditsTextAndDropsAClearedLine() {
        val added = CaptionEditor.addCue(Transcript(), "halo", 0L, 1_000L)
        val id = added.cues.single().id

        val edited = CaptionEditor.updateCue(added, id, text = "hai")
        assertEquals("hai", edited.cues.single().text)

        val cleared = CaptionEditor.updateCue(edited, id, text = "   ")
        assertTrue(cleared.cues.isEmpty())
    }

    @Test
    fun updateCueIgnoresAnUnknownId() {
        val added = CaptionEditor.addCue(Transcript(), "halo", 0L, 1_000L)

        assertEquals(added, CaptionEditor.updateCue(added, "no-such-cue", text = "x"))
    }

    @Test
    fun removeCueDeletesOnlyThatLine() {
        val added = CaptionEditor.addCue(
            CaptionEditor.addCue(Transcript(), "satu", 0L, 1_000L),
            "dua",
            1_000L,
            2_000L
        )
        val target = added.cues.first()

        val remaining = CaptionEditor.removeCue(added, target.id)

        assertEquals(listOf("dua"), remaining.cues.map { it.text })
    }

    @Test
    fun replaceCuesKeepsTheProvenance() {
        val cues = listOf(
            CaptionCue(startMs = 0L, endMs = 900L, text = "hello world"),
            CaptionCue(startMs = 1_000L, endMs = 1_800L, text = "bye")
        )

        val replaced = CaptionEditor.replaceCues(
            Transcript(language = "en"),
            cues,
            TranscriptSource.IMPORTED_SRT
        )

        assertEquals(2, replaced.cues.size)
        assertEquals("en", replaced.language)
        assertEquals(TranscriptSource.IMPORTED_SRT, replaced.source)
    }

    @Test
    fun setStyleAppliesOneConfigToEveryCue() {
        val base = CaptionEditor.addCue(
            CaptionEditor.addCue(Transcript(), "satu", 0L, 1_000L),
            "dua",
            1_000L,
            2_000L
        )
        val style = TextConfig(
            text = "",
            fontSizeSp = 44f,
            stylePreset = TextStylePreset.OUTLINE
        )

        val styled = CaptionEditor.setStyle(base, style)

        styled.cues.forEach { cue ->
            assertEquals(44f, cue.style?.fontSizeSp ?: 0f, 0.0001f)
            assertEquals(TextStylePreset.OUTLINE, cue.style?.stylePreset)
        }
    }

    @Test
    fun cuesAtUsesAHalfOpenWindow() {
        val transcript = CaptionEditor.addCue(Transcript(), "halo", 1_000L, 2_000L)

        assertEquals(1, CaptionEditor.cuesAt(transcript, 1_000L).size)
        assertEquals(1, CaptionEditor.cuesAt(transcript, 1_999L).size)
        assertTrue(CaptionEditor.cuesAt(transcript, 2_000L).isEmpty())
        assertTrue(CaptionEditor.cuesAt(transcript, 999L).isEmpty())
    }

    @Test
    fun sanitizeDropsEmptyTextAndRepairsInvertedTimes() {
        val hostile = Transcript(
            cues = listOf(
                CaptionCue(startMs = 0L, endMs = 1_000L, text = "   "),
                CaptionCue(startMs = 5_000L, endMs = 4_000L, text = "rusak"),
                CaptionCue(startMs = -10L, endMs = 500L, text = "negatif")
            )
        )

        val clean = CaptionEditor.sanitize(hostile)

        assertEquals(listOf("negatif", "rusak"), clean.cues.map { it.text })
        assertEquals(0L, clean.cues.first().startMs)
        assertEquals(
            5_000L + CaptionEditor.MIN_DURATION_MS,
            clean.cues.last().endMs
        )
    }

    @Test
    fun styleOfFallsBackToTheDefaultLineStyle() {
        val cue = CaptionCue(startMs = 0L, endMs = 1_000L, text = "halo")

        assertEquals(
            CaptionEditor.defaultStyle().fontSizeSp,
            CaptionEditor.styleOf(cue).fontSizeSp,
            0.0001f
        )
    }
}