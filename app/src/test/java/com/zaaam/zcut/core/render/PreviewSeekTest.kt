package com.zaaam.zcut.core.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewSeekTest {

    @Test
    fun clampKeepsPositionInsideTimeline() {
        assertEquals(0L, PreviewSeek.clamp(-10L, 5_000L))
        assertEquals(2_000L, PreviewSeek.clamp(2_000L, 5_000L))
        assertEquals(5_000L, PreviewSeek.clamp(9_000L, 5_000L))
        assertEquals(0L, PreviewSeek.clamp(1_000L, -1L))
    }

    @Test
    fun rangeRequiresBothEndsAndPositiveLength() {
        assertNull(PreviewSeek.range(null, 1_000L, 5_000L))
        assertNull(PreviewSeek.range(1_000L, null, 5_000L))
        assertNull(PreviewSeek.range(1_000L, 1_000L, 5_000L))
        assertNull(PreviewSeek.range(2_000L, 1_000L, 5_000L))
        assertEquals(1_000L..2_000L, PreviewSeek.range(1_000L, 2_000L, 5_000L))
    }

    @Test
    fun rangeIsClampedToTimeline() {
        assertEquals(4_000L..5_000L, PreviewSeek.range(4_000L, 9_000L, 5_000L))
        assertEquals(0L..2_000L, PreviewSeek.range(-3_000L, 2_000L, 5_000L))
    }

    @Test
    fun playFromRestartsAtZeroWhenTimelineFinished() {
        assertEquals(0L, PreviewSeek.playFrom(null, 5_000L, 5_000L))
        assertEquals(0L, PreviewSeek.playFrom(null, 9_000L, 5_000L))
        assertEquals(1_200L, PreviewSeek.playFrom(null, 1_200L, 5_000L))
    }

    @Test
    fun playFromRestartsRangeOutsideItsBounds() {
        val range = 2_000L..4_000L

        assertEquals(2_000L, PreviewSeek.playFrom(range, 0L, 5_000L))
        assertEquals(2_000L, PreviewSeek.playFrom(range, 4_000L, 5_000L))
        assertEquals(3_000L, PreviewSeek.playFrom(range, 3_000L, 5_000L))
    }

    @Test
    fun reachedEndUsesRangeOrTimelineDuration() {
        assertFalse(PreviewSeek.reachedEnd(null, 4_999L, 5_000L))
        assertTrue(PreviewSeek.reachedEnd(null, 5_000L, 5_000L))
        assertFalse(PreviewSeek.reachedEnd(2_000L..4_000L, 3_999L, 5_000L))
        assertTrue(PreviewSeek.reachedEnd(2_000L..4_000L, 4_000L, 5_000L))
    }

    @Test
    fun manualSeekOutsideRangeClearsIt() {
        val range = 2_000L..4_000L

        assertEquals(range, PreviewSeek.rangeAfterSeek(range, 3_000L))
        assertNull(PreviewSeek.rangeAfterSeek(range, 1_999L))
        assertNull(PreviewSeek.rangeAfterSeek(range, 4_000L))
        assertNull(PreviewSeek.rangeAfterSeek(null, 3_000L))
    }
}
