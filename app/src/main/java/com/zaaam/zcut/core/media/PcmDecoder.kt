package com.zaaam.zcut.core.media

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Decodes a local audio file to a mono float waveform with the platform
 * decoder. Used by beat detection and loudness normalisation so no bundled
 * native DSP or network call is involved.
 *
 * The result is capped by [MAX_DURATION_MS] to keep memory bounded.
 */
object PcmDecoder {

    const val MAX_DURATION_MS = 60_000L

    data class Mono(val samples: FloatArray, val sampleRate: Int)

    suspend fun decodeMono(
        path: String,
        maxDurationMs: Long = MAX_DURATION_MS
    ): Mono? = withContext(Dispatchers.IO) {
        if (!File(path).isFile) return@withContext null
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(path)
            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                val mime = runCatching {
                    extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)
                }.getOrNull()
                mime?.startsWith("audio/") == true
            } ?: return@withContext null
            extractor.selectTrack(trackIndex)
            val inputFormat = extractor.getTrackFormat(trackIndex)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: return@withContext null

            val decoder = MediaCodec.createDecoderByType(mime)
            codec = decoder
            decoder.configure(inputFormat, null, null, 0)
            decoder.start()

            val pcm = ByteArrayOutputStream()
            val bufferInfo = MediaCodec.BufferInfo()
            var sampleRate = inputFormat.intOr(MediaFormat.KEY_SAMPLE_RATE, 44_100)
            var channels = inputFormat.intOr(MediaFormat.KEY_CHANNEL_COUNT, 1)
            var bytesPerSample = 2
            var inputDone = false
            var outputDone = false
            val deadlineNs = System.nanoTime() + DECODE_TIMEOUT_MS * 1_000_000L

            while (!outputDone && System.nanoTime() < deadlineNs) {
                if (!inputDone) {
                    val inputIndex = decoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inputIndex >= 0) {
                        val inputBuffer = decoder.getInputBuffer(inputIndex)
                        val size = if (inputBuffer == null) -1 else extractor.readSampleData(inputBuffer, 0)
                        if (size < 0) {
                            decoder.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                        } else {
                            decoder.queueInputBuffer(inputIndex, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                when (val outputIndex = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outputFormat = decoder.outputFormat
                        sampleRate = outputFormat.intOr(MediaFormat.KEY_SAMPLE_RATE, sampleRate)
                        channels = outputFormat.intOr(MediaFormat.KEY_CHANNEL_COUNT, channels)
                            .coerceAtLeast(1)
                        val encoding = outputFormat.intOr(
                            MediaFormat.KEY_PCM_ENCODING,
                            AudioFormat.ENCODING_PCM_16BIT
                        )
                        bytesPerSample = if (encoding == AudioFormat.ENCODING_PCM_FLOAT) 4 else 2
                    }

                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit

                    else -> if (outputIndex >= 0) {
                        val outputBuffer = decoder.getOutputBuffer(outputIndex)
                        if (outputBuffer != null && bufferInfo.size > 0) {
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            val chunk = ByteArray(bufferInfo.size)
                            outputBuffer.get(chunk)
                            pcm.write(chunk)
                        }
                        decoder.releaseOutputBuffer(outputIndex, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputDone = true
                        }
                    }
                }

                val maxBytes = (maxDurationMs * sampleRate / 1_000L) * bytesPerSample * channels
                if (maxBytes > 0L && pcm.size().toLong() >= maxBytes) outputDone = true
            }

            if (sampleRate <= 0) return@withContext null
            Mono(toMono(pcm.toByteArray(), bytesPerSample, channels), sampleRate)
        } catch (error: Exception) {
            null
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }

    private fun MediaFormat.intOr(key: String, fallback: Int): Int =
        if (containsKey(key)) runCatching { getInteger(key) }.getOrDefault(fallback) else fallback

    /** Averages channels so the detector sees one waveform regardless of layout. */
    private fun toMono(raw: ByteArray, bytesPerSample: Int, channels: Int): FloatArray {
        val frameBytes = bytesPerSample * channels
        if (frameBytes <= 0) return FloatArray(0)
        val frames = raw.size / frameBytes
        val samples = FloatArray(frames)
        val buffer = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)
        for (frame in 0 until frames) {
            var sum = 0f
            for (channel in 0 until channels) {
                val offset = frame * frameBytes + channel * bytesPerSample
                sum += if (bytesPerSample == 4) {
                    buffer.getFloat(offset)
                } else {
                    buffer.getShort(offset) / 32_768f
                }
            }
            samples[frame] = sum / channels
        }
        return samples
    }

    private const val TIMEOUT_US = 10_000L
    private const val DECODE_TIMEOUT_MS = 20_000L
}
