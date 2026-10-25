package com.zaaam.zcut.feature.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaaam.zcut.core.media.MediaThumbnails
import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.ClipType
import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.model.Track
import com.zaaam.zcut.core.model.TrackType
import com.zaaam.zcut.core.util.TimeFormat
import com.zaaam.zcut.feature.media.rememberThumbnail
import com.zaaam.zcut.ui.theme.ZcutAmber
import com.zaaam.zcut.ui.theme.ZcutCoral
import com.zaaam.zcut.ui.theme.ZcutCyan
import com.zaaam.zcut.ui.theme.ZcutGreen
import com.zaaam.zcut.ui.theme.ZcutLime
import com.zaaam.zcut.ui.theme.ZcutOnPrimary
import com.zaaam.zcut.ui.theme.ZcutViolet
import kotlinx.coroutines.flow.StateFlow

/**
 * Multi-track timeline: one shared horizontal scroll state drives the ruler and
 * every lane, so the playhead can never drift from the clips. Tapping the ruler
 * or a lane seeks; tapping a clip selects it.
 */
@Composable
fun TimelineCanvas(
    project: Project,
    playhead: StateFlow<Long>,
    selectedClipId: String?,
    onSeek: (Long) -> Unit,
    onSelectClip: (String) -> Unit,
    thumbnails: MediaThumbnails,
    modifier: Modifier = Modifier
) {
    // Collected as Compose state but only read inside the layout lambdas below:
    // a moving playhead invalidates placement, not composition of every clip.
    val playheadMs = playhead.collectAsStateWithLifecycle()
    val tracks = EditorTimeline.orderedTracks(project.timeline.tracks)
    val durationMs = project.timeline.durationMs.coerceAtLeast(1_000L)
    val scrollState = rememberScrollState()
    val canvasHeight = EditorTimeline.canvasHeightDp(tracks.size).dp

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val contentWidth: Dp = maxOf(
            maxWidth,
            EditorTimeline.contentWidthDp(durationMs, 0f).dp
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.width(EditorTimeline.LABEL_COLUMN_DP.dp)) {
                Spacer(Modifier.height(EditorTimeline.RULER_HEIGHT_DP.dp))
                tracks.forEach { track ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(EditorTimeline.LANE_HEIGHT_DP.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = EditorTimeline.label(track.type),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(EditorTimeline.LANE_GAP_DP.dp))
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState)
            ) {
                Box(modifier = Modifier.width(contentWidth)) {
                    Column {
                        TimelineRuler(
                            durationMs = durationMs,
                            contentWidth = contentWidth,
                            onSeek = onSeek
                        )
                        tracks.forEach { track ->
                            TrackLane(
                                track = track,
                                project = project,
                                contentWidth = contentWidth,
                                selectedClipId = selectedClipId,
                                onSeek = onSeek,
                                onSelectClip = onSelectClip,
                                thumbnails = thumbnails
                            )
                            Spacer(Modifier.height(EditorTimeline.LANE_GAP_DP.dp))
                        }
                    }

                    // Playhead spans the ruler and every lane.
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = EditorTimeline.offsetDp(playheadMs.value).dp.roundToPx(),
                                    y = 0
                                )
                            }
                            .width(2.dp)
                            .height(canvasHeight)
                            .background(ZcutLime)
                    )
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = (EditorTimeline.offsetDp(playheadMs.value).dp - 4.dp)
                                        .roundToPx(),
                                    y = 0
                                )
                            }
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(ZcutLime)
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineRuler(
    durationMs: Long,
    contentWidth: Dp,
    onSeek: (Long) -> Unit
) {
    val tickColor = MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .width(contentWidth)
            .height(EditorTimeline.RULER_HEIGHT_DP.dp)
            .pointerInput(durationMs) {
                detectTapGestures { position ->
                    onSeek(
                        EditorTimeline.msAt(position.x.toDp().value).coerceIn(0L, durationMs)
                    )
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val secondPx = EditorTimeline.PX_PER_SECOND_DP.dp.toPx()
            val totalSeconds = (durationMs / 1_000L).toInt().coerceAtLeast(1)
            val interval = EditorTimeline.labelIntervalSeconds(durationMs)
            for (second in 0..totalSeconds) {
                val x = second * secondPx
                if (x > size.width) break
                val major = second % interval == 0
                drawLine(
                    color = tickColor,
                    start = Offset(x, if (major) 0f else size.height * 0.55f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        val interval = EditorTimeline.labelIntervalSeconds(durationMs)
        var second = 0
        while (second * 1_000L <= durationMs) {
            Text(
                text = TimeFormat.clock(second * 1_000L),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .offset(
                        x = EditorTimeline.offsetDp(second * 1_000L).dp,
                        y = 2.dp
                    )
                    .padding(start = 3.dp)
            )
            second += interval
        }
    }
}

@Composable
private fun TrackLane(
    track: Track,
    project: Project,
    contentWidth: Dp,
    selectedClipId: String?,
    onSeek: (Long) -> Unit,
    onSelectClip: (String) -> Unit,
    thumbnails: MediaThumbnails
) {
    Box(
        modifier = Modifier
            .width(contentWidth)
            .height(EditorTimeline.LANE_HEIGHT_DP.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .pointerInput(project.timeline.durationMs) {
                detectTapGestures { position ->
                    onSeek(EditorTimeline.msAt(position.x.toDp().value))
                }
            }
    ) {
        track.clips.forEach { clip ->
            val asset = clip.assetId?.let { assetId ->
                project.assets.firstOrNull { it.id == assetId }
            }
            ClipBox(
                clip = clip,
                asset = asset,
                selected = clip.id == selectedClipId,
                onClick = { onSelectClip(clip.id) },
                thumbnails = thumbnails
            )
        }
    }
}

@Composable
private fun ClipBox(
    clip: Clip,
    asset: MediaAsset?,
    selected: Boolean,
    onClick: () -> Unit,
    thumbnails: MediaThumbnails
) {
    val shape = RoundedCornerShape(6.dp)
    val accent = trackColor(clip)
    val thumbnail by rememberThumbnail(key = asset?.id) {
        if (asset == null) null else thumbnails.load(asset, 160)
    }

    Box(
        modifier = Modifier
            .offset(x = EditorTimeline.offsetDp(clip.startMs).dp)
            .width(EditorTimeline.widthDp(clip.durationMs).dp)
            .fillMaxHeight()
            .padding(vertical = 2.dp)
            .clip(shape)
            .background(accent.copy(alpha = 0.92f))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) ZcutLime else MaterialTheme.colorScheme.outline,
                shape = shape
            )
            .clickable(onClick = onClick)
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ZcutOnPrimary.copy(alpha = 0.28f))
            )
        } else if (clip.type == ClipType.AUDIO) {
            AudioBars(seed = clip.id)
        }

        Text(
            text = clipLabel(clip, asset),
            style = MaterialTheme.typography.labelSmall,
            color = if (thumbnail != null) Color.White else ZcutOnPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 5.dp)
        )
    }
}

