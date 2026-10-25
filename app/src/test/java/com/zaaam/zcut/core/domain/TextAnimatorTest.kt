package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.TextAnimation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class TextAnimatorTest {

    @Test
    fun noneIsAlwaysIdle() {
        assertEquals(
            TextAnimationFrame.Idle,
            TextAnimator.frame(TextAnimation.NONE, 123L, 1_000L, 10)
        )
    }

    @Test
    fun riseFadesInAndLiftsToRest() {
        val start = TextAnimator.frame(TextAnimation.RISE, 0L, 1_000L, 5)
        val end = TextAnimator.frame(TextAnimation.RISE, 400L, 1_000L, 5)

        assertEquals(0f, start.alpha, 0.001f)
        assertTrue(start.offsetYFraction > 0f)
        assertEquals(1f, end.alpha, 0.001f)
        assertEquals(0f, end.offsetYFraction, 0.001f)
    }

    @Test
    fun typewriterRevealsMonotonicallyAndFinishes() {
        val start = TextAnimator.frame(TextAnimation.TYPEWRITER, 0L, 1_000L, 10)
        val middle = TextAnimator.frame(TextAnimation.TYPEWRITER, 300L, 1_000L, 10)
        val end = TextAnimator.frame(TextAnimation.TYPEWRITER, 600L, 1_000L, 10)

        assertEquals(0, start.visibleChars)
        assertTrue(middle.visibleChars in 1 until 10)
        assertEquals(10, end.visibleChars)
    }

    @Test
    fun popOvershootsThenSettlesAtOne() {
        val middle = TextAnimator.frame(TextAnimation.POP, 225L, 1_000L, 5)
        val end = TextAnimator.frame(TextAnimation.POP, 450L, 1_000L, 5)

        assertTrue(middle.scale > 1f)
        assertEquals(1f, end.scale, 0.001f)
        assertEquals(1f, end.alpha, 0.001f)
    }

    @Test
    fun glitchIsDeterministicAndDecaysToIdle() {
        val first = TextAnimator.frame(TextAnimation.GLITCH, 60L, 1_000L, 5, seed = 7)
        val same = TextAnimator.frame(TextAnimation.GLITCH, 60L, 1_000L, 5, seed = 7)
        val late = TextAnimator.frame(TextAnimation.GLITCH, 400L, 1_000L, 5, seed = 7)
        val done = TextAnimator.frame(TextAnimation.GLITCH, 500L, 1_000L, 5, seed = 7)

        assertEquals(first, same)
        assertTrue(abs(late.glitchXFraction) <= 0.2f)
        assertTrue(abs(late.glitchYFraction) <= 0.2f)
        assertEquals(TextAnimationFrame.Idle, done)
    }

    @Test
    fun negativeElapsedNeverCrashes() {
        TextAnimation.entries.forEach { animation ->
            val frame = TextAnimator.frame(animation, -500L, 1_000L, 8)
            assertTrue(frame.alpha in 0f..1f)
        }
    }

    @Test
    fun emptyTextIsIdle() {
        assertEquals(
            TextAnimationFrame.Idle,
            TextAnimator.frame(TextAnimation.RISE, 100L, 1_000L, 0)
        )
    }
}
