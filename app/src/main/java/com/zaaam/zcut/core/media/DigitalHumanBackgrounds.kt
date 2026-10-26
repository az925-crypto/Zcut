package com.zaaam.zcut.core.media

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import com.zaaam.zcut.core.domain.DigitalHumanAvatar
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Renders the placeholder background for a digital-human preparation draft.
 *
 * The avatar itself is backend-only, so the local draft gets a real 1080×1920
 * gradient still (not a copied template) sized to the voice track. Keeping the
 * file on disk means the draft survives restarts and exports normally.
 */
object DigitalHumanBackgrounds {

    const val WIDTH = 1_080
    const val HEIGHT = 1_920

    suspend fun render(avatar: DigitalHumanAvatar, output: File): Boolean =
        withContext(Dispatchers.IO) {
            var bitmap: Bitmap? = null
            try {
                output.parentFile?.mkdirs()
                bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                paint.shader = LinearGradient(
                    0f,
                    0f,
                    WIDTH.toFloat(),
                    HEIGHT.toFloat(),
                    intArrayOf(avatar.colorTop.toInt(), avatar.colorBottom.toInt()),
                    null,
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), paint)

                // A single soft ring, echoing the mockup's preview treatment.
                paint.shader = null
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 6f
                paint.color = 0x33FFFFFF
                canvas.drawCircle(WIDTH * 0.82f, HEIGHT * 0.18f, WIDTH * 0.28f, paint)

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
