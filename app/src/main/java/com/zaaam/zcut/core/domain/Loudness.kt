package com.zaaam.zcut.core.domain

import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Local loudness normalisation ("AudioLoudnessUnify" parity).
 *
 * Measures the RMS level of a decoded waveform and returns the gain multiplier
 * that moves it to a target level. The target is a common broadcast reference,
 * not a value lifted from the original app.
 */
object Loudness {

    /** Roughly -16 dBFS RMS, a typical streaming dialogue/music target. */
    const val TARGET_DBFS = -16f

    /** A single click must not be able to apply an extreme gain. */
    const val MAX_GAIN = 4f
    const val MIN_GAIN = 0.25f

    /** RMS level in dBFS, or null when the signal is effectively silent. */
    fun rmsDbfs(samples: FloatArray): Float? {
        if (samples.isEmpty()) return null
        var sum = 0.0
        for (sample in samples) {
            sum += sample.toDouble() * sample.toDouble()
        }
        val rms = sqrt(sum / samples.size)
        if (rms <= 1e-6) return null
        return (20.0 * log10(rms)).toFloat()
    }

    /**
     * Gain to reach [targetDbfs] from the measured level, clamped so a near
     * silent recording cannot be amplified into noise. Silence returns 1.
     */
    fun normalizeGain(
        samples: FloatArray,
        targetDbfs: Float = TARGET_DBFS,
        maxGain: Float = MAX_GAIN
    ): Float {
        val measured = rmsDbfs(samples) ?: return 1f
        val gain = 10.0.pow(((targetDbfs - measured) / 20.0)).toFloat()
        val ceiling = maxGain.coerceAtLeast(1f)
        return gain.coerceIn(MIN_GAIN, ceiling)
    }
}
