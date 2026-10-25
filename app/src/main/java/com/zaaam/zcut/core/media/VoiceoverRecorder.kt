package com.zaaam.zcut.core.media

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.util.UUID

/**
 * Records a voiceover from the device microphone into the app's private media
 * folder. Nothing is uploaded: the file becomes a normal local asset.
 *
 * A recorder owns the microphone, so it is created per recording session rather
 * than kept as a singleton.
 */
class VoiceoverRecorder(private val context: Context) {

    data class Recording(val file: File, val durationMs: Long)

    private var recorder: MediaRecorder? = null
    private var target: File? = null
    private var startedAtMs = 0L

    val isRecording: Boolean get() = recorder != null

    /** Starts recording and returns the output file, or null when it cannot start. */
    @Suppress("DEPRECATION")
    fun start(): File? {
        if (recorder != null) return null
        val dir = File(context.filesDir, MEDIA_DIR)
        dir.mkdirs()
        val file = File(dir, "voiceover-${UUID.randomUUID()}.m4a")
        val created = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }
        } catch (error: Exception) {
            file.delete()
            return null
        }
        return try {
            created.setAudioSource(MediaRecorder.AudioSource.MIC)
            created.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            created.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            created.setAudioEncodingBitRate(128_000)
            created.setAudioSamplingRate(44_100)
            created.setOutputFile(file.absolutePath)
            created.prepare()
            created.start()
            recorder = created
            target = file
            startedAtMs = System.currentTimeMillis()
            file
        } catch (error: Exception) {
            // Release the recorder we just built: it holds the mic even when
            // prepare()/start() failed, and `recorder` is only assigned on success.
            runCatching { created.release() }
            recorder = null
            target = null
            file.delete()
            null
        }
    }

    /** Stops and returns the finished recording, or null when nothing usable was captured. */
    fun stop(): Recording? {
        val active = recorder ?: return null
        val file = target
        recorder = null
        target = null
        val elapsed = (System.currentTimeMillis() - startedAtMs).coerceAtLeast(0L)
        return try {
            active.stop()
            active.release()
            if (file == null || !file.isFile || file.length() == 0L) {
                file?.delete()
                null
            } else {
                Recording(file, AudioDuration.of(file.absolutePath) ?: elapsed)
            }
        } catch (error: Exception) {
            // stop() throws when the recording was too short to encode anything.
            runCatching { active.release() }
            file?.delete()
            null
        }
    }

    /** Aborts the session and removes the partial file. */
    fun cancel() {
        val active = recorder ?: return
        recorder = null
        runCatching { active.stop() }
        runCatching { active.release() }
        target?.delete()
        target = null
    }

    private companion object {
        const val MEDIA_DIR = "media"
    }
}
