package com.zaaam.zcut.core.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.zaaam.zcut.core.domain.BeautyEditor
import com.zaaam.zcut.core.domain.BrushStroke
import com.zaaam.zcut.core.domain.ColorPipeline
import com.zaaam.zcut.core.domain.PhotoEditLimits
import com.zaaam.zcut.core.domain.PhotoEditState
import com.zaaam.zcut.core.domain.PhotoExportFormat
import com.zaaam.zcut.core.domain.StickerCatalog
import com.zaaam.zcut.core.model.FilterConfig
import com.zaaam.zcut.core.model.MattingBackground
import com.zaaam.zcut.core.model.TextConfig
import com.zaaam.zcut.core.render.FaceGeometry
import com.zaaam.zcut.core.render.SubjectMask
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Pixel pipeline behind the photo editor. The recipe ([PhotoEditState]) is the
 * source of truth; this only turns it into pixels for the preview and the export.
 *
 * Order: crop → transform → colour + vignette → retouch → cutout → overlays
 * (brush/text/sticker). Everything is local, CPU-side bitmap work; there is no
 * LUT texture or GPU mesh, and the long side is bounded by
 * [PhotoEditLimits.MAX_DIMENSION] so a 50 MP source cannot blow up memory.
 */
object PhotoRenderer {

