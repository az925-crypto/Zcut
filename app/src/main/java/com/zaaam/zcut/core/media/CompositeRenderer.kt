package com.zaaam.zcut.core.media

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import com.zaaam.zcut.core.domain.CellRect
import com.zaaam.zcut.core.domain.FacecamShape
import com.zaaam.zcut.core.domain.GameplayConfig
import com.zaaam.zcut.core.domain.GameplayGeometry
import com.zaaam.zcut.core.domain.SplitGeometry
import com.zaaam.zcut.core.domain.SplitScreenConfig
import java.io.File
import java.io.FileOutputStream

/**
 * Renders the local still composites for split screen, gameplay and live photo.
 *
 * Zcut composes **one high-resolution still** from the first frame of each
 * source; simultaneous multi-video playback/export is not implemented, and the
 * screens say so. Everything here is Canvas work on-device — no SDK, no upload.
 */
object CompositeRenderer {

    /** Long edge of an exported composite; matches the export presets. */
    const val DEFAULT_LONG_EDGE = 1_440

    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    fun renderSplit(
        frames: List<Bitmap?>,
        config: SplitScreenConfig,
        width: Int,
        height: Int
    ): Bitmap? {
        if (width <= 0 || height <= 0) return null
        val safe = config.sanitize()
        val gap = borderGap(safe.borderPercent, width, height)
        val rects = SplitGeometry.cellRects(safe.layout, width, height, gap)
        if (rects.isEmpty()) return null

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(safe.backgroundArgb.toInt())
        val radius = rects.minOf { minOf(it.width, it.height) } * safe.cornerPercent / 100f
        val stroke = if (gap > 0) (gap / 2f).coerceAtLeast(1f) else 0f

        rects.forEachIndexed { index, rect ->
            drawCell(
                canvas = canvas,
                bitmap = frames.getOrNull(index),
                rect = rect,
                radiusPx = radius,
                centerCrop = safe.centerCrop,
                backgroundArgb = safe.backgroundArgb,
                strokePx = stroke
            )
        }
        return output
    }

    fun renderGameplay(
        gameplay: Bitmap?,
        facecam: Bitmap?,
        config: GameplayConfig,
        width: Int,
        height: Int
    ): Bitmap? {
        if (width <= 0 || height <= 0) return null
        val safe = config.sanitize()
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(safe.backgroundArgb.toInt())
        drawFull(canvas, gameplay, width, height)

        val rect = GameplayGeometry.facecamRect(safe, width, height)
        if (rect.width > 0 && rect.height > 0) {
            val radius = when (safe.shape) {
                FacecamShape.CIRCLE -> minOf(rect.width, rect.height) / 2f
                FacecamShape.ROUNDED -> minOf(rect.width, rect.height) * 0.12f
            }
            drawCell(
                canvas = canvas,
                bitmap = facecam,
                rect = rect,
                radiusPx = radius,
                centerCrop = true,
                backgroundArgb = safe.backgroundArgb,
                strokePx = borderGap(safe.borderPercent, width, height)
                    .toFloat()
                    .coerceAtLeast(1f) / 2f
            )
        }
        return output
    }

    /** Writes [bitmap] as a JPEG; false on any I/O failure. */
    fun saveJpeg(bitmap: Bitmap, file: File, quality: Int = 92): Boolean = runCatching {
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { sink ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(1, 100), sink)
        }
    }.getOrDefault(false)

    private fun drawFull(canvas: Canvas, bitmap: Bitmap?, width: Int, height: Int) {
        if (bitmap == null || bitmap.width <= 0 || bitmap.height <= 0) return
        val src = SplitGeometry.centerCropRect(bitmap.width, bitmap.height, width, height)
        if (src.width <= 0 || src.height <= 0) return
        canvas.drawBitmap(
            bitmap,
            Rect(src.left, src.top, src.right, src.bottom),
            Rect(0, 0, width, height),
            bitmapPaint
        )
    }

    private fun drawCell(
        canvas: Canvas,
        bitmap: Bitmap?,
        rect: CellRect,
        radiusPx: Float,
        centerCrop: Boolean,
        backgroundArgb: Long,
        strokePx: Float
    ) {
        if (rect.width <= 0 || rect.height <= 0) return
        val dst = Rect(rect.left, rect.top, rect.right, rect.bottom)
        val path = Path().apply {
            addRoundRect(RectF(dst), radiusPx, radiusPx, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(path)
        canvas.drawColor(backgroundArgb.toInt())
        if (bitmap != null && bitmap.width > 0 && bitmap.height > 0) {
            if (centerCrop) {
                val src = SplitGeometry.centerCropRect(
                    bitmap.width,
                    bitmap.height,
                    rect.width,
                    rect.height
                )
                if (src.width > 0 && src.height > 0) {
                    canvas.drawBitmap(
                        bitmap,
                        Rect(src.left, src.top, src.right, src.bottom),
                        dst,
                        bitmapPaint
                    )
                }
            } else {
                val scale = minOf(
                    rect.width.toFloat() / bitmap.width,
                    rect.height.toFloat() / bitmap.height
                )
                val drawWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
                val drawHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
                val left = rect.left + (rect.width - drawWidth) / 2
                val top = rect.top + (rect.height - drawHeight) / 2
                canvas.drawBitmap(
                    bitmap,
                    null,
                    Rect(left, top, left + drawWidth, top + drawHeight),
                    bitmapPaint
                )
            }
        }
        canvas.restore()

        if (strokePx > 0f) {
            val inset = strokePx / 2f
            val strokeRect = RectF(
                dst.left + inset,
                dst.top + inset,
                dst.right - inset,
                dst.bottom - inset
            )
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = 0x40FFFFFF
                strokeWidth = strokePx
            }
            canvas.drawRoundRect(strokeRect, radiusPx, radiusPx, strokePaint)
        }
    }

    private fun borderGap(percent: Int, width: Int, height: Int): Int =
        (minOf(width, height) * percent.coerceIn(0, 20) / 300f).toInt()
}
