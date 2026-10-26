package com.zaaam.zcut.core.domain

import java.io.File
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

/**
 * Moods drawn from the approved AI Music mockup's chips.
 *
 * The original app streams a generated song from a task API
 * (`com.vega.audio.aimusic`, `GenerateAiMusicMv`); that needs a server and is
 * marked BUTUH BACKEND. What runs here is a real, local synthesiser: it writes a
 * playable WAV loop with no network and no license gate.
 */
enum class MusicMood {
    LOFI,
    CINEMATIC,
    POP,
    AMBIENT,
    TENSE
}

/**
 * A mood's musical parameters. These are the app's own choices (the artifact
 * never exposes its model or presets — the spec marks that Unknown), so they are
 * documented as inferred rather than presented as reverse-engineered values.
 */
data class MusicStyle(
    val mood: MusicMood,
    val label: String,
    val bpm: Int,
    val keyLabel: String,
    /** Root note in semitones relative to A4 (A4 = 0). */
    val rootSemitone: Int,
    /** Scale degrees in semitones from the root; natural minor, Dorian or major. */
    val scale: List<Int>,
    /** One bar per entry, as scale-degree indexes (0-based). */
    val progression: List<Int>,
    /** Vinyl/air noise level; zero would make the loop sound sterile. */
    val texture: Float
)

object MusicCatalog {

    private val MINOR = listOf(0, 2, 3, 5, 7, 8, 10)
    private val DORIAN = listOf(0, 2, 3, 5, 7, 9, 10)
    private val MAJOR = listOf(0, 2, 4, 5, 7, 9, 11)

    val styles: List<MusicStyle> = listOf(
        MusicStyle(MusicMood.LOFI, "Lo-fi", 84, "A minor", -12, MINOR, listOf(0, 5, 3, 4), 0.035f),
        MusicStyle(MusicMood.CINEMATIC, "Cinematic", 96, "D minor", -7, MINOR, listOf(0, 3, 4, 5), 0.010f),
        MusicStyle(MusicMood.POP, "Pop", 112, "C major", 3, MAJOR, listOf(0, 4, 5, 3), 0.006f),
        MusicStyle(MusicMood.AMBIENT, "Ambient", 68, "E Dorian", -5, DORIAN, listOf(0, 2, 5, 1), 0.020f),
        MusicStyle(MusicMood.TENSE, "Tense", 128, "F minor", -4, MINOR, listOf(0, 6, 5, 6), 0.012f)
    )

    val moods: List<MusicMood> = MusicMood.entries

    fun style(mood: MusicMood): MusicStyle =
        styles.first { it.mood == mood }
}

/**
 * A small on-device music synthesiser.
 *
 * Everything here is pure Kotlin arithmetic (no Android types), so the same
 * output can be asserted in a JVM test. The render is deterministic for a given
 * seed: the same mood, duration and seed produce byte-identical audio.
 *
 * Ponytail: this is additive synthesis, not a model — three sine voices per bar
 * (pad triad, plucked bass, kick/hat). It is enough for a usable loop bed, and
 * the honest ceiling is documented in the UI; a real generative model stays
 * BUTUH BACKEND.
 */
object MusicGenerator {

    const val SAMPLE_RATE = 22_050
    const val BEATS_PER_BAR = 4
    const val MIN_DURATION_MS = 8_000L
    const val MAX_DURATION_MS = 60_000L

    /** Beat markers for the generated tempo, used as the draft's snap grid. */
    fun beatGrid(bpm: Int, durationMs: Long): List<Long> =
        if (durationMs <= 0L) {
            emptyList()
        } else {
            BeatDetector.grid(bpm, durationMs.coerceAtMost(MAX_DURATION_MS))
        }

    /** Renders a mono 16-bit WAV of [durationMs] for [style]. */
    fun wav(style: MusicStyle, durationMs: Long, seed: Int): ByteArray =
        wrapWav(pcm(style, durationMs, seed), SAMPLE_RATE)

    /**
     * Result name shown in the UI and used on disk. Derived only from the mood,
     * plus the length and seed so two different renders never overwrite each
     * other's file; it can never carry a path separator or user text.
     */
    fun fileName(style: MusicStyle, durationMs: Long, seed: Int): String {
        val label = style.label.lowercase().replace(Regex("[^a-z0-9]+"), "")
        // Full milliseconds, not whole seconds: two renders of the same mood a
        // few hundred ms apart are different audio and must not share a file.
        val ms = durationMs.coerceIn(MIN_DURATION_MS, MAX_DURATION_MS)
        return "${label}_loop_${ms}ms_${seed.toUInt()}.wav"
    }

