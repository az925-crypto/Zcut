package com.zaaam.zcut.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DigitalHumanTest {

    @Test
    fun catalogFallsBackToTheDefaultAvatar() {
        assertEquals(DigitalHumanCatalog.default, DigitalHumanCatalog.byId(null))
        assertEquals(DigitalHumanCatalog.default, DigitalHumanCatalog.byId("does-not-exist"))
        assertEquals("mika", DigitalHumanCatalog.byId("mika").id)
    }

    @Test
    fun catalogIdsAreUniqueAndComplete() {
        val ids = DigitalHumanCatalog.avatars.map { it.id }

        assertEquals(ids.size, ids.toSet().size)
        assertEquals(4, ids.size)
        DigitalHumanCatalog.avatars.forEach { avatar ->
            assertTrue(avatar.name.isNotBlank())
            assertTrue(avatar.background.isNotBlank())
            assertTrue(avatar.artKey.isNotBlank())
        }
    }

    @Test
    fun requestIsOnlyReadyWithAScript() {
        assertFalse(DigitalHumanRequest().isReady)
        assertFalse(DigitalHumanRequest(script = "   ").sanitized().isReady)
        assertTrue(DigitalHumanRequest(script = "hello").isReady)
    }

    @Test
    fun sanitizeClampsScriptRatePitchAndAvatar() {
        val request = DigitalHumanRequest(
            avatarId = "nope",
            script = "  " + "a".repeat(DigitalHumanRequest.MAX_SCRIPT + 100) + "  ",
            rate = 9f,
            pitch = 0.01f
        ).sanitized()

        assertEquals(DigitalHumanCatalog.default.id, request.avatarId)
        assertEquals(DigitalHumanRequest.MAX_SCRIPT, request.script.length)
        assertFalse(request.script.startsWith(" "))
        assertEquals(DigitalHumanRequest.MAX_RATE, request.rate)
        assertEquals(DigitalHumanRequest.MIN_PITCH, request.pitch)
    }

    @Test
    fun captionsAreEmptyWithoutAScriptOrDuration() {
        assertEquals(emptyList<CaptionSegmenter.Line>(), DigitalHumanPlan.captions("hi", 0L))
        assertEquals(emptyList<CaptionSegmenter.Line>(), DigitalHumanPlan.captions("   ", 5_000L))
    }

    @Test
    fun captionsCoverTheWholeVoiceTrack() {
        val script = "Welcome back to the channel. Today we are cutting a night-city " +
            "short in under a minute."
        val lines = DigitalHumanPlan.captions(script, 12_000L)

        assertTrue(lines.size > 1)
        assertEquals(0L, lines.first().startMs)
        assertEquals(12_000L, lines.last().endMs)
        lines.zipWithNext().forEach { (first, second) ->
            assertEquals(first.endMs, second.startMs)
        }
        // Every word of the script survives the split.
        assertEquals(
            script.split(Regex("\\s+")).filter { it.isNotEmpty() },
            lines.joinToString(" ") { it.text }.split(Regex("\\s+"))
        )
    }

    @Test
    fun captionLinesRespectTheCharacterBudget() {
        val lines = DigitalHumanPlan.captions("a ".repeat(60).trim(), 5_000L, maxChars = 20)

        assertTrue(lines.isNotEmpty())
        lines.forEach { assertTrue(it.text.length <= 20) }
    }
}