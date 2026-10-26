package com.zaaam.zcut.feature.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaaam.zcut.core.domain.CaptionEditor
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.ui.components.ZcutText
import kotlinx.coroutines.flow.StateFlow

/**
 * Compose overlay for caption cues. Media3 only maps one visual sequence, so
 * captions — like text — are composited here, in the editor frame, and are not
 * burned into the exported file yet (documented render limit).
 *
 * Only the first cue covering the playhead is drawn; overlapping cues resolve to
 * the earliest one instead of stacking on top of each other.
 */
@Composable
fun CaptionOverlayLayer(
    project: Project,
    playhead: StateFlow<Long>,
    modifier: Modifier = Modifier
) {
    val cues = remember(project.transcript) { CaptionEditor.ordered(project.transcript) }
    if (cues.isEmpty()) return
    val position by playhead.collectAsStateWithLifecycle()
    BoxWithConstraints(modifier = modifier) {
        val frameHeightDp = maxHeight.value
        val active = cues.firstOrNull {
            position >= it.startMs && position < it.endMs
        } ?: return@BoxWithConstraints
        val config = CaptionEditor.styleOf(active).copy(text = active.text)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                // Clears the timestamp scrim at the bottom of the preview.
                .padding(start = 12.dp, end = 12.dp, bottom = 72.dp),
            contentAlignment = Alignment.Center
        ) {
            ZcutText(config = config, frameHeightDp = frameHeightDp)
        }
    }
}