    /**
     * First free name for [base] inside [dir]. Two callers (AI Music, the agent)
     * pick their seeds independently, so a name collision must not silently make
     * one render overwrite the other's audio.
     */
    fun writeUnique(dir: File, base: String, bytes: ByteArray): File {
        dir.mkdirs()
        val stem = base.substringBeforeLast('.')
        val ext = base.substringAfterLast('.', "")
        var candidate = File(dir, base)
        var index = 2
        while (candidate.exists()) {
            candidate = File(dir, "$stem-$index.$ext")
            index++
        }
        candidate.writeBytes(bytes)
        return candidate
    }

    private fun pcm(style: MusicStyle, durationMs: Long, seed: Int): ShortArray {
        val total = durationMs.coerceIn(MIN_DURATION_MS, MAX_DURATION_MS)
        val count = ((total * SAMPLE_RATE) / 1_000L).toInt()
        val out = ShortArray(count)
        val random = Random(seed)
        val beat = 60.0 / style.bpm
        val bar = beat * BEATS_PER_BAR
        val scale = style.scale
        val progression = style.progression

        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE
            val barIndex = (t / bar).toInt()
            val inBar = t - barIndex * bar
            val degree = progression[barIndex % progression.size]

            // Pad: a sustained triad, one bar per chord, soft in and out.
            val padEnv = envelope(inBar, bar, 0.35)
            var pad = 0.0
            for (offset in intArrayOf(0, 2, 4)) {
                val index = degree + offset
                val semitone = style.rootSemitone + scale[index % scale.size] +
                    12 * (index / scale.size)
                pad += sin(TWO_PI * freq(semitone) * t)
            }
            pad = pad / 3.0 * padEnv * 0.30

            // Bass: the chord root, plucked once per beat.
            val inBeat = t - (t / beat).toInt() * beat
            val bass = sin(
                TWO_PI * freq(style.rootSemitone - 12 + scale[degree % scale.size]) * t
            ) * exp(-inBeat * 3.2) * 0.34

            // Percussion: kick on beats 1 and 3, a short hat on 2 and 4.
            val beatInBar = (t / beat).toInt() % BEATS_PER_BAR
            var perc = 0.0
            if (beatInBar == 0 || beatInBar == 2) {
                if (inBeat < 0.18) {
                    val sweep = 48.0 + 90.0 * exp(-inBeat * 26.0)
                    perc += sin(TWO_PI * sweep * t) * exp(-inBeat * 20.0) * 0.55
                }
            } else if (inBeat < 0.05) {
                perc += (random.nextDouble() * 2.0 - 1.0) * exp(-inBeat * 90.0) * 0.10
            }

            val air = (random.nextDouble() * 2.0 - 1.0) * style.texture
            val mixed = tanh((pad + bass + perc + air) * 0.9)
            out[i] = (mixed * Short.MAX_VALUE * 0.85)
                .toInt()
                .coerceIn(MIN_SAMPLE, MAX_SAMPLE)
                .toShort()
        }
        return out
    }

    private fun envelope(t: Double, length: Double, fade: Double): Double {
        if (length <= 0.0) return 0.0
        val f = fade.coerceAtMost(length / 2.0)
        if (f <= 0.0) return 1.0
        val attack = (t / f).coerceIn(0.0, 1.0)
        val release = ((length - t) / f).coerceIn(0.0, 1.0)
        return min(attack, release)
    }

    private fun freq(semitoneFromA4: Int): Double =
        440.0 * 2.0.pow(semitoneFromA4 / 12.0)

    private fun wrapWav(pcm: ShortArray, sampleRate: Int): ByteArray {
        val dataSize = pcm.size * 2
        val out = ByteArray(WAV_HEADER_BYTES + dataSize)
        var p = 0
        fun ascii(text: String) {
            text.forEach { out[p++] = it.code.toByte() }
        }

        fun le32(value: Int) {
            out[p++] = (value and 0xFF).toByte()
            out[p++] = (value ushr 8 and 0xFF).toByte()
            out[p++] = (value ushr 16 and 0xFF).toByte()
            out[p++] = (value ushr 24 and 0xFF).toByte()
        }

        fun le16(value: Int) {
            out[p++] = (value and 0xFF).toByte()
            out[p++] = (value ushr 8 and 0xFF).toByte()
        }

        ascii("RIFF")
        le32(36 + dataSize)
        ascii("WAVE")
        ascii("fmt ")
        le32(16)
        le16(1)                 // PCM
        le16(1)                 // mono
        le32(sampleRate)
        le32(sampleRate * 2)    // byte rate
        le16(2)                 // block align
        le16(16)                // bits per sample
        ascii("data")
        le32(dataSize)
        pcm.forEach { sample -> le16(sample.toInt()) }
        return out
    }

    private const val WAV_HEADER_BYTES = 44
    private const val MIN_SAMPLE = -32_768
    private const val MAX_SAMPLE = 32_767
    private const val TWO_PI = 2.0 * PI
}