    /** Decodes [path] with a power-of-two sample size, bounded by [maxDimension]. */
    fun load(path: String, maxDimension: Int = PhotoEditLimits.MAX_DIMENSION): Bitmap? =
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeFile(path, options)
        }.getOrNull()

    fun sampleSize(width: Int, height: Int, maxDimension: Int): Int {
        if (maxDimension <= 0) return 1
        var sample = 1
        while (max(width, height) / (sample * 2) >= maxDimension) sample *= 2
        return sample
    }

    /**
     * Renders the recipe. [face] and [mask] are supplied by the screen when the
     * retouch/cutout tools are in use; null means "skip that step".
     */
    fun render(
        source: Bitmap,
        state: PhotoEditState,
        face: FaceGeometry? = null,
        mask: SubjectMask? = null
    ): Bitmap {
        val recipe = state.sanitized()
        var frame = cropAndTransform(source, recipe)
        frame = applyColor(frame, recipe)
        if (BeautyEditor.isActive(recipe.beauty)) {
            frame = BeautyProcessor.apply(frame, face, recipe.beauty) ?: frame
        }
        if (recipe.matting.enabled &&
            recipe.matting.background != MattingBackground.TRANSPARENT &&
            recipe.matting.background != MattingBackground.AI &&
            mask != null
        ) {
            frame = MattingComposer.compose(frame, mask, recipe.matting)
        }
        return drawOverlays(frame, recipe)
    }

    /** The export path: an unedited recipe keeps the decoded source untouched. */
    fun renderForExport(
        source: Bitmap,
        state: PhotoEditState,
        face: FaceGeometry? = null,
        mask: SubjectMask? = null
    ): Bitmap = if (!state.isEdited) source else render(source, state, face, mask)

    fun export(bitmap: Bitmap, format: PhotoExportFormat, output: File): Boolean = runCatching {
        output.parentFile?.mkdirs()
        val compress = when (format) {
            PhotoExportFormat.JPG -> Bitmap.CompressFormat.JPEG
            PhotoExportFormat.PNG -> Bitmap.CompressFormat.PNG
            PhotoExportFormat.WEBP -> Bitmap.CompressFormat.WEBP
        }
        FileOutputStream(output).use { stream ->
            bitmap.compress(compress, format.quality, stream)
        }
        output.isFile && output.length() > 0L
    }.getOrDefault(false)

    private fun cropAndTransform(source: Bitmap, state: PhotoEditState): Bitmap {
        val window = state.crop
        val x = (window.left * source.width).roundToInt().coerceIn(0, source.width - 1)
        val y = (window.top * source.height).roundToInt().coerceIn(0, source.height - 1)
        val width = max(1, (window.widthRatio * source.width).roundToInt())
        val height = max(1, (window.heightRatio * source.height).roundToInt())
        val safeWidth = min(width, source.width - x)
        val safeHeight = min(height, source.height - y)
        var frame = if (safeWidth == source.width && safeHeight == source.height) {
            source
        } else {
            Bitmap.createBitmap(source, x, y, safeWidth, safeHeight)
        }
        if (state.quarterTurns != 0 || state.flipHorizontal || state.flipVertical) {
            val matrix = Matrix().apply {
                postRotate((state.quarterTurns * 90).toFloat())
                postScale(
                    if (state.flipHorizontal) -1f else 1f,
                    if (state.flipVertical) -1f else 1f
                )
            }
            frame = Bitmap.createBitmap(frame, 0, 0, frame.width, frame.height, matrix, true)
        }
        return frame
    }

    private fun applyColor(source: Bitmap, state: PhotoEditState): Bitmap {
        // isNeutral covers every numeric field, including vignette.
        if (state.filterId == null && state.color.isNeutral) return source
        val matrix = ColorPipeline.buildMatrix(
            filter = state.filterId?.let { FilterConfig(id = it, intensity = state.filterIntensity) },
            color = state.color
        )
        val vignette = state.color.vignette.coerceIn(0f, 1f)
        return intoBitmap(source) { canvas ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(ColorMatrix(matrix))
                isFilterBitmap = true
            }
            canvas.drawBitmap(source, 0f, 0f, paint)
            if (vignette > 0f) {
                val radius = max(source.width, source.height) * 0.75f
                val alpha = (vignette * 255f).roundToInt()
                val shader = RadialGradient(
                    source.width / 2f,
                    source.height / 2f,
                    radius,
                    intArrayOf(0x00000000, 0x00000000, alpha shl 24),
                    floatArrayOf(0f, 0.55f, 1f),
                    Shader.TileMode.CLAMP
                )
                val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
                canvas.drawRect(
                    0f,
                    0f,
                    source.width.toFloat(),
                    source.height.toFloat(),
                    vignettePaint
                )
            }
        }
    }

    private fun drawOverlays(source: Bitmap, state: PhotoEditState): Bitmap {
        val sticker = state.stickerId?.let { StickerCatalog.canonical(it) }
        val text = state.text
        val strokes = state.brush.filter { it.isDrawable }
        if (sticker == null && text == null && strokes.isEmpty()) return source

        return intoBitmap(source) { canvas ->
            canvas.drawBitmap(source, 0f, 0f, null)
            strokes.forEach { stroke -> drawStroke(canvas, source, stroke) }
            text?.let { drawText(canvas, source, it) }
            sticker?.let { drawSticker(canvas, source, it.glyph) }
        }
    }

    private fun drawStroke(canvas: Canvas, frame: Bitmap, stroke: BrushStroke) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = stroke.colorArgb.toInt()
            alpha = (stroke.opacity.coerceIn(0f, 1f) * 255f).roundToInt()
            strokeWidth = (stroke.widthFraction * frame.width).coerceAtLeast(1f)
        }
        val path = Path()
        stroke.points.forEachIndexed { index, point ->
            val x = point.x * frame.width
            val y = point.y * frame.height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, paint)
    }

    private fun drawText(canvas: Canvas, frame: Bitmap, config: TextConfig) {
        val text = config.text.trim()
        if (text.isEmpty()) return
        val size = (frame.width * 0.075f * (config.fontSizeSp / 42f)).coerceAtLeast(12f)
        val width = (frame.width * 0.9f).roundToInt().coerceAtLeast(1)
        val base = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.colorArgb.toInt()
            textSize = size
        }
        if (config.shadow) {
            base.setShadowLayer(size * 0.12f, 0f, size * 0.06f, 0xAA000000.toInt())
        }

        canvas.save()
        canvas.translate((frame.width - width) / 2f, 0f)
        val fill = layoutOf(text, base, width, config)
        val top = ((frame.height - fill.height) * 0.72f).coerceAtLeast(0f)
        canvas.translate(0f, top)
        fill.draw(canvas)
        if (config.strokeWidth > 0f) {
            val stroke = TextPaint(base).apply {
                style = Paint.Style.STROKE
                strokeWidth = config.strokeWidth
                color = config.strokeColorArgb.toInt()
                clearShadowLayer()
            }
            layoutOf(text, stroke, width, config).draw(canvas)
        }
        canvas.restore()
    }

    private fun layoutOf(
        text: String,
        paint: TextPaint,
        width: Int,
        config: TextConfig
    ): StaticLayout = StaticLayout.Builder
        .obtain(text, 0, text.length, paint, width)
        .setAlignment(Layout.Alignment.ALIGN_CENTER)
        .setLineSpacing(0f, config.lineHeight.coerceIn(0.8f, 2.5f))
        .build()

    private fun drawSticker(canvas: Canvas, frame: Bitmap, glyph: String) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = frame.width * 0.34f
            textAlign = Paint.Align.CENTER
        }
        val metrics = paint.fontMetrics
        val baseline = frame.height / 2f - (metrics.ascent + metrics.descent) / 2f
        canvas.drawText(glyph, frame.width / 2f, baseline, paint)
    }

    private inline fun intoBitmap(source: Bitmap, draw: (Canvas) -> Unit): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        draw(canvas)
        return output
    }
}