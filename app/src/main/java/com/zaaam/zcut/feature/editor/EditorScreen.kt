package com.zaaam.zcut.feature.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.zcut.core.domain.ExportDimensions
import com.zaaam.zcut.core.media.MediaThumbnails
import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.util.TimeFormat
import com.zaaam.zcut.feature.media.rememberThumbnail
import com.zaaam.zcut.ui.LocalAppContainer
import com.zaaam.zcut.ui.components.EmptyState
import com.zaaam.zcut.ui.components.ZcutIconButton
import com.zaaam.zcut.ui.components.ZcutPrimaryButton
import com.zaaam.zcut.ui.components.ZcutScaffold
import com.zaaam.zcut.ui.components.ZcutTopBar
import com.zaaam.zcut.ui.editorViewModelFactory
import com.zaaam.zcut.ui.navigation.ZcutRoutes
import com.zaaam.zcut.ui.theme.ZcutSpacing

@Composable
fun EditorScreen(
    projectId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    onAddMedia: () -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: EditorViewModel = viewModel(
        key = "editor:$projectId",
        factory = editorViewModelFactory(container, projectId)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    EditorContent(
        state = state,
        thumbnails = container.mediaThumbnails,
        onBack = onBack,
        onNavigate = onNavigate,
        onAddMedia = onAddMedia,
        onSeek = viewModel::seekTo,
        onSelectClip = viewModel::selectClip
    )
}

@Composable
private fun EditorContent(
    state: EditorUiState,
    thumbnails: MediaThumbnails,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    onAddMedia: () -> Unit,
    onSeek: (Long) -> Unit,
    onSelectClip: (String?) -> Unit
) {
    val project = state.project

    ZcutScaffold(
        topBar = {
            ZcutTopBar(
                title = project?.name ?: "Editor",
                subtitle = project?.let(::projectMeta),
                navigationIcon = {
                    ZcutIconButton(
                        icon = Icons.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        onClick = onBack
                    )
                },
                actions = {
                    TextButton(
                        onClick = { onNavigate(ZcutRoutes.Export) },
                        enabled = project != null
                    ) {
                        Text("Export")
                    }
                }
            )
        }
    ) { insets ->
        when {
            state.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(insets),
                contentAlignment = Alignment.Center
            ) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.5f))
            }

            state.notFound -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(insets),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    title = "Draft tidak ditemukan",
                    message = "Draft ini mungkin sudah dihapus dari perangkat.",
                    actionLabel = "Kembali",
                    onAction = onBack
                )
            }

            project == null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(insets),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    title = "Gagal memuat draft",
                    message = state.error ?: "Terjadi kesalahan saat membaca dokumen draft.",
                    actionLabel = "Kembali",
                    onAction = onBack
                )
            }

            else -> EditorBody(
                project = project,
                playheadMs = state.playheadMs,
                selectedClipId = state.selectedClipId,
                isEmpty = state.isEmpty,
                thumbnails = thumbnails,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(insets),
                onNavigate = onNavigate,
                onAddMedia = onAddMedia,
                onSeek = onSeek,
                onSelectClip = { clipId -> onSelectClip(clipId) }
            )
        }
    }
}

@Composable
private fun EditorBody(
    project: Project,
    playheadMs: Long,
    selectedClipId: String?,
    isEmpty: Boolean,
    thumbnails: MediaThumbnails,
    modifier: Modifier,
    onNavigate: (String) -> Unit,
    onAddMedia: () -> Unit,
    onSeek: (Long) -> Unit,
    onSelectClip: (String) -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        // The mockup caps the preview at 42% of the body; the rest belongs to
        // the timeline. On a short screen the whole column scrolls instead of
        // squeezing the preview to zero.
        val previewHeight = (maxHeight * 0.42f).coerceAtLeast(120.dp)
        val timelineHeight = EditorTimeline.canvasHeightDp(project.timeline.tracks.size).dp
        val fixedEstimate = previewHeight + timelineHeight + 150.dp
        val needsScroll = fixedEstimate > maxHeight
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (needsScroll) Modifier.verticalScroll(scrollState) else Modifier
                )
        ) {
            PreviewPanel(
                project = project,
                playheadMs = playheadMs,
                isEmpty = isEmpty,
                thumbnails = thumbnails,
                onAddMedia = onAddMedia,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(previewHeight)
                    .padding(horizontal = ZcutSpacing.screen, vertical = ZcutSpacing.sm)
            )

            ToolStrip(onNavigate = onNavigate)

            TimelineCanvas(
                project = project,
                playheadMs = playheadMs,
                selectedClipId = selectedClipId,
                onSeek = onSeek,
                onSelectClip = onSelectClip,
                thumbnails = thumbnails,
                modifier = Modifier.padding(top = ZcutSpacing.sm)
            )

            if (needsScroll) {
                Spacer(Modifier.height(ZcutSpacing.sm))
            } else {
                Spacer(Modifier.weight(1f))
            }

            TimelineFooter(onNavigate = onNavigate, onAddMedia = onAddMedia)
        }
    }
}