@Composable
private fun AudioBars(seed: String) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp, vertical = 7.dp)
    ) {
        val bars = 28
        val step = size.width / bars
        for (index in 0 until bars) {
            val noise = ((seed.hashCode() * 31 + index * 17) and 0x7FFFFFFF) % 100 / 100f
            val barHeight = size.height * (0.25f + noise * 0.6f)
            val x = index * step
            drawLine(
                color = ZcutOnPrimary.copy(alpha = 0.45f),
                start = Offset(x, size.height / 2f - barHeight / 2f),
                end = Offset(x, size.height / 2f + barHeight / 2f),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

private fun clipLabel(clip: Clip, asset: MediaAsset?): String =
    asset?.displayName?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
        ?: if (clip.assetId != null) {
            "Media hilang"
        } else {
            when (clip.type) {
                ClipType.VIDEO -> "Video"
                ClipType.IMAGE -> "Foto"
                ClipType.AUDIO -> "Audio"
                ClipType.TEXT -> "Teks"
                ClipType.STICKER -> "Sticker"
                ClipType.CAPTION -> "Caption"
                ClipType.EFFECT -> "Efek"
                ClipType.SHAPE -> "Bentuk"
                ClipType.ADJUSTMENT -> "Adjust"
            }
        }

private fun trackColor(clip: Clip): Color = when (clip.type) {
    ClipType.VIDEO,
    ClipType.IMAGE -> ZcutCyan
    ClipType.AUDIO -> ZcutLime
    ClipType.TEXT -> ZcutAmber
    ClipType.STICKER -> ZcutViolet
    ClipType.CAPTION -> ZcutGreen
    ClipType.EFFECT -> ZcutCoral
    else -> ZcutCyan
}
