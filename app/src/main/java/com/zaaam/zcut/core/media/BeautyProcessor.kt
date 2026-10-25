package com.zaaam.zcut.core.media

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.zaaam.zcut.core.domain.BeautyEditor
import com.zaaam.zcut.core.domain.BeautySlider
import com.zaaam.zcut.core.model.BeautyConfig
import com.zaaam.zcut.core.render.FaceBox
import com.zaaam.zcut.core.render.FaceGeometry
import kotlin.math.max
import kotlin.math.min

/**
 * Local retouch: the pixel operations behind the beauty sliders. Nothing is
 * uploaded and no model is called here — the face geometry comes from
 * [com.zaaam.zcut.core.render.FaceDetector].
 *
 * The reshape model mirrors the shipped package
 * (`assets/zip/FaceReshape_V2/distortion.json`): each distortion is one landmark
 * with `offsetX/offsetY` applied inside `radiusX/radiusY` and a `scale`, and the
 * shipped file uses two kinds (`type: 1` and `type: 2`). Here that becomes one
 * displacement field with two attractor kinds, [WarpKind.OFFSET] and
 * [WarpKind.EXPAND]; the mapping from `type` to kind is *inferred* from the
 * config shape, not observed in code, and the magnitudes are ours.
 *
 * Quality ceiling: this is CPU bitmap work on one still frame, not a GPU mesh
 * warp over video. It is honest about it in the UI and docs.
 */
object BeautyProcessor {

    /** Blur radius as a fraction of the frame's short side. */
    private const val BLUR_RADIUS_FRACTION = 0.012f

    /** Softness of the face / mouth masks. */
    private const val MASK_FEATHER = 0.45f

    /** Lip tint colour (ours; the original ships a makeup effect id we do not have). */
    private const val LIP_COLOR = 0xFFFF5A78.toInt()

    /** Max horizontal pull per face side, as a fraction of the face width. */
    private const val SLIM_MAX_PULL = 0.10f

    /** Max magnification of one eye, as a fraction of the eye region radius. */
    private const val EYE_MAX_ZOOM = 0.28f

    /** Max pinch around the nose, same unit as [EYE_MAX_ZOOM]. */
    private const val NOSE_MAX_PINCH = 0.22f

    /**
     * Applies every active slider of [config] to one frame.
     *
     * `null` means "nothing to show": no face to work on, or no slider moved.
     * The caller renders the untouched frame and says why.
     */
    fun apply(frame: Bitmap, face: FaceGeometry?, config: BeautyConfig?): Bitmap? {
        if (face == null || config == null || !BeautyEditor.isActive(config)) return null
        if (face.bounds.width <= 0f || face.bounds.height <= 0f) return null

        var out = frame.copy(Bitmap.Config.ARGB_8888, true) ?: return null
        if (needsReshape(config)) out = reshape(out, face, config)
        out = smooth(out, face, config)
        out = whiten(out, face, config)
        out = lips(out, face, config)
        return out
    }

    // ---------------------------------------------------------------- reshape

    private fun needsReshape(config: BeautyConfig): Boolean =
        BeautySlider.FACE_SLIM.strength(config.faceSlim) > 0f ||
            BeautySlider.EYES.strength(config.eyes) > 0f ||
            BeautySlider.NOSE.strength(config.nose) > 0f

    /** One attractor of the displacement field. */
    private class Warp(
        val kind: Kind,
        val centerX: Float,
        val centerY: Float,
        val radiusX: Float,
        val radiusY: Float,
        val offsetX: Float,
        val offsetY: Float,
        val expand: Float
    ) {
        enum class Kind { OFFSET, EXPAND }
    }

