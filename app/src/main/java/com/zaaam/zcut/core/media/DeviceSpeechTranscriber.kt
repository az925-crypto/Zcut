package com.zaaam.zcut.core.media

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Local speech-to-text through the platform [SpeechRecognizer].
 *
 * The recogniser listens to the **microphone**, not to a file — that is the only
 * on-device ASR the platform exposes without bundling a model. The screen tells
 * the user to play the clip through the speaker, and each recognised phrase is
 * stamped with the *measured* elapsed time since listening started, so the cue
 * windows come from a stopwatch, never from a guess. When the app can time a
 * phrase to the playhead it maps it back by subtracting the playhead at start.
 *
 * Nothing is uploaded by this class: `SpeechRecognizer` may use the device's own
 * service, and if none is installed [isAvailable] is false and the UI says so
 * instead of pretending to transcribe.
 *
 * All methods must be called from the main thread: the platform recogniser needs
 * a `Looper` and its callbacks arrive there.
 */
class DeviceSpeechTranscriber(private val context: Context) {

    /** One recognised phrase with a measured window, relative to [start]. */
    data class Phrase(
        val text: String,
        val startMs: Long,
        val endMs: Long
    )

    private val _phrases = MutableSharedFlow<Phrase>(extraBufferCapacity = 64)
    val phrases: SharedFlow<Phrase> = _phrases

    private val _listening = MutableStateFlow(false)
    val listening: StateFlow<Boolean> = _listening

    private val _partial = MutableStateFlow("")
    val partial: StateFlow<String> = _partial

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice

    private var recognizer: SpeechRecognizer? = null
    private var originElapsedMs = 0L
    private var segmentStartMs = 0L
    private var lastPhraseEndMs = 0L

    val isAvailable: Boolean get() = SpeechRecognizer.isRecognitionAvailable(context)

    /** Starts (or keeps) listening; returns false when no recogniser exists. */
    fun start(): Boolean {
        if (_listening.value) return true
        if (!isAvailable) {
            _notice.value = "Perangkat tidak punya layanan pengenalan suara lokal."
            return false
        }
        val created = runCatching { SpeechRecognizer.createSpeechRecognizer(context) }.getOrNull()
        if (created == null) {
            _notice.value = "Pengenalan suara lokal gagal dimuat."
            return false
        }
        created.setRecognitionListener(listener)
        recognizer = created
        originElapsedMs = SystemClock.elapsedRealtime()
        segmentStartMs = 0L
        lastPhraseEndMs = 0L
        _partial.value = ""
        _notice.value = null
        _listening.value = true
        listen()
        return true
    }

    fun stop() {
        if (!_listening.value && recognizer == null) return
        _listening.value = false
        _partial.value = ""
        runCatching { recognizer?.cancel() }
        runCatching { recognizer?.destroy() }
        recognizer = null
    }

    private fun listen() {
        val active = recognizer ?: return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
        }
        runCatching { active.startListening(intent) }.onFailure {
            _notice.value = "Pengenalan suara gagal dimulai."
            stop()
        }
    }

    /** Restarts the engine, which stops after each utterance by design. */
    private fun restartIfListening() {
        if (_listening.value) listen()
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit

        override fun onBeginningOfSpeech() {
            segmentStartMs = elapsed()
        }

        override fun onRmsChanged(rmsdB: Float) = Unit

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() = Unit

        override fun onError(error: Int) {
            // ERROR_NO_MATCH/ERROR_SPEECH_TIMEOUT just mean "nothing said yet";
            // keep listening so a pause does not end the session.
            if (error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                restartIfListening()
            } else if (_listening.value) {
                restartIfListening()
            }
        }

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            if (text.isNotEmpty()) {
                val end = elapsed()
                val start = segmentStartMs.coerceIn(lastPhraseEndMs, end)
                lastPhraseEndMs = end
                _phrases.tryEmit(Phrase(text, start, end))
            }
            _partial.value = ""
            restartIfListening()
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            _partial.value = text
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun elapsed(): Long =
        (SystemClock.elapsedRealtime() - originElapsedMs).coerceAtLeast(0L)
}