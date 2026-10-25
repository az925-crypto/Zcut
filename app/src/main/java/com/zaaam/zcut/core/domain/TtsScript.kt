package com.zaaam.zcut.core.domain

import java.security.MessageDigest
import java.util.Locale
import kotlin.math.roundToLong

/**
 * Script rules and cache identity for the system TextToSpeech engine.
 *
 * The cache key covers every input that changes the audio bytes, so the same
 * line with the same voice/rate/pitch is synthesised once and reused offline.
 */
object TtsScript {

    const val MAX_LENGTH = 1_000

    /** Rough reading speed used only for the "≈ N s" hint, never for rendering. */
    const val CHARS_PER_SECOND = 15f

    const val MIN_ESTIMATE_MS = 500L
    const val MAX_ESTIMATE_MS = 120_000L

    fun sanitize(text: String): String = text.trim().take(MAX_LENGTH)

    fun estimateDurationMs(text: String): Long {
        val length = text.trim().length
        if (length == 0) return 0L
        val estimate = (length / CHARS_PER_SECOND * 1_000f).roundToLong()
        return estimate.coerceIn(MIN_ESTIMATE_MS, MAX_ESTIMATE_MS)
    }

    /**
     * Stable SHA-256 identity for a synthesis request. Rate and pitch are
     * formatted with [Locale.US] so a device locale cannot change the key.
     */
    fun cacheKey(
        text: String,
        voiceId: String?,
        rate: Float,
        pitch: Float,
        engine: String = "system"
    ): String {
        val payload = buildString {
            append(engine)
            append('|')
            append(voiceId.orEmpty())
            append('|')
            append(String.format(Locale.US, "%.3f", rate))
            append('|')
            append(String.format(Locale.US, "%.3f", pitch))
            append('|')
            append(text)
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray())
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    /** Cache file name inside the app's TTS cache directory. */
    fun fileName(
        text: String,
        voiceId: String?,
        rate: Float,
        pitch: Float
    ): String = "tts-${cacheKey(text, voiceId, rate, pitch)}.wav"
}
