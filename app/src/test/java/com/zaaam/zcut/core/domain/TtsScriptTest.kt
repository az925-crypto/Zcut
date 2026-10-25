package com.zaaam.zcut.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsScriptTest {

    @Test
    fun sanitizeTrimsAndLimitsLength() {
        assertEquals("halo", TtsScript.sanitize("  halo  "))
        assertEquals(TtsScript.MAX_LENGTH, TtsScript.sanitize("x".repeat(5_000)).length)
    }

    @Test
    fun estimateScalesWithLength() {
        val short = TtsScript.estimateDurationMs("hi")
        val long = TtsScript.estimateDurationMs("x".repeat(150))

        assertEquals(TtsScript.MIN_ESTIMATE_MS, short)
        assertEquals(10_000L, long)
        assertEquals(0L, TtsScript.estimateDurationMs("   "))
    }

    @Test
    fun cacheKeyIsDeterministic() {
        val first = TtsScript.cacheKey("halo dunia", "id-ID-warm", 1f, 1f)
        val second = TtsScript.cacheKey("halo dunia", "id-ID-warm", 1f, 1f)

        assertEquals(first, second)
        assertEquals(64, first.length)
    }

    @Test
    fun cacheKeyChangesWithEveryInput() {
        val base = TtsScript.cacheKey("halo", "v1", 1f, 1f)

        assertNotEquals(base, TtsScript.cacheKey("halo!", "v1", 1f, 1f))
        assertNotEquals(base, TtsScript.cacheKey("halo", "v2", 1f, 1f))
        assertNotEquals(base, TtsScript.cacheKey("halo", "v1", 1.2f, 1f))
        assertNotEquals(base, TtsScript.cacheKey("halo", "v1", 1f, 0.8f))
    }

    @Test
    fun fileNameUsesTheCacheKey() {
        val name = TtsScript.fileName("halo", "v1", 1f, 1f)

        assertTrue(name.startsWith("tts-"))
        assertTrue(name.endsWith(".wav"))
        assertTrue(name.contains(TtsScript.cacheKey("halo", "v1", 1f, 1f)))
    }
}