@Composable
private fun PreviewPanel(
    project: Project,
    playheadMs: Long,
    isEmpty: Boolean,
    thumbnails: MediaThumbnails,
    onAddMedia: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cover = remember(project.id, project.assets, project.timeline) {
        coverAsset(project)
    }
    val thumbnail by rememberThumbnail(key = cover?.id) {
        if (cover == null) null else thumbnails.load(cover, 720)
    }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val ratio = project.aspectRatio.ratio
        val frameWidth = minOf(maxWidth, maxHeight * ratio)
        val frameHeight = frameWidth / ratio

        Box(
            modifier = Modifier
                .size(width = frameWidth, height = frameHeight)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail!!,
                    contentDescription = "Pratinjau ${project.name}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(ZcutSpacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isEmpty) "Belum ada media" else "Pratinjau belum tersedia",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isEmpty) {
                        Text(
                            text = "Tambahkan video, foto, atau audio untuk mulai.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = ZcutSpacing.xs)
                        )
                        ZcutPrimaryButton(
                            text = "Tambah media",
                            onClick = onAddMedia,
                            modifier = Modifier.padding(top = ZcutSpacing.md)
                        )
                    }
                }
            }

            // Scrim keeps the timestamp readable over any frame.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.62f))
                        )
                    )
            )

            Surface(
                color = Color.Black.copy(alpha = 0.55f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(ZcutSpacing.sm)
            ) {
                Text(
                    text = "${project.aspectRatio.label} · ${TimeFormat.clock(project.timeline.durationMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }

            Surface(
                color = Color.Black.copy(alpha = 0.38f),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.padding(12.dp)
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = ZcutSpacing.md, vertical = ZcutSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = TimeFormat.clock(playheadMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
                Text(
                    text = " · pemutar aktif di M3",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ToolStrip(onNavigate: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = ZcutSpacing.screen),
        horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
    ) {
        EditorTools.forEach { tool ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onNavigate(tool.route) }
            ) {
                Text(
                    text = tool.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                )
            }
        }
    }
}

@Composable
private fun TimelineFooter(onNavigate: (String) -> Unit, onAddMedia: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = ZcutSpacing.screen, vertical = ZcutSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
    ) {
        FooterChip(label = "+ Media", primary = true, onClick = onAddMedia)
        FooterChip(label = "Trim", onClick = { onNavigate(ZcutRoutes.Trim) })
        FooterChip(label = "Speed", onClick = { onNavigate(ZcutRoutes.Speed) })
        FooterChip(label = "Volume", onClick = { onNavigate(ZcutRoutes.Audio) })
    }
}

@Composable
private fun FooterChip(
    label: String,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = if (primary) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (primary) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        )
    }
}

private fun projectMeta(project: Project): String {
    val (width, height) = ExportDimensions.forProject(project)
    return "$width × $height · ${project.frameRate} fps · ${TimeFormat.clock(project.timeline.durationMs)}"
}

private fun coverAsset(project: Project): MediaAsset? {
    val byId = project.assets.associateBy { it.id }
    project.coverAssetId?.let { byId[it] }?.let { return it }
    return project.timeline.tracks
        .asSequence()
        .flatMap { it.clips.asSequence() }
        .sortedBy { it.startMs }
        .mapNotNull { clip -> clip.assetId?.let(byId::get) }
        .firstOrNull { it.isVisual }
}

private data class EditorTool(val label: String, val route: String)

private val EditorTools = listOf(
    EditorTool("Edit", ZcutRoutes.Trim),
    EditorTool("Audio", ZcutRoutes.Audio),
    EditorTool("Teks", ZcutRoutes.Text),
    EditorTool("Sticker", ZcutRoutes.Stickers),
    EditorTool("Efek", ZcutRoutes.Effects),
    EditorTool("Filter", ZcutRoutes.Filters),
    EditorTool("Adjust", ZcutRoutes.Adjust),
    EditorTool("Mask", ZcutRoutes.Mask)
)
