package com.zaaam.zcut.core.media

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Renders the Ad Maker's 9:16 product card still.
 *
 * The real ad maker composes product albums and branded components on-device
 * from the user's own material; this clean-room version draws a neutral card
 * with the user's product name and chosen CTA, so the draft has a real visual
 * without copying any template, font or brand asset.
 */
object AdBackdrop {

    const val WIDTH = 1_080
    const val HEIGHT = 1_920

    private const val ACCENT = 0xFFC8F04A.toInt()
    private const val TOP = 0xFF0B1020.toInt()
    private const val BOTTOM = 0xFF1B2B4B.toInt()

    suspend fun render(product: String, cta: String, output: File): Boolean =
        withContext(Dispatchers.IO) {
            var bitmap: Bitmap? = null
            try {
                output.parentFile?.mkdirs()
                bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)

                val background = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f,
                        0f,
                        WIDTH.toFloat(),
                        HEIGHT.toFloat(),
                        TOP,
                        BOTTOM,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), background)

                // Soft accent halo, echoing the mockup's product-ad preview.
                val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 5f
                    color = 0x33C8F04A
                }
                canvas.drawCircle(WIDTH * 0.78f, HEIGHT * 0.24f, WIDTH * 0.34f, halo)
                canvas.drawCircle(WIDTH * 0.18f, HEIGHT * 0.68f, WIDTH * 0.22f, halo)

                val namePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFFFFFFFF.toInt()
                    textSize = 86f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    isAntiAlias = true
                }
                val title = product.trim().ifBlank { "Product ad" }
                val titleLayout = StaticLayout.Builder
                    .obtain(title, 0, title.length, namePaint, WIDTH - 160)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setLineSpacing(0f, 1.08f)
                    .setEllipsize(TextUtils.TruncateAt.END)
                    .setMaxLines(3)
                    .build()
                canvas.save()
                canvas.translate(80f, HEIGHT * 0.42f)
                titleLayout.draw(canvas)
                canvas.restore()

                val captionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0x99FFFFFF.toInt()
                    textSize = 38f
                    letterSpacing = 0.18f
                    typeface = android.graphics.Typeface.DEFAULT
                }
                canvas.drawText("ZCUT AD · LOCAL", 80f, HEIGHT * 0.36f, captionPaint)

                // CTA pill at the bottom.
                val label = cta.trim().ifBlank { "Shop now" }
                val ctaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF0B1020.toInt()
                    textSize = 44f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.CENTER
                }
                val pillWidth = ctaPaint.measureText(label) + 128f
                val pill = RectF(
                    (WIDTH - pillWidth) / 2f,
                    HEIGHT * 0.78f,
                    (WIDTH + pillWidth) / 2f,
                    HEIGHT * 0.78f + 120f
                )
                val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ACCENT }
                canvas.drawRoundRect(pill, 60f, 60f, pillPaint)
                canvas.drawText(
                    label,
                    WIDTH / 2f,
                    pill.centerY() - (ctaPaint.ascent() + ctaPaint.descent()) / 2f,
                    ctaPaint
                )

                FileOutputStream(output).use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 92, stream)
                }
                output.isFile && output.length() > 0L
            } catch (error: Exception) {
                output.delete()
                false
            } finally {
                bitmap?.recycle()
            }
        }
}
