package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.AspectRatio
import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.core.model.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerationTest {

    private fun asset(
        id: String = "g1",
        kind: MediaKind = MediaKind.IMAGE,
        localPath: String? = "/workspace/$id.png"
    ): MediaAsset = MediaAsset(
        id = id,
        uri = "content://test/$id",
        displayName = "$id.png",
        mimeType = "image/png",
        kind = kind,
        localPath = localPath
    )

    private fun entry(
        id: String = "g1",
        prompt: String = "neon alley",
        createdAtMs: Long = 1_000L,
        seed: Long = 42L,
        asset: MediaAsset = asset(id)
    ): GenerationEntry = GenerationEntry(
        id = id,
        prompt = prompt,
        type = GenerationType.IMAGE,
        style = GenerationStyle.CINEMATIC,
        aspectRatio = AspectRatio.PORTRAIT_9_16,
        asset = asset,
        createdAtMs = createdAtMs,
        seed = seed
    )

    @Test
    fun requestIsOnlyReadyWithText() {
        assertFalse(GenerationRequest().isReady)
        assertFalse(GenerationRequest(prompt = "   ").sanitized().isReady)
        assertTrue(GenerationRequest(prompt = "a shot").isReady)
    }

    @Test
    fun sanitizeTrimsAndClampsEveryField() {
        val request = GenerationRequest(
            prompt = "  " + "a".repeat(GenerationRequest.MAX_PROMPT + 50) + "  ",
            negativePrompt = "b".repeat(GenerationRequest.MAX_NEGATIVE + 10),
            durationMs = 99_000L,
            seed = -5L
        ).sanitized()

        assertEquals(GenerationRequest.MAX_PROMPT, request.prompt.length)
        assertEquals(GenerationRequest.MAX_NEGATIVE, request.negativePrompt.length)
        assertEquals(GenerationRequest.MAX_VIDEO_DURATION_MS, request.durationMs)
        assertEquals(0L, request.seed)
        assertFalse(request.prompt.startsWith(" "))
    }

    @Test
    fun durationIsClampedToTheSupportedWindow() {
        assertEquals(
            GenerationRequest.MIN_VIDEO_DURATION_MS,
            GenerationRequest(durationMs = 0L).sanitized().durationMs
        )
        assertEquals(
            GenerationRequest.MAX_VIDEO_DURATION_MS,
            GenerationRequest(durationMs = Long.MAX_VALUE).sanitized().durationMs
        )
    }

    @Test
    fun seedIsDeterministicAndPromptSpecific() {
        val first = GenerationRequest.fromPrompt("neon alley").seed
        val again = GenerationRequest.fromPrompt("neon alley").seed
        val other = GenerationRequest.fromPrompt("sunny beach").seed

        assertEquals(first, again)
        assertNotEquals(first, other)
        assertTrue(first >= 0L)
        assertTrue(other >= 0L)
    }

    @Test
    fun fromPromptSanitizesAndCarriesChoices() {
        val request = GenerationRequest.fromPrompt(
            prompt = "  a shot  ",
            type = GenerationType.VIDEO,
            style = GenerationStyle.ANIME,
            aspectRatio = AspectRatio.SQUARE_1_1
        )

        assertEquals("a shot", request.prompt)
        assertEquals(GenerationType.VIDEO, request.type)
        assertEquals(GenerationStyle.ANIME, request.style)
        assertEquals(AspectRatio.SQUARE_1_1, request.aspectRatio)
        assertEquals(request.derivedSeed(), request.seed)
    }

    @Test
    fun addPutsNewestFirstAndDropsTheSameId() {
        val old = entry(id = "g1", createdAtMs = 1_000L)
        val newer = entry(id = "g2", createdAtMs = 2_000L)

        val result = GenerationHistory.add(listOf(old), newer)

        assertEquals(listOf("g2", "g1"), result.map { it.id })
        val replaced = GenerationHistory.add(result, old.copy(createdAtMs = 3_000L))
        assertEquals(listOf("g1", "g2"), replaced.map { it.id })
        assertEquals(3_000L, replaced.first().createdAtMs)
    }

    @Test
    fun addCapsTheHistory() {
        var entries = emptyList<GenerationEntry>()
        repeat(GenerationEntry.MAX_ENTRIES + 5) { index ->
            entries = GenerationHistory.add(
                entries,
                entry(id = "g$index", createdAtMs = index.toLong())
            )
        }

        assertEquals(GenerationEntry.MAX_ENTRIES, entries.size)
        assertEquals("g${GenerationEntry.MAX_ENTRIES + 4}", entries.first().id)
        assertEquals("g5", entries.last().id)
    }

    @Test
    fun sanitizeDropsBrokenEntries() {
        val clean = entry(id = "ok")
        val blankId = entry(id = " ")
        val noFile = entry(
            id = "nofile",
            asset = asset(id = "nofile", localPath = null).copy(uri = "")
        )
        val noPrompt = entry(id = "noprompt", prompt = "  ")

        val result = GenerationHistory.sanitize(
            listOf(blankId, noFile, clean, noPrompt, clean.copy(createdAtMs = 9_000L))
        )

        // Blank prompts are allowed (the display name fills in), but an unusable
        // asset is not: it could never become a draft.
        // Both the clean entry and the blank-prompt entry survive; the blank id,
        // the asset-less entry and the duplicate id are dropped.
        assertEquals(listOf("ok", "noprompt"), result.map { it.id }.distinct())
        assertTrue(result.none { it.asset.sourcePath == null })
        assertEquals(1, result.count { it.id == "ok" })
        assertTrue(result.any { it.id == "noprompt" })
    }

    @Test
    fun sanitizeClampsTextAndOrder() {
        val long = entry(id = "g1", prompt = "p".repeat(5_000), createdAtMs = 10L)
        val newer = entry(id = "g2", prompt = " q ", createdAtMs = 20L)

        val result = GenerationHistory.sanitize(listOf(long, newer))

        assertEquals(listOf("g2", "g1"), result.map { it.id })
        assertEquals(GenerationRequest.MAX_PROMPT, result.last().prompt.length)
        assertEquals("q", result.first().prompt)
    }

    @Test
    fun sanitizeHandlesAnEmptyCap() {
        assertEquals(emptyList<GenerationEntry>(), GenerationHistory.sanitize(listOf(entry()), 0))
        assertEquals(emptyList<GenerationEntry>(), GenerationHistory.add(emptyList(), entry(), 0))
    }

    @Test
    fun negativeSeedIsClampedWhenReadBack() {
        val result = GenerationHistory.sanitize(listOf(entry(seed = -99L)))

        assertEquals(0L, result.single().seed)
    }
}
