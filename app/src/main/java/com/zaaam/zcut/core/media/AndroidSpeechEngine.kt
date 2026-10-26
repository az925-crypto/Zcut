package com.zaaam.zcut.core.media

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.zaaam.zcut.core.render.SpeechEngine
import com.zaaam.zcut.core.render.SpeechVoice
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * On-device speech synthesis through the platform [TextToSpeech] engine.
 *
 * The engine is created lazily on first use and kept for the app's lifetime
 * (its initialisation costs a service binding). Every call is serialised: the
 * TTS API state (voice, rate, pitch, listener) is global per engine.
 */
@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
class AndroidSpeechEngine(private val context: Context) : SpeechEngine {

    private val mutex = Mutex()

    @Volatile
    private var engine: TextToSpeech? = null

    @Volatile
    private var ready = false

    override suspend fun voices(): List<SpeechVoice> {
        val active = ensureReady() ?: return emptyList()
        val reported = runCatching { active.voices }.getOrNull()
            .orEmpty()
            // A network voice would send the user's script to a third-party
            // service, which contradicts the "local TTS" promise on screen.
            .filterNot { voice ->
                runCatching { voice.isNetworkConnectionRequired }.getOrDefault(false)
            }
            .map { voice ->
                SpeechVoice(
                    id = voice.name,
                    label = "${voice.name} · ${voice.locale.toLanguageTag()}",
                    locale = voice.locale.toLanguageTag()
                )
            }
            .distinctBy { it.id }
            .sortedBy { it.locale }
            .take(MAX_VOICES)
        // An engine with no voice list still has a usable default; showing one
        // honest "device default" entry beats an empty picker.
        return reported.ifEmpty {
            listOf(
                SpeechVoice(
                    id = "",
                    label = "Bawaan perangkat",
                    locale = Locale.getDefault().toLanguageTag()
                )
            )
        }
    }

    override suspend fun speak(
        text: String,
        voiceId: String?,
        rate: Float,
        pitch: Float
    ): Boolean {
        if (text.isBlank()) return false
        val active = ensureReady() ?: return false
        return mutex.withLock {
            withContext(Dispatchers.Main.immediate) {
                runCatching {
                    applyVoice(active, voiceId)
                    active.setSpeechRate(rate.coerceIn(MIN_RATE, MAX_RATE))
                    active.setPitch(pitch.coerceIn(MIN_PITCH, MAX_PITCH))
                    active.speak(
                        text,
                        TextToSpeech.QUEUE_FLUSH,
                        Bundle(),
                        UUID.randomUUID().toString()
                    ) == TextToSpeech.SUCCESS
                }.getOrDefault(false)
            }
        }
    }

    override suspend fun synthesize(
        text: String,
        voiceId: String?,
        rate: Float,
        pitch: Float,
        outputPath: String
    ): Boolean {
        if (text.isBlank()) return false
        val target = File(outputPath)
        target.parentFile?.mkdirs()
        val active = ensureReady() ?: return false
        return mutex.withLock {
            withContext(Dispatchers.Main.immediate) {
                runCatching {
                    applyVoice(active, voiceId)
                    active.setSpeechRate(rate.coerceIn(MIN_RATE, MAX_RATE))
                    active.setPitch(pitch.coerceIn(MIN_PITCH, MAX_PITCH))

                    val finished = CompletableDeferred<Boolean>()
                    active.setOnUtteranceProgressListener(
                        object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) = Unit

                            override fun onDone(utteranceId: String?) {
                                finished.complete(true)
                            }

                            override fun onError(utteranceId: String?) {
                                finished.complete(false)
                            }

                            override fun onError(utteranceId: String?, errorCode: Int) {
                                finished.complete(false)
                            }
                        }
                    )
                    val utteranceId = UUID.randomUUID().toString()
                    val queued = active.synthesizeToFile(text, Bundle(), target, utteranceId)
                    if (queued != TextToSpeech.SUCCESS) {
                        false
                    } else {
                        val completed = withTimeoutOrNull(SYNTH_TIMEOUT_MS) { finished.await() }
                        completed == true && target.isFile && target.length() > 0L
                    }
                }.getOrDefault(false)
            }
        }
    }

    override fun stop() {
        runCatching { engine?.stop() }
    }

    private suspend fun ensureReady(): TextToSpeech? {
        engine?.takeIf { ready }?.let { return it }
        return mutex.withLock {
            engine?.takeIf { ready }?.let { return@withLock it }
            withContext(Dispatchers.Main.immediate) {
                val init = CompletableDeferred<Boolean>()
                val created = runCatching {
                    TextToSpeech(context) { status ->
                        init.complete(status == TextToSpeech.SUCCESS)
                    }
                }.getOrNull()
                if (created == null) return@withContext null
                engine = created
                ready = withTimeoutOrNull(INIT_TIMEOUT_MS) { init.await() } == true
                created.takeIf { ready }
            }
        }
    }

    /** Applies a requested voice, falling back to the device language. */
    private fun applyVoice(active: TextToSpeech, voiceId: String?) {
        val requested = voiceId?.takeIf { it.isNotBlank() }?.let { id ->
            runCatching { active.voices }.getOrNull()?.firstOrNull { it.name == id }
        }
        if (requested != null) {
            runCatching { active.voice = requested }
        } else {
            runCatching { active.setLanguage(Locale.getDefault()) }
        }
    }

    private companion object {
        const val INIT_TIMEOUT_MS = 8_000L
        const val SYNTH_TIMEOUT_MS = 60_000L
        const val MIN_RATE = 0.25f
        const val MAX_RATE = 3f
        const val MIN_PITCH = 0.5f
        const val MAX_PITCH = 2f
        const val MAX_VOICES = 40
    }
}