    /**
     * Applies face slim / eye size / nose wing in a single pass: sampling the
     * source once per pixel beats walking the frame three times.
     */
    private fun reshape(source: Bitmap, face: FaceGeometry, config: BeautyConfig): Bitmap {
        val width = source.width
        val height = source.height
        val warps = warps(face, config, width, height)
        if (warps.isEmpty()) return source

        val src = IntArray(width * height)
        source.getPixels(src, 0, width, 0, 0, width, height)
        val dst = IntArray(width * height)

        for (y in 0 until height) {
            val fy = y.toFloat()
            for (x in 0 until width) {
                var sx = x.toFloat()
                var sy = fy
                val fx = x.toFloat()
                for (warp in warps) {
                    val nx = (fx - warp.centerX) / warp.radiusX
                    val ny = (fy - warp.centerY) / warp.radiusY
                    val inside = 1f - (nx * nx + ny * ny)
                    if (inside <= 0f) continue
                    val falloff = inside * inside * (3f - 2f * inside)
                    when (warp.kind) {
                        Warp.Kind.OFFSET -> {
                            sx += warp.offsetX * falloff
                            sy += warp.offsetY * falloff
                        }
                        Warp.Kind.EXPAND -> {
                            sx -= (fx - warp.centerX) * warp.expand * falloff
                            sy -= (fy - warp.centerY) * warp.expand * falloff
                        }
                    }
                }
                dst[y * width + x] = if (sx == fx && sy == fy) {
                    src[y * width + x]
                } else {
                    bilinear(src, width, height, sx, sy)
                }
            }
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(dst, 0, width, 0, 0, width, height)
        source.recycle()
        return result
    }

    private fun warps(face: FaceGeometry, config: BeautyConfig, width: Int, height: Int): List<Warp> {
        val box = face.bounds
        val boxWidth = box.width * width
        val boxHeight = box.height * height
        val centerX = box.centerX * width
        val centerY = box.centerY * height
        val result = mutableListOf<Warp>()

        val slim = BeautySlider.FACE_SLIM.strength(config.faceSlim)
        if (slim > 0f && boxWidth > 4f) {
            // Pull both sides toward the face centre: the sampled pixels come
            // from further out, so the outline lands further in.
            val pull = slam(slim) * SLIM_MAX_PULL * boxWidth
            val radiusX = (boxWidth * 0.6f).coerceAtLeast(2f)
            val radiusY = (boxHeight * 0.75f).coerceAtLeast(2f)
            result += Warp(
                kind = Warp.Kind.OFFSET,
                centerX = box.left * width,
                centerY = centerY,
                radiusX = radiusX,
                radiusY = radiusY,
                offsetX = -pull,
                offsetY = 0f,
                expand = 0f
            )
            result += Warp(
                kind = Warp.Kind.OFFSET,
                centerX = box.right * width,
                centerY = centerY,
                radiusX = radiusX,
                radiusY = radiusY,
                offsetX = pull,
                offsetY = 0f,
                expand = 0f
            )
        }

        val eyes = BeautySlider.EYES.strength(config.eyes)
        val eyeRadius = face.eyeDistance() * width * 0.35f
        if (eyes > 0f && eyeRadius > 2f) {
            for (eye in listOfNotNull(face.leftEye, face.rightEye)) {
                result += Warp(
                    kind = Warp.Kind.EXPAND,
                    centerX = eye.x * width,
                    centerY = eye.y * height,
                    radiusX = eyeRadius,
                    radiusY = eyeRadius,
                    offsetX = 0f,
                    offsetY = 0f,
                    expand = slam(eyes) * EYE_MAX_ZOOM
                )
            }
        }

        val nose = BeautySlider.NOSE.strength(config.nose)
        if (nose > 0f) {
            // Nose base when the model returned it, face centre otherwise: a
            // pinch around the wrong point is worse than a coarser one.
            val anchorX = (face.noseBase?.x ?: box.centerX) * width
            val anchorY = (face.noseBase?.y ?: box.centerY) * height
            val radius = (boxWidth * 0.30f).coerceAtLeast(2f)
            result += Warp(
                kind = Warp.Kind.EXPAND,
                centerX = anchorX,
                centerY = anchorY,
                radiusX = radius,
                radiusY = radius,
                offsetX = 0f,
                offsetY = 0f,
                expand = -slam(nose) * NOSE_MAX_PINCH
            )
        }
        return result
    }

    /** Controls the pull at the very edge so a strong slider cannot tear the frame. */
    private fun slam(strength: Float): Float = strength.coerceIn(0f, 1f)

    // ------------------------------------------------------------------ skin

    private fun smooth(source: Bitmap, face: FaceGeometry, config: BeautyConfig): Bitmap {
        val strength = BeautySlider.SMOOTH.strength(config.smooth)
        if (strength <= 0f) return source
        val mix = BeautyEditor.smoothMix(strength, config.preserveTexture)
        if (mix <= 0f) return source

        val width = source.width
        val height = source.height
        val radius = max(1f, min(width, height) * BLUR_RADIUS_FRACTION)
        val blurred = boxBlur(source, radius)
        val mask = faceMask(face.bounds, width, height)
        val layer = maskedLayer(width, height, mask) { canvas ->
            canvas.drawBitmap(blurred, 0f, 0f, FILTERED)
        }
        val out = source.copy(Bitmap.Config.ARGB_8888, true) ?: source
        Canvas(out).drawBitmap(layer, 0f, 0f, alphaPaint(mix))

        blurred.recycle()
        mask.recycle()
        layer.recycle()
        if (out !== source) source.recycle()
        return out
    }

    private fun whiten(source: Bitmap, face: FaceGeometry, config: BeautyConfig): Bitmap {
        val strength = BeautySlider.WHITEN.strength(config.whiten)
        if (strength <= 0f) return source
        val mix = BeautyEditor.whitenMix(strength)
        if (mix <= 0f) return source

        val width = source.width
        val height = source.height
        // Screen blend with a grey layer lightens mid-tones without blowing
        // highlights to pure white; the grey level is scaled by the slider.
        val level = (mix * 120f).toInt().coerceIn(0, 255)
        val mask = faceMask(face.bounds, width, height)
        val layer = maskedLayer(width, height, mask) { canvas ->
            canvas.drawBitmap(source, 0f, 0f, FILTERED)
            canvas.drawColor(Color.rgb(level, level, level), PorterDuff.Mode.SCREEN)
        }
        val out = source.copy(Bitmap.Config.ARGB_8888, true) ?: source
        Canvas(out).drawBitmap(layer, 0f, 0f, alphaPaint(1f))

        mask.recycle()
        layer.recycle()
        if (out !== source) source.recycle()
        return out
    }

    private fun lips(source: Bitmap, face: FaceGeometry, config: BeautyConfig): Bitmap {
        val strength = BeautySlider.LIPS.strength(config.lips)
        if (strength <= 0f) return source
        val mix = BeautyEditor.lipMix(strength)
        if (mix <= 0f) return source

        val width = source.width
        val height = source.height
        val mouth = mouthBox(face)
        if (mouth.width() <= 1f || mouth.height() <= 1f) return source

        val mask = softEllipseMask(mouth, width, height)
        val layer = maskedLayer(width, height, mask) { canvas ->
            canvas.drawBitmap(source, 0f, 0f, FILTERED)
            canvas.drawColor(LIP_COLOR, PorterDuff.Mode.MULTIPLY)
        }
        val out = source.copy(Bitmap.Config.ARGB_8888, true) ?: source
        Canvas(out).drawBitmap(layer, 0f, 0f, alphaPaint(mix))

        mask.recycle()
        layer.recycle()
        if (out !== source) source.recycle()
        return out
    }

    /**
     * Mouth region in pixels. Uses the returned landmarks, or falls back to the
     * lower third of the face box — never to a guessed "typical" position.
     */
    private fun mouthBox(face: FaceGeometry): RectF {
        val box = face.bounds
        val left = face.mouthLeft
        val right = face.mouthRight
        val bottom = face.mouthBottom
        if (left != null && right != null && bottom != null) {
            val top = face.noseBase?.y ?: (left.y - (bottom.y - left.y) * 0.6f)
            return RectF(left.x, top, right.x, bottom.y)
        }
        val top = box.top + box.height * 0.62f
        val bottomY = box.top + box.height * 0.90f
        val inset = box.width * 0.22f
        return RectF(box.left + inset, top, box.right - inset, bottomY)
    }

    // ------------------------------------------------------------------ masks

    private fun faceMask(box: FaceBox, width: Int, height: Int): Bitmap = softEllipseMask(
        RectF(
            box.left * width,
            box.top * height,
            box.right * width,
            box.bottom * height
        ),
        width,
        height
    )

    /**
     * White ellipse with a soft edge, used as the `DST_IN` source so a colour or
     * blur change only lands on the region it belongs to.
     */
    private fun softEllipseMask(rect: RectF, width: Int, height: Int): Bitmap {
        val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val centerX = rect.centerX()
        val centerY = rect.centerY()
        val radiusX = (rect.width() / 2f).coerceAtLeast(1f)
        val radiusY = (rect.height() / 2f).coerceAtLeast(1f)
        val radius = max(radiusX, radiusY)
        val shader = RadialGradient(
            centerX,
            centerY,
            radius,
            intArrayOf(Color.WHITE, Color.WHITE, Color.TRANSPARENT),
            floatArrayOf(0f, 1f - MASK_FEATHER, 1f),
            Shader.TileMode.CLAMP
        )
        shader.setLocalMatrix(
            Matrix().apply {
                setScale(radiusX / radius, radiusY / radius, centerX, centerY)
            }
        )
        Canvas(mask).drawRect(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            Paint().apply {
                this.shader = shader
                isFilterBitmap = true
            }
        )
        return mask
    }

    private inline fun maskedLayer(
        width: Int,
        height: Int,
        mask: Bitmap,
        paint: (Canvas) -> Unit
    ): Bitmap {
        val layer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(layer)
        paint(canvas)
        canvas.drawBitmap(mask, 0f, 0f, DST_IN)
        return layer
    }

    // ------------------------------------------------------------------ blur

    /**
     * Cheap blur: down/up-scale twice. Good enough on a 720px preview frame and
     * far cheaper than a stack blur per slider release.
     * ponytail: two-pass scale blur; upgrade path is a real box/gaussian pass if
     * the smoothing ever looks blocky on a high-resolution frame.
     */
    private fun boxBlur(source: Bitmap, radius: Float): Bitmap {
        val smallEdge = max(2, (min(source.width, source.height) / max(2f, radius)).toInt())
        val ratio = smallEdge.toFloat() / min(source.width, source.height).toFloat()
        val smallWidth = max(2, (source.width * ratio).toInt())
        val smallHeight = max(2, (source.height * ratio).toInt())

        val first = Bitmap.createScaledBitmap(source, smallWidth, smallHeight, true)
        val second = Bitmap.createScaledBitmap(first, max(2, smallWidth / 2), max(2, smallHeight / 2), true)
        val back = Bitmap.createScaledBitmap(second, source.width, source.height, true)
        first.recycle()
        second.recycle()
        return back
    }

    // --------------------------------------------------------------- helpers

    private fun alphaPaint(alpha: Float): Paint = Paint().apply {
        this.alpha = (alpha.coerceIn(0f, 1f) * 255f).toInt()
        isFilterBitmap = true
    }

    /** Bilinear sample; coordinates outside the frame clamp to the edge. */
    private fun bilinear(pixels: IntArray, width: Int, height: Int, x: Float, y: Float): Int {
        val cx = x.coerceIn(0f, (width - 1).toFloat())
        val cy = y.coerceIn(0f, (height - 1).toFloat())
        val x0 = cx.toInt()
        val y0 = cy.toInt()
        val x1 = min(x0 + 1, width - 1)
        val y1 = min(y0 + 1, height - 1)
        val fx = cx - x0
        val fy = cy - y0

        val c00 = pixels[y0 * width + x0]
        val c10 = pixels[y0 * width + x1]
        val c01 = pixels[y1 * width + x0]
        val c11 = pixels[y1 * width + x1]

        var result = 0
        for (shift in CHANNEL_SHIFTS) {
            val top = channel(c00, shift) + (channel(c10, shift) - channel(c00, shift)) * fx
            val bottom = channel(c01, shift) + (channel(c11, shift) - channel(c01, shift)) * fx
            val value = (top + (bottom - top) * fy).toInt().coerceIn(0, 255)
            result = result or (value shl shift)
        }
        return result
    }

    private fun channel(color: Int, shift: Int): Float = ((color shr shift) and 0xFF).toFloat()

    private val CHANNEL_SHIFTS = intArrayOf(24, 16, 8, 0)

    private val FILTERED = Paint().apply { isFilterBitmap = true }

    private val DST_IN = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        isFilterBitmap = true
    }
}
