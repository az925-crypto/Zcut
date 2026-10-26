package com.zaaam.zcut.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptionSegmenterTest {

    @Test
    fun splitIntoLinesWrapsWithinTheCharacterBudget() {
        val lines = CaptionSegmenter.splitIntoLines(
            "the quick brown fox jumps",
            maxChars = 9
        )

        assertEquals(listOf("the quick", "brown fox", "jumps"), lines)
    }

    @Test
    fun splitIntoLinesNeverCutsASingleLongWord() {
        val lines = CaptionSegmenter.splitIntoLines(
            "supercalifragilistic hi",
            maxChars = 5
        )

        assertEquals(listOf("supercalifragilistic", "hi"), lines)
    }

    @Test
    fun splitIntoLinesReturnsNothingForBlankText() {
        assertTrue(CaptionSegmenter.splitIntoLines("   ").isEmpty())
    }

    @Test
    fun segmentCoversTheWholeWindowInOrder() {
        val lines = CaptionSegmenter.segment("aa bb cc dd", 0L, 1_000L, maxChars = 5)

        assertEquals(listOf("aa bb", "cc dd"), lines.map { it.text })
        assertEquals(0L, lines.first().startMs)
        assertEquals(1_000L, lines.last().endMs)
        // Contiguous: each line starts where the previous one ended.
        assertEquals(lines[0].endMs, lines[1].startMs)
    }

    @Test
    fun segmentWeightsTimingByLineLength() {
        // 4 chars vs 8 chars: the short line gets a third of the window.
        val lines = CaptionSegmenter.segment("aa bb", 0L, 900L, maxChars = 2)

        assertEquals(listOf("aa", "bb"), lines.map { it.text })
        assertEquals(450L, lines.first().endMs - lines.first().startMs)
        assertEquals(450L, lines.last().endMs - lines.last().startMs)
    }

    @Test
    fun fromWordsFlushesOnTheCharacterBudget() {
        val words = listOf(
            CaptionSegmenter.Word("hello", 0L, 400L),
            CaptionSegmenter.Word("world", 400L, 900L),
            CaptionSegmenter.Word("again", 900L, 1_300L)
        )

        val lines = CaptionSegmenter.fromWords(words, maxChars = 11)

        assertEquals(2, lines.size)
        assertEquals("hello world", lines[0].text)
        assertEquals(0L, lines[0].startMs)
        assertEquals(900L, lines[0].endMs)
        assertEquals("again", lines[1].text)
    }

    @Test
    fun fromWordsFlushesWhenALineStaysTooLong() {
        val words = listOf(
            CaptionSegmenter.Word("a", 0L, 600L),
            CaptionSegmenter.Word("b", 600L, 1_200L),
            CaptionSegmenter.Word("c", 1_200L, 1_300L)
        )

        val lines = CaptionSegmenter.fromWords(words, maxChars = 100, maxDurationMs = 1_000L)

        assertEquals(listOf("a", "b c"), lines.map { it.text })
        assertEquals(1_300L, lines.last().endMs)
    }
}