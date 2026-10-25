package com.zaaam.zcut.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class BeatDetectorTest {

    private val sampleRate = 8_000

    /** Impulse train with a short decaying click every [intervalMs]. */
    private fun clickTrack(durationMs: Long, intervalMs: Long): FloatArray {
        val samples = FloatArray((durationMs * sampleRate / 1_000L).toInt())
        var clickMs = 0L
        while (clickMs < durationMs) {
            val start = (clickMs * sampleRate / 1_000L).toInt()
            for (i in 0 until 80) {
                val index = start + i
                if (index >= samples.size) break
                samples[index] += 0.9f * (1f - i / 80f)
            }
            clickMs += intervalMs
        }
        return samples
    }

    @Test
    fun detectsOneBeatPerClick() {
        val beats = BeatDetector.detect(clickTrack(durationMs = 5_000L, intervalMs = 500L), sampleRate)

        assertTrue("expected around 10 beats, got ${beats.size}", beats.size in 9..11)
    }

    @Test
    fun detectedSpacingMatchesTheClickInterval() {
        val beats = BeatDetector.detect(clickTrack(durationMs = 5_000L, intervalMs = 500L), sampleRate)
        assertTrue(beats.size >= 3)

        val intervals = beats.zipWithNext { a, b -> b - a }
        intervals.forEach { interval ->
            assertTrue("unexpected interval $interval", abs(interval - 500L) <= 130L)
        }
    }

    @Test
    fun reportBpmOfAClickTrack() {
        val beats = BeatDetector.detect(clickTrack(durationMs = 6_000L, intervalMs = 500L), sampleRate)

        val bpm = BeatDetector.bpm(beats)
        assertTrue("unexpected bpm $bpm", bpm != null && abs(bpm - 120) <= 6)
    }

    @Test
    fun silenceHasNoBeats() {
        assertEquals(emptyList<Long>(), BeatDetector.detect(FloatArray(sampleRate), sampleRate))
    }

    @Test
    fun signalTooShortOrInvalidRateIsRejected() {
        assertEquals(emptyList<Long>(), BeatDetector.detect(FloatArray(64), sampleRate))
        assertEquals(emptyList<Long>(), BeatDetector.detect(FloatArray(4_096), 0))
    }

    @Test
    fun bpmNeedsAtLeastThreeBeats() {
        assertEquals(null, BeatDetector.bpm(listOf(0L, 500L)))
        assertEquals(null, BeatDetector.bpm(emptyList()))
    }
}
