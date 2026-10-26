package com.zaaam.zcut.core.media

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
 * Nothing is uploaded by this class: it asks the platform recogniser for local
 * recognition (`EXTRA_PREFER_OFFLINE`) and never touches the network itself —
 * this app has no `INTERNET` permission. Whether the device's recogniser service
 * honours that request is the service's business, so if none is installed
 * [isAvailable] is false and the UI says so instead of pretending to transcribe.
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

    private val _phrases = MutableSharedFlow<Phrase>(extraBufferCapacity = PHRASE_BUFFER)
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

    /** Restarts are posted here so a persistent error cannot spin the CPU. */
    private val mainHandler = Handler(Looper.getMainLooper())
    private var errorStreak = 0
    private var restartPending = false

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
        restartPending = false
        errorStreak = 0
        mainHandler.removeCallbacksAndMessages(null)
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
            // Ask for on-device recognition; the service may ignore it, which is
            // why the screen never promises that nothing leaves the device.
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        runCatching { active.startListening(intent) }.onFailure {
            _notice.value = "Pengenalan suara gagal dimulai."
            stop()
        }
    }

    /** Restarts the engine, which stops after each utterance by design. */
    private fun restartIfListening() {
        if (!_listening.value) return
        // An immediate restart makes a queued backoff restart redundant; without
        // this the engine could get two startListening calls in the same window.
        mainHandler.removeCallbacksAndMessages(null)
        restartPending = false
        listen()
    }

    /**
     * Restart after a growing pause. A missing microphone or a broken recogniser
     * service answers every request with an error, and restarting at once turns
     * that into a tight main-thread loop, so repeated errors back off instead.
     */
    private fun restartAfterError() {
        if (!_listening.value || restartPending) return
        restartPending = true
        val delayMs = BACKOFF_MS[errorStreak.coerceAtMost(BACKOFF_MS.lastIndex)]
        errorStreak++
        mainHandler.postDelayed({
            restartPending = false
            restartIfListening()
        }, delayMs)
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
            if (error == SpeechRecognizer.ERROR_NO_MATCH ||
                error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
            ) {
                restartIfListening()
            } else {
                restartAfterError()
            }
        }

        override fun onResults(results: Bundle?) {
            // A completed cycle means the engine is healthy again; a service that
            // is always "ready" and then errors never reaches here, so its backoff
            // keeps growing instead of being pinned at the first step.
            errorStreak = 0
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            if (text.isNotEmpty()) {
                val end = elapsed()
                val start = segmentStartMs.coerceIn(lastPhraseEndMs, end)
                lastPhraseEndMs = end
                // A dropped phrase would be a silent hole in the transcript, so
                // say so instead of losing it quietly; a later success clears it.
                if (_phrases.tryEmit(Phrase(text, start, end))) {
                    if (_notice.value == DROP_NOTICE) _notice.value = null
                } else {
                    _notice.value = DROP_NOTICE
                }
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

    private companion object {
        /** Pause before retrying after consecutive errors; the last value sticks. */
        val BACKOFF_MS = longArrayOf(250L, 500L, 1_000L, 2_000L, 4_000L)

        /**
         * Room for recognised phrases the collector has not picked up yet. Every
         * drop is reported, so the buffer only needs to absorb a short burst.
         */
        const val PHRASE_BUFFER = 256

        /** Shown while the buffer is dropping phrases; cleared on the next emit. */
        const val DROP_NOTICE = "Beberapa frasa tidak tercatat (buffer penuh)."
    }
}