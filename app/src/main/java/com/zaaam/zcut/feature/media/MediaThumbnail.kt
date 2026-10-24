package com.zaaam.zcut.feature.media

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads one thumbnail off the main thread and keeps it until [key] changes.
 * A null [key] short-circuits, so callers do not need their own guard.
 */
@Composable
fun rememberThumbnail(
    key: String?,
    loader: suspend () -> Bitmap?
): State<ImageBitmap?> = produceState<ImageBitmap?>(initialValue = null, key) {
    value = if (key == null) {
        null
    } else {
        try {
            withContext(Dispatchers.IO) { loader()?.asImageBitmap() }
        } catch (cancel: kotlinx.coroutines.CancellationException) {
            throw cancel
        } catch (error: Exception) {
            null
        }
    }
}
