package com.zaaam.zcut.feature.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaaam.zcut.core.domain.KeyframeEditor
import com.zaaam.zcut.core.domain.TextAnimator
import com.zaaam.zcut.core.domain.TextEditor
import com.zaaam.zcut.core.model.AnimatableProperty
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.ui.components.ZcutText
import kotlinx.coroutines.flow.StateFlow

/**
 * Compose overlay for text clips. Media3 only maps one visual + one audio
 * sequence, so text is composited here, in the editor frame, and later burned
 * into the export in M13. Playhead is collected in this child so the 30 fps
 * tick does not recompose the player or the timeline.
 */
@Composable
fun TextOverlayLayer(
    project: Project,
    playhead: StateFlow<Long>,
    modifier: Modifier = Modifier
) {
    val clips = remember(project.timeline) { TextEditor.textClips(project.timeline) }
    if (clips.isEmpty()) return
    val position by playhead.collectAsStateWithLifecycle()
    BoxWithConstraints(modifier = modifier) {
        val frameWidthDp = maxWidth.value
        val frameHeightDp = maxHeight.value
        clips.forEach { clip ->
            if (position < clip.startMs || position >= clip.endMs) return@forEach
            val config = clip.text ?: return@forEach
            val elapsed = position - clip.startMs
            val animation = TextAnimator.frame(
                animation = config.animation,
                elapsedMs = elapsed,
                durationMs = clip.durationMs,
                textLength = config.text.length,
                seed = clip.id.hashCode()
            )
            // `KeyframeEditor.sample` already falls back to `clip.opacity`, so
            // do not multiply by it again.
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
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = offsetX.dp, y = offsetY.dp)
                    .graphicsLayer {
                        rotationZ = rotation
                        scaleX = scale
                        scaleY = scale
                    }
            ) {
                ZcutText(
                    config = config,
                    frameHeightDp = frameHeightDp,
                    frame = animation.copy(alpha = animation.alpha * opacity)
                )
            }
        }
    }
}
