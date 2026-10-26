package com.zaaam.zcut.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The generator is pure Kotlin, so the whole render is assertable without an
 * Android runtime. These cases pin the WAV container, the determinism promise
 * and the beat grid the draft's snap uses.
 */
class MusicGeneratorTest {

    private val lofi = MusicCatalog.style(MusicMood.LOFI)

    @Test
    fun wavHeaderDescribesTheRenderedPcm() {
        val wav = MusicGenerator.wav(lofi, 8_000L, seed = 1)

        assertEquals("RIFF", ascii(wav, 0, 4))
        assertEquals("WAVE", ascii(wav, 8, 4))
        assertEquals("fmt ", ascii(wav, 12, 4))
        assertEquals("data", ascii(wav, 36, 4))

        val samples = 8_000 * MusicGenerator.SAMPLE_RATE / 1_000
        val dataSize = samples * 2
        assertEquals(dataSize, le32(wav, 40))
        assertEquals(wav.size, HEADER_BYTES + dataSize)
        assertEquals(HEADER_BYTES + dataSize - 8, le32(wav, 4))
    }

    @Test
    fun headerDeclaresMono16BitPcmAtTheGeneratorRate() {
        val wav = MusicGenerator.wav(lofi, 8_000L, seed = 1)

        assertEquals(1, le16(wav, 20))                       // PCM
        assertEquals(1, le16(wav, 22))                       // mono
        assertEquals(MusicGenerator.SAMPLE_RATE, le32(wav, 24))
        assertEquals(MusicGenerator.SAMPLE_RATE * 2, le32(wav, 28)) // byte rate
        assertEquals(2, le16(wav, 32))                       // block align
        assertEquals(16, le16(wav, 34))                      // bits per sample
    }

    @Test
    fun sameMoodDurationAndSeedRenderTheSameBytes() {
        val first = MusicGenerator.wav(lofi, 8_000L, seed = 7)
        val second = MusicGenerator.wav(lofi, 8_000L, seed = 7)

        assertTrue(first.contentEquals(second))
    }

    @Test
    fun aDifferentSeedRendersDifferentAudio() {
        val first = MusicGenerator.wav(lofi, 8_000L, seed = 7)
        val second = MusicGenerator.wav(lofi, 8_000L, seed = 8)

        assertNotEquals(first.size, 0)
        assertTrue("seed must change the render", !first.contentEquals(second))
    }

    @Test
    fun durationIsClampedToTheSupportedRange() {
        val short = MusicGenerator.wav(lofi, 1_000L, seed = 1)
        val long = MusicGenerator.wav(lofi, 600_000L, seed = 1)

        assertEquals(headerDataSize(short), minDataSize())
        assertEquals(headerDataSize(long), maxDataSize())
    }

    @Test
    fun beatGridStartsAtZeroAndFollowsTheTempo() {
        val grid = MusicGenerator.beatGrid(84, 24_000L)

        assertEquals(0L, grid.first())
        assertTrue("grid must be ascending", grid.zipWithNext().all { (a, b) -> b > a })
        assertTrue("grid must not run past the loop", grid.last() <= 24_000L)
        val expectedStep = (60_000.0 / 84).toLong()
        grid.zipWithNext().forEach { (a, b) ->
            assertEquals(expectedStep, b - a)
        }
        assertEquals(34, grid.size)
    }

    @Test
    fun beatGridRefusesNonsenseInput() {
        assertTrue(MusicGenerator.beatGrid(0, 24_000L).isEmpty())
        assertTrue(MusicGenerator.beatGrid(84, 0L).isEmpty())
    }

    @Test
    fun everyMoodHasAStyleWithAPlayableTempo() {
        assertEquals(MusicMood.entries.size, MusicCatalog.styles.size)
        MusicMood.entries.forEach { mood ->
            val style = MusicCatalog.style(mood)
            assertEquals(mood, style.mood)
            assertTrue("${style.label} needs a tempo", style.bpm > 0)
            assertTrue("${style.label} needs a key", style.keyLabel.isNotBlank())
            assertTrue("${style.label} needs a chord progression", style.progression.isNotEmpty())
            assertEquals("${style.label} needs a seven-note scale", 7, style.scale.size)
        }
    }

    @Test
    fun resultFileNamesAreStableAndNeverCarryAPath() {
        MusicMood.entries.forEach { mood ->
            val name = MusicGenerator.fileName(MusicCatalog.style(mood))
            assertTrue("$name must be a plain file name", name.endsWith("_loop.wav"))
            assertTrue("$name must not contain a separator", !name.contains('/'))
            assertTrue("$name must not contain a separator", !name.contains('\\'))
            assertTrue("$name must be lowercase ascii", name.all { it.isDigit() || it in 'a'..'z' })
        }
        assertEquals("lofi_loop.wav", MusicGenerator.fileName(lofi))
    }

    private fun headerDataSize(wav: ByteArray): Int = le32(wav, 40)

    private fun minDataSize(): Int =
        (MusicGenerator.MIN_DURATION_MS * MusicGenerator.SAMPLE_RATE / 1_000).toInt() * 2

    private fun maxDataSize(): Int =
        (MusicGenerator.MAX_DURATION_MS * MusicGenerator.SAMPLE_RATE / 1_000).toInt() * 2

    private fun ascii(bytes: ByteArray, offset: Int, length: Int): String =
        buildString {
            repeat(length) { append((bytes[offset + it].toInt() and 0xFF).toChar()) }
        }

    private fun le32(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)

    private fun le16(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)

    private companion object {
        const val HEADER_BYTES = 44
    }
}