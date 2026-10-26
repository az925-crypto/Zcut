package com.zaaam.zcut.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdMakerTest {

    @Test
    fun linesIgnoreBlankRowsAndBullets() {
        val lines = AdScript.lines(
            "  - Hook line  \n\nBenefit one\n   \n- CTA now\n"
        )

        assertEquals(listOf("Hook line", "Benefit one", "CTA now"), lines)
    }

    @Test
    fun linesAreCappedSoOnePasteCannotExplodeTheDraft() {
        val script = (1..20).joinToString("\n") { "line $it" }

        assertEquals(AdScript.MAX_LINES, AdScript.lines(script).size)
        assertTrue(AdScript.lines("x".repeat(5_000)).all { it.length <= AdScript.MAX_LINE_CHARS })
    }

    @Test
    fun firstLineIsHookLastIsCtaAndTheRestAreBenefits() {
        assertEquals(AdSceneRole.HOOK, AdScript.roleFor(0, 1))
        assertEquals(AdSceneRole.HOOK, AdScript.roleFor(0, 3))
        assertEquals(AdSceneRole.BENEFIT, AdScript.roleFor(1, 3))
        assertEquals(AdSceneRole.CTA, AdScript.roleFor(2, 3))
        assertEquals(AdSceneRole.CTA, AdScript.roleFor(1, 2))
    }

    @Test
    fun scenesAreSequentialAndNeverOverlap() {
        val scenes = AdScript.scenes("First line\nSecond line\nThird line")

        assertEquals(3, scenes.size)
        assertEquals(0L, scenes.first().startMs)
        scenes.zipWithNext().forEach { (first, second) ->
            assertEquals(first.endMs, second.startMs)
            assertTrue(first.durationMs > 0L)
        }
        assertEquals(scenes.last().endMs, AdScript.totalDurationMs(scenes))
    }

    @Test
    fun measuredVoiceoverDurationsWinOverTheEstimate() {
        val scenes = AdScript.scenes(
            script = "Short\nMuch longer line",
            voiceDurationsMs = listOf(4_200L, 1_100L)
        )

        assertEquals(4_200L, scenes[0].durationMs)
        assertEquals(1_100L, scenes[1].durationMs)
        assertEquals(5_300L, AdScript.totalDurationMs(scenes))
    }

    @Test
    fun aMissingMeasurementFallsBackToTheEstimate() {
        val scenes = AdScript.scenes(
            script = "Measured line\nEstimated line",
            voiceDurationsMs = listOf(2_000L, 0L)
        )

        assertEquals(2_000L, scenes[0].durationMs)
        assertTrue(scenes[1].durationMs >= AdScript.MIN_SCENE_MS)
    }

    @Test
    fun briefKeepsOnlyOneAudioBedAndTrimsTheProduct() {
        val brief = AdBrief(
            script = "Hook",
            product = "  Orbit Bottle  ",
            voiceover = true,
            music = true
        ).sanitized()

        assertEquals("Orbit Bottle", brief.product)
        assertTrue(brief.voiceover)
        assertFalse(brief.music)
        assertTrue(brief.isReady)
    }

    @Test
    fun anEmptyScriptIsNotReady() {
        val brief = AdBrief(script = "   \n\n ").sanitized()

        assertTrue(brief.scenes.isEmpty())
        assertFalse(brief.isReady)
    }
}
