package com.zaaam.zcut.core.render

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.Segmenter
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Subject segmentation with ML Kit's bundled selfie model. Everything runs on
 * the device: the model ships in the APK (≈4.5 MB) and no image leaves it.
 *
 * The client is deliberately long-lived (one per app process) because building
 * it loads the native model; the mask itself is returned in memory so the
 * caller can composite without a file round-trip.
 */
class MlKitSubjectSegmenter : SubjectSegmenter {

    @Volatile
    private var client: Segmenter? = null

    /** Model load failed (unsupported ABI, missing native lib); say so, do not guess. */
    @Volatile
    var unavailableReason: String? = null
        private set

    override suspend fun segment(frame: Bitmap): SubjectMask? = withContext(Dispatchers.Default) {
        if (frame.width < MIN_SIDE || frame.height < MIN_SIDE) {
            // ML Kit asks for at least 256px; a smaller frame just produces noise.
            return@withContext null
        }
        val segmenter = clientOrNull() ?: return@withContext null
        val mask = try {
            segmenter.process(InputImage.fromBitmap(frame, 0)).awaitMask()
        } catch (error: Exception) {
            Log.w(TAG, "segmentation failed", error)
            return@withContext null
        } ?: return@withContext null

        // Mask dimensions match the input frame unless the raw-size option is on;
        // either way the buffer holds one float per pixel in native order.
        val buffer = mask.buffer.apply { rewind() }
        val width = mask.width
        val height = mask.height
        val values = FloatArray(width * height)
        var i = 0
        while (i < values.size && buffer.remaining() >= 4) {
            values[i] = buffer.float.coerceIn(0f, 1f)
            i++
        }
        if (i < values.size) null else SubjectMask(values, width, height)
    }

    private fun clientOrNull(): Segmenter? {
        client?.let { return it }
        return synchronized(this) {
            client ?: runCatching {
                Segmentation.getClient(
                    SelfieSegmenterOptions.Builder()
                        .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                        .build()
                )
            }.onFailure { error ->
                Log.w(TAG, "ML Kit segmenter unavailable", error)
                unavailableReason = error.message ?: "Model segmentasi tidak tersedia"
            }.getOrNull()?.also { client = it }
        }
    }

    private companion object {
        const val TAG = "ZcutMatting"
        /** Below this the model has nothing to work with. */
        const val MIN_SIDE = 256
    }
}

private suspend fun com.google.android.gms.tasks.Task<
    com.google.mlkit.vision.segmentation.SegmentationMask
    >.awaitMask(): com.google.mlkit.vision.segmentation.SegmentationMask? =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { mask ->
            if (continuation.isActive) continuation.resume(mask)
        }
        addOnFailureListener { error ->
            if (continuation.isActive) continuation.resumeWithException(error)
        }
    }
