package com.zaaam.zcut.feature.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaaam.zcut.core.domain.KeyframeEditor
import com.zaaam.zcut.core.domain.StickerEditor
import com.zaaam.zcut.core.model.AnimatableProperty
import com.zaaam.zcut.core.model.Project
import kotlinx.coroutines.flow.StateFlow

/**
 * Compose overlay for sticker clips (M16 completion: the sticker was only drawn
 * in the tool preview before).
 *
 * Placement, scale, rotation, flip and opacity all read from the clip's own
 * [com.zaaam.zcut.core.model.Transform]/opacity, sampled through the keyframe
 * editor, so a sticker animates exactly like any other clip. Stickers are not
 * burned into the export yet (same render limit as text).
 */
@Composable
fun StickerOverlayLayer(
    project: Project,
    playhead: StateFlow<Long>,
    modifier: Modifier = Modifier
) {
    val clips = remember(project.timeline) { StickerEditor.stickerClips(project.timeline) }
    if (clips.isEmpty()) return
    val position by playhead.collectAsStateWithLifecycle()
    BoxWithConstraints(modifier = modifier) {
        val frameWidthDp = maxWidth.value
        val frameHeightDp = maxHeight.value
        clips.forEach { clip ->
            if (position < clip.startMs || position >= clip.endMs) return@forEach
            val elapsed = position - clip.startMs
            // `sample` already falls back to the static value, so opacity is not
            // multiplied by `clip.opacity` a second time.
            val opacity = KeyframeEditor.sample(clip, AnimatableProperty.OPACITY, elapsed)
            val offsetX = KeyframeEditor.sample(
                clip,
                AnimatableProperty.POSITION_X,
                elapsed
            ) / 100f * frameWidthDp
            val offsetY = KeyframeEditor.sample(
                clip,
                AnimatableProperty.POSITION_Y,
                elapsed
            ) / 100f * frameHeightDp
            val scale = KeyframeEditor.sample(clip, AnimatableProperty.SCALE, elapsed)
            val rotation = KeyframeEditor.sample(clip, AnimatableProperty.ROTATION, elapsed)
            Text(
                text = StickerEditor.glyph(clip),
                fontSize = (40f * scale).coerceIn(12f, 220f).sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = offsetX.dp, y = offsetY.dp)
                    .graphicsLayer {
                        rotationZ = rotation
                        scaleX = if (clip.transform.flipHorizontal) -1f else 1f
                        scaleY = if (clip.transform.flipVertical) -1f else 1f
                    }
                    .alpha(opacity.coerceIn(0f, 1f))
            )
        }
    }
}