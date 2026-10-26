package com.zaaam.zcut.core.util

import com.zaaam.zcut.core.model.CaptionCue
import com.zaaam.zcut.core.model.Transcript
import com.zaaam.zcut.core.model.TranscriptSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SrtFormatTest {

    @Test
    fun timestampPadsEveryField() {
        assertEquals("00:00:00,000", SrtFormat.timestamp(0L))
        assertEquals("00:00:02,400", SrtFormat.timestamp(2_400L))
        assertEquals("01:02:03,456", SrtFormat.timestamp(3_723_456L))
    }

    @Test
    fun timestampClampsNegativeInput() {
        assertEquals("00:00:00,000", SrtFormat.timestamp(-500L))
    }

    @Test
    fun writeProducesNumberedBlocksInStartOrder() {
        val transcript = Transcript(
            cues = listOf(
                CaptionCue(startMs = 1_000L, endMs = 2_000L, text = "bye"),
                CaptionCue(startMs = 0L, endMs = 1_000L, text = "hello world")
            )
        )

        assertEquals(
            "1\n" +
                "00:00:00,000 --> 00:00:01,000\n" +
                "hello world\n" +
                "\n" +
                "2\n" +
                "00:00:01,000 --> 00:00:02,000\n" +
                "bye\n",
            SrtFormat.write(transcript)
        )
    }

    @Test
    fun writeReturnsNothingForAnEmptyTranscript() {
        assertEquals("", SrtFormat.write(Transcript()))
    }

    @Test
    fun writeThenParseRoundTripsTextAndTimes() {
        val original = Transcript(
            cues = listOf(
                CaptionCue(startMs = 0L, endMs = 900L, text = "hello world"),
                CaptionCue(startMs = 1_000L, endMs = 1_800L, text = "baris dua")
            )
        )

        val parsed = SrtFormat.parse(SrtFormat.write(original))

        assertEquals(listOf("hello world", "baris dua"), parsed.cues.map { it.text })
        assertEquals(0L, parsed.cues.first().startMs)
        assertEquals(1_800L, parsed.cues.last().endMs)
        assertEquals(TranscriptSource.IMPORTED_SRT, parsed.source)
    }

    @Test
    fun parseAcceptsDotMillisCrlfAndAMissingIndex() {
        val parsed = SrtFormat.parse(
            "00:00:01.500 --> 00:00:02.000\r\nHalo\r\n"
        )

        val cue = parsed.cues.single()
        assertEquals("Halo", cue.text)
        assertEquals(1_500L, cue.startMs)
        assertEquals(2_000L, cue.endMs)
    }

    @Test
    fun parseSkipsBlocksWithoutAValidTimeLine() {
        val parsed = SrtFormat.parse(
            "1\n\nnot a subtitle\n\n" +
                "2\n00:00:00,000 --> 00:00:01,000\nok\n"
        )

        assertEquals(listOf("ok"), parsed.cues.map { it.text })
    }

    @Test
    fun parseOfGarbageReturnsAnEmptyTranscript() {
        val parsed = SrtFormat.parse("just some prose with no timing")

        assertTrue(parsed.cues.isEmpty())
    }
}