package com.zaaam.zcut.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class LoudnessTest {

    private fun sine(amplitude: Float, count: Int = 16_000): FloatArray =
        FloatArray(count) { index ->
            (amplitude * sin(2.0 * PI * 440.0 * index / 16_000.0)).toFloat()
        }

    @Test
    fun emptySignalHasNoLevel() {
        assertNull(Loudness.rmsDbfs(FloatArray(0)))
    }

    @Test
    fun silenceIsTreatedAsSilent() {
        assertNull(Loudness.rmsDbfs(FloatArray(1_000)))
        assertEquals(1f, Loudness.normalizeGain(FloatArray(1_000)), 0.001f)
    }

    @Test
    fun quietSignalIsBoostedTowardsTheTarget() {
        // Amplitude 0.1 → ≈ -23 dBFS RMS → ≈ 2.2× gain to reach -16 dBFS.
        val gain = Loudness.normalizeGain(sine(0.1f))

        assertTrue("unexpected gain $gain", gain in 2.0f..2.5f)
    }

    @Test
    fun loudSignalIsTrimmed() {
        val gain = Loudness.normalizeGain(sine(0.9f))

        assertTrue("unexpected gain $gain", gain in 0.25f..0.5f)
    }

    @Test
    fun gainIsClampedAtTheCeiling() {
        val gain = Loudness.normalizeGain(sine(0.0005f), maxGain = 2f)

        assertEquals(2f, gain, 0.001f)
    }
}
