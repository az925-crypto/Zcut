package com.zaaam.zcut.core.media

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.zaaam.zcut.core.domain.MattingEditor
import com.zaaam.zcut.core.model.MattingBackground
import com.zaaam.zcut.core.model.MattingConfig
import com.zaaam.zcut.core.render.SubjectMask

/**
 * Turns a frame + [SubjectMask] into the cutout the editor shows: the subject
 * kept, everything else replaced by the chosen backdrop.
 *
 * All local bitmap work; nothing is uploaded and no model is called here.
 */
object MattingComposer {

    /** Background is downscaled to this edge before being stretched back (blur). */
    private const val BLUR_MIN_EDGE = 12

    fun compose(frame: Bitmap, mask: SubjectMask, config: MattingConfig): Bitmap {
        val width = frame.width
        val height = frame.height
        val scaledMask = scaleMask(mask, width, height)

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val backdrop = background(frame, config)
        canvas.drawBitmap(backdrop, 0f, 0f, null)
        backdrop.recycle()

        val cutout = cutout(frame, scaledMask, config)
        canvas.drawBitmap(cutout, 0f, 0f, null)

        cutout.recycle()
        return result
    }

    /** Alpha-only mask at frame size, built from the raw confidence values. */
    private fun cutout(frame: Bitmap, mask: SubjectMask, config: MattingConfig): Bitmap {
        val width = frame.width
        val height = frame.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val alpha = MattingEditor.matteAlpha(mask.at(x, y), config.edgeRefine)
                pixels[y * width + x] = (alpha * 255f).toInt().coerceIn(0, 255) shl 24 or 0x00FFFFFF
            }
        }
        val stencil = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        val cutout = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(cutout)
        canvas.drawBitmap(frame, 0f, 0f, null)
        canvas.drawBitmap(
            stencil,
            0f,
            0f,
            Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }
        )
        stencil.recycle()
        return cutout
    }

    private fun background(frame: Bitmap, config: MattingConfig): Bitmap {
        val width = frame.width
        val height = frame.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        when (config.background) {
            // Real transparency: the checkerboard comes from the screen.
            MattingBackground.TRANSPARENT -> Unit

            MattingBackground.COLOR -> canvas.drawColor(withAlpha(config.colorArgb))

            MattingBackground.BLUR -> {
                val edge = (BLUR_MIN_EDGE + (1f - config.blur) * Math.min(width, height) / 2f)
                    .toInt()
                    .coerceIn(BLUR_MIN_EDGE, Math.max(width, height))
                val small = Bitmap.createScaledBitmap(frame, edge, edge, true)
                canvas.drawBitmap(small, null, android.graphics.Rect(0, 0, width, height), FILTERED)
                small.recycle()
            }

            // Both need material we do not have locally (a picked still / a
            // generated backdrop), so the frame itself stands in rather than a
            // fabricated scene.
            MattingBackground.IMAGE, MattingBackground.AI -> canvas.drawBitmap(frame, 0f, 0f, null)
        }

        // A transparent backdrop has nothing to darken, so the shadow slider only
        // applies once the background is actually opaque.
        if (config.shadow > 0f && config.background != MattingBackground.TRANSPARENT) {
            canvas.drawColor(
                Color.argb((config.shadow.coerceIn(0f, 1f) * 255f).toInt(), 0, 0, 0)
            )
        }
        return bitmap
    }

    /**
     * The mask arrives at the model's own size, so it is resampled to the frame
     * by nearest neighbour: a matte is a soft gradient, and interpolating it
     * would smooth away exactly the edge detail we asked for.
     * ponytail: nearest sampling, upgrade path is a bilinear pass if visible.
     */
    private fun scaleMask(mask: SubjectMask, width: Int, height: Int): SubjectMask {
        if (mask.width == width && mask.height == height) return mask
        val values = FloatArray(width * height)
        for (y in 0 until height) {
            val sourceY = y * mask.height / height
            for (x in 0 until width) {
                values[y * width + x] = mask.confidence[sourceY * mask.width + x * mask.width / width]
            }
        }
        return SubjectMask(values, width, height)
    }

    private fun withAlpha(argb: Long): Int = Color.argb(255, red(argb), green(argb), blue(argb))

    private fun red(argb: Long) = ((argb shr 16) and 0xFF).toInt()
    private fun green(argb: Long) = ((argb shr 8) and 0xFF).toInt()
    private fun blue(argb: Long) = (argb and 0xFF).toInt()

    private val FILTERED = Paint().apply { isFilterBitmap = true }
}
