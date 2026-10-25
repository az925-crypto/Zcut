package com.zaaam.zcut.core.render

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Face geometry with ML Kit's bundled face model: everything runs on the device
 * and no frame leaves it.
 *
 * Landmarks are requested because the local retouch operations need an anchor
 * per feature, exactly like the shipped reshape package drives its distortion
 * mesh from landmark positions. A missing landmark stays missing instead of
 * being replaced by a guess.
 */
class MlKitFaceDetector : FaceDetector {

    @Volatile
    private var client: com.google.mlkit.vision.face.FaceDetector? = null

    /** Model load failed (unsupported ABI, missing native lib); say so, do not guess. */
    @Volatile
    var unavailableReason: String? = null
        private set

    override suspend fun detect(frame: Bitmap): List<FaceGeometry>? =
        withContext(Dispatchers.Default) {
            if (frame.width < MIN_SIDE || frame.height < MIN_SIDE) {
                // Below this the model has nothing usable to work with.
                return@withContext emptyList()
            }
            // null (not empty) so the screen can say "model unavailable" instead
            // of blaming the frame for a model that never loaded.
            val detector = clientOrNull() ?: return@withContext null
            val faces = try {
                detector.process(InputImage.fromBitmap(frame, 0)).awaitFaces()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                Log.w(TAG, "face detection failed", error)
                return@withContext emptyList()
            }
            faces
                .take(MAX_FACES)
                .map { it.toGeometry(frame.width, frame.height) }
                .filter { it.bounds.width > 0f && it.bounds.height > 0f }
        }

    private fun clientOrNull(): com.google.mlkit.vision.face.FaceDetector? {
        client?.let { return it }
        return synchronized(this) {
            client ?: runCatching {
                FaceDetection.getClient(
                    FaceDetectorOptions.Builder()
                        // One still frame per edit, so accuracy beats latency here.
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                        .setMinFaceSize(MIN_FACE_FRACTION)
                        .build()
                )
            }.onFailure { error ->
                Log.w(TAG, "ML Kit face detector unavailable", error)
                unavailableReason = error.message ?: "Model deteksi wajah tidak tersedia"
            }.getOrNull()?.also { client = it }
        }
    }

    private companion object {
        const val TAG = "ZcutBeauty"
        const val MIN_SIDE = 128
        const val MIN_FACE_FRACTION = 0.1f

        /**
         * The mockup shows one face chip; more than a handful of faces would
         * also multiply the per-pixel work on a thumbnail.
         */
        const val MAX_FACES = 3
    }
}

/**
 * Converts one ML Kit face into normalised geometry. Positions come back in the
 * analysed image's pixel space, so they are divided by the frame size; sorting
 * the eyes keeps a mirrored input from flipping every local operation.
 */
private fun Face.toGeometry(width: Int, height: Int): FaceGeometry {
    val box = boundingBox
    fun landmark(type: Int): FacePoint? =
        getLandmark(type)?.position?.let { FacePoint.of(it.x, it.y, width, height) }

    return FaceGeometry(
        bounds = FaceBox.of(
            left = box.left.toFloat(),
            top = box.top.toFloat(),
            right = box.right.toFloat(),
            bottom = box.bottom.toFloat(),
            width = width,
            height = height
        ),
        leftEye = landmark(FaceLandmark.LEFT_EYE),
        rightEye = landmark(FaceLandmark.RIGHT_EYE),
        noseBase = landmark(FaceLandmark.NOSE_BASE),
        mouthLeft = landmark(FaceLandmark.MOUTH_LEFT),
        mouthRight = landmark(FaceLandmark.MOUTH_RIGHT),
        mouthBottom = landmark(FaceLandmark.MOUTH_BOTTOM)
    ).withCanonicalEyes()
}

private suspend fun com.google.android.gms.tasks.Task<List<Face>>.awaitFaces(): List<Face> =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { faces ->
            if (continuation.isActive) continuation.resume(faces)
        }
        addOnFailureListener { error ->
            if (continuation.isActive) continuation.resumeWithException(error)
        }
    }
