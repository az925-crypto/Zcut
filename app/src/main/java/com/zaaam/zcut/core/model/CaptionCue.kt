package com.zaaam.zcut.core.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class CaptionCue(
    val id: String = UUID.randomUUID().toString(),
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val language: String = "und",
    val style: TextConfig? = null
) {
    val durationMs: Long
        get() = (endMs - startMs).coerceAtLeast(0L)
}

@Serializable
data class Transcript(
    val language: String = "und",
    val cues: List<CaptionCue> = emptyList(),
    val source: TranscriptSource = TranscriptSource.MANUAL
)

@Serializable
enum class TranscriptSource {
    MANUAL,
    ON_DEVICE_SPEECH_RECOGNIZER,
    IMPORTED_SRT,
    /** Cues estimated from a locally synthesised voice track (F25). */
    SYNTHESIZED,
    BACKEND
}
