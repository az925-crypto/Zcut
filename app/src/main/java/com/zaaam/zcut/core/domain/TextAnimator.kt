package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.TextAnimation
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.sin

/**
 * One sampled animation state. Fractions are resolution-independent so the
 * editor preview and the export renderer can share the same math:
 * - [offsetYFraction] is a fraction of the frame height;
 * - [glitchXFraction]/[glitchYFraction] are fractions of the font size;
 * - [visibleChars] caps the rendered string for TYPEWRITER.
 */
data class TextAnimationFrame(
    val alpha: Float = 1f,
    val offsetYFraction: Float = 0f,
    val scale: Float = 1f,
    val visibleChars: Int = Int.MAX_VALUE,
    val glitchXFraction: Float = 0f,
    val glitchYFraction: Float = 0f
) {
    companion object {
        val Idle = TextAnimationFrame()
    }
}

/** Pure, deterministic text intro animations. */
object TextAnimator {

    private const val RISE_MS = 400L
    private const val TYPEWRITER_MS = 600L
    private const val POP_MS = 450L
    private const val GLITCH_MS = 500L

    fun frame(
        animation: TextAnimation,
        elapsedMs: Long,
        durationMs: Long,
        textLength: Int,
        seed: Int = 0
    ): TextAnimationFrame {
        if (textLength <= 0) return TextAnimationFrame.Idle
        val elapsed = elapsedMs.coerceAtLeast(0L)
        val duration = durationMs.coerceAtLeast(0L)
        return when (animation) {
            TextAnimation.NONE -> TextAnimationFrame.Idle
            TextAnimation.RISE -> rise(elapsed)
            TextAnimation.TYPEWRITER -> typewriter(elapsed, textLength)
            TextAnimation.POP -> pop(elapsed)
            TextAnimation.GLITCH -> glitch(elapsed, duration, seed)
        }
    }
    private fun rise(elapsedMs: Long): TextAnimationFrame {
        val progress = smooth((elapsedMs.toFloat() / RISE_MS).coerceIn(0f, 1f))
        return TextAnimationFrame(
            alpha = progress,
            offsetYFraction = (1f - progress) * 0.06f
        )
    }

    private fun typewriter(elapsedMs: Long, textLength: Int): TextAnimationFrame {
        val progress = (elapsedMs.toFloat() / TYPEWRITER_MS).coerceIn(0f, 1f)
        val chars = ceil(textLength * progress).toInt().coerceIn(0, textLength)
        return TextAnimationFrame(visibleChars = chars)
    }

    private fun pop(elapsedMs: Long): TextAnimationFrame {
        val progress = (elapsedMs.toFloat() / POP_MS).coerceIn(0f, 1f)
        // Overshoots slightly past 1 before settling, like the mockup's POP.
        val scale = 0.72f + 0.28f * progress + 0.18f * sin(PI.toFloat() * progress)
        return TextAnimationFrame(alpha = progress, scale = scale)
    }

    private fun glitch(elapsedMs: Long, durationMs: Long, seed: Int): TextAnimationFrame {
        val window = minOf(GLITCH_MS, durationMs.coerceAtLeast(1L))
        if (elapsedMs >= window) return TextAnimationFrame.Idle
        val progress = (elapsedMs.toFloat() / window).coerceIn(0f, 1f)
        val (x, y) = glitchOffset(seed, elapsedMs)
        val decay = 1f - progress
        val flicker = 0.65f + 0.35f * ((sin(elapsedMs / 40.0).toFloat() + 1f) / 2f)
        return TextAnimationFrame(
            alpha = flicker.coerceIn(0f, 1f),
            glitchXFraction = x * decay,
            glitchYFraction = y * decay
        )
    }

    /** Deterministic per-60ms jitter in `[-1, 1]`; same seed/time = same offset. */
    fun glitchOffset(seed: Int, elapsedMs: Long): Pair<Float, Float> {
        val step = (elapsedMs.coerceAtLeast(0L) / 60L).toInt()
        return pseudo(seed, step, 17) to pseudo(seed, step, 31)
    }

    private fun pseudo(seed: Int, step: Int, salt: Int): Float {
        var hash = seed * 31 + step * 17 + salt
        hash = hash xor (hash shl 13)
        hash = hash xor (hash ushr 17)
        hash = hash xor (hash shl 5)
        val normalized = (hash and 0x7fffffff).toFloat() / Int.MAX_VALUE.toFloat()
        return normalized * 2f - 1f
    }

    private fun smooth(t: Float): Float = t * t * (3f - 2f * t)
}
