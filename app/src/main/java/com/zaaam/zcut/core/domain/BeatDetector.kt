package com.zaaam.zcut.core.domain

import kotlin.math.roundToInt

/**
 * Local beat detection over a decoded mono waveform.
 *
 * The detector never touches a file or a network: it works on energy onsets so
 * the same maths can run on a preview waveform or on a decoded music track. It
 * is intentionally simple (no ML model, no fingerprint) and says so in the UI.
 */
object BeatDetector {

    /** Two beats closer than this are treated as one (covers up to ~250 BPM). */
    const val DEFAULT_MIN_GAP_MS = 240L

    /** Multiplier over the local onset average; higher = fewer, stronger beats. */
    const val DEFAULT_SENSITIVITY = 1.5f

    private const val WINDOW_SAMPLES = 1_024
    private const val HOP_SAMPLES = 512
    private const val HISTORY_FRAMES = 20

    /**
     * Returns beat times in milliseconds, ascending. An empty result means the
     * audio has no usable onsets (silence, speech without rhythm), not an error.
     */
    fun detect(
        samples: FloatArray,
        sampleRate: Int,
        minGapMs: Long = DEFAULT_MIN_GAP_MS,
        sensitivity: Float = DEFAULT_SENSITIVITY
    ): List<Long> {
        if (samples.size < WINDOW_SAMPLES || sampleRate <= 0) return emptyList()
        val frameCount = (samples.size - WINDOW_SAMPLES) / HOP_SAMPLES + 1
        if (frameCount < 3) return emptyList()

        val energy = FloatArray(frameCount)
        for (frame in 0 until frameCount) {
            val start = frame * HOP_SAMPLES
            var sum = 0.0
            for (i in 0 until WINDOW_SAMPLES) {
                val sample = samples[start + i]
                sum += sample.toDouble() * sample
            }
            energy[frame] = (sum / WINDOW_SAMPLES).toFloat()
        }

        // Positive energy difference = onset strength. A falling level is not an
        // onset, so negatives are dropped instead of contributing to the mean.
        val onset = FloatArray(frameCount)
        for (frame in 1 until frameCount) {
            val diff = energy[frame] - energy[frame - 1]
            onset[frame] = if (diff > 0f) diff else 0f
        }

        val minGapFrames = (minGapMs.coerceAtLeast(0L) * sampleRate / 1_000.0 / HOP_SAMPLES)
            .toInt()
            .coerceAtLeast(1)
        val beats = ArrayList<Long>()
        // Long so the "no beat yet" sentinel cannot overflow when subtracted
        // from the first candidate frame (Int.MIN_VALUE would wrap and reject
        // every peak).
        var lastBeatFrame = Long.MIN_VALUE / 2
        for (frame in 1 until frameCount - 1) {
            val from = (frame - HISTORY_FRAMES).coerceAtLeast(0)
            var sum = 0f
            for (i in from until frame) sum += onset[i]
            val mean = sum / (frame - from).coerceAtLeast(1)
            val threshold = mean * sensitivity + 1e-6f
            val isPeak = onset[frame] >= threshold &&
                onset[frame] >= onset[frame - 1] &&
                onset[frame] >= onset[frame + 1]
            if (isPeak && frame - lastBeatFrame >= minGapFrames) {
                beats += frame.toLong() * HOP_SAMPLES * 1_000L / sampleRate
                lastBeatFrame = frame.toLong()
            }
        }
        return beats
    }

    /** Median-interval tempo estimate, or null when there is nothing to measure. */
    fun bpm(timesMs: List<Long>): Int? {
        if (timesMs.size < 3) return null
        val intervals = timesMs.zipWithNext { a, b -> b - a }
            .filter { it > 0L }
            .sorted()
        if (intervals.isEmpty()) return null
        val median = intervals[intervals.size / 2]
        if (median <= 0L) return null
        return (60_000.0 / median).roundToInt().coerceIn(30, 300)
    }

    /**
     * Even beat markers for a *known* tempo, starting at zero.
     *
     * This is the shared single source for "markers from a BPM": the local music
     * generator and templates both feed their tempo through here so a detected
     * grid and a declared one cannot drift apart.
     */
    fun grid(bpm: Int, durationMs: Long): List<Long> {
        if (bpm <= 0 || durationMs <= 0L) return emptyList()
        val step = (60_000.0 / bpm).toLong().coerceAtLeast(1L)
        val beats = ArrayList<Long>()
        var time = 0L
        while (time <= durationMs) {
            beats += time
            time += step
        }
        return beats
    }
}
