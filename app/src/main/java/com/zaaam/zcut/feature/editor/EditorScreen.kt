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
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.zaaam.zcut.core.domain.ExportDimensions
import com.zaaam.zcut.core.domain.TextEditor
import com.zaaam.zcut.core.media.MediaThumbnails
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.model.coverAsset
import com.zaaam.zcut.core.render.PreviewState
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
import kotlinx.coroutines.flow.StateFlow

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

    // The ViewModel survives on the back stack, so leaving the editor (or
    // backgrounding the app) must stop playback explicitly; onCleared is too
    // late and would keep audio and the decoder running behind another screen.
    // Preview rebuilds are deferred while stopped and flushed on ON_START.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onScreenStarted()
                Lifecycle.Event.ON_STOP -> viewModel.onScreenStopped()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            viewModel.onScreenStarted()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onScreenStopped()
        }
    }

    EditorContent(
        state = state,
        previewState = viewModel.previewState,
        playhead = viewModel.playhead,
        previewPlayer = viewModel.previewPlayer,
        thumbnails = container.mediaThumbnails,
        onBack = onBack,
        onNavigate = onNavigate,
        onAddMedia = onAddMedia,
        onSeek = viewModel::seekTo,
        onSelectClip = viewModel::selectClip,
        onSelectClipOnly = viewModel::selectClipOnly,
        onTogglePlay = viewModel::togglePlayback,
        onToggleMute = viewModel::toggleMute,
        onSplit = viewModel::splitAtPlayhead,
        onDelete = { viewModel.deleteSelected(ripple = false) },
        onDuplicate = viewModel::duplicateSelected,
        onMoveClip = viewModel::moveClip,
        onTrimClip = viewModel::trimClip
    )
}

@Composable
private fun EditorContent(
    state: EditorUiState,
    previewState: StateFlow<PreviewState>,
    playhead: StateFlow<Long>,
    previewPlayer: StateFlow<Player?>,
    thumbnails: MediaThumbnails,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    onAddMedia: () -> Unit,
    onSeek: (Long) -> Unit,
    onSelectClip: (String?) -> Unit,
    onSelectClipOnly: (String) -> Unit,
    onTogglePlay: () -> Unit,
    onToggleMute: () -> Unit,
    onSplit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveClip: (String, String, Long) -> Unit,
    onTrimClip: (String, Long, Long) -> Unit
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
                previewState = previewState,
                playhead = playhead,
                previewPlayer = previewPlayer,
                selectedClipId = state.selectedClipId,
                isEmpty = state.isEmpty,
                thumbnails = thumbnails,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(insets),
                onNavigate = onNavigate,
                onAddMedia = onAddMedia,
                onSeek = onSeek,
                onSelectClip = { clipId -> onSelectClip(clipId) },
                onSelectClipOnly = onSelectClipOnly,
                onTogglePlay = onTogglePlay,
                onToggleMute = onToggleMute,
                onSplit = onSplit,
                onDelete = onDelete,
                onDuplicate = onDuplicate,
                onMoveClip = onMoveClip,
                onTrimClip = onTrimClip
            )
        }
    }
}

@Composable
private fun EditorBody(
    project: Project,
    previewState: StateFlow<PreviewState>,
    playhead: StateFlow<Long>,
    previewPlayer: StateFlow<Player?>,
    selectedClipId: String?,
    isEmpty: Boolean,
    thumbnails: MediaThumbnails,
    modifier: Modifier,
    onNavigate: (String) -> Unit,
    onAddMedia: () -> Unit,
    onSeek: (Long) -> Unit,
    onSelectClip: (String) -> Unit,
    onSelectClipOnly: (String) -> Unit,
    onTogglePlay: () -> Unit,
    onToggleMute: () -> Unit,
    onSplit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveClip: (String, String, Long) -> Unit,
    onTrimClip: (String, Long, Long) -> Unit
) {
    val openTool: (String) -> Unit = { route ->
        val target = if (route == ZcutRoutes.Text) {
            ZcutRoutes.text(project.id, selectedClipId, playhead.value)
        } else {
            ZcutRoutes.toolRoute(route, project.id, selectedClipId)
        }
        onNavigate(target)
    }
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
                previewState = previewState,
                previewPlayer = previewPlayer,
                playhead = playhead,
                isEmpty = isEmpty,
                thumbnails = thumbnails,
                onAddMedia = onAddMedia,
                onTogglePlay = onTogglePlay,
                onToggleMute = onToggleMute,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(previewHeight)
                    .padding(horizontal = ZcutSpacing.screen, vertical = ZcutSpacing.sm)
            )

            ToolStrip(onOpenTool = openTool)

            TimelineCanvas(
                project = project,
                playhead = playhead,
                selectedClipId = selectedClipId,
                onSeek = onSeek,
                onSelectClip = onSelectClip,
                onSelectClipOnly = onSelectClipOnly,
                onMoveClip = onMoveClip,
                onTrimClip = onTrimClip,
                thumbnails = thumbnails,
                modifier = Modifier.padding(top = ZcutSpacing.sm)
            )

            if (needsScroll) {
                Spacer(Modifier.height(ZcutSpacing.sm))
            } else {
                Spacer(Modifier.weight(1f))
            }

            TimelineFooter(
                selectedClipId = selectedClipId,
                onOpenTool = openTool,
                onAddMedia = onAddMedia,
                onSplit = onSplit,
                onDelete = onDelete,
                onDuplicate = onDuplicate
            )
        }
    }
}

@Composable
private fun PreviewPanel(
    project: Project,
    previewState: StateFlow<PreviewState>,
    previewPlayer: StateFlow<Player?>,
    playhead: StateFlow<Long>,
    isEmpty: Boolean,
    thumbnails: MediaThumbnails,
    onAddMedia: () -> Unit,
    onTogglePlay: () -> Unit,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Collected here, not at the screen root: the 1 Hz FPS update and
    // play/pause changes must not recompose the timeline and its clips.
    val preview by previewState.collectAsStateWithLifecycle()
    val player by previewPlayer.collectAsStateWithLifecycle()
    val cover = remember(project.id, project.assets, project.timeline) {
        coverAsset(project)
    }
    val thumbnail by rememberThumbnail(key = cover?.id) {
        if (cover == null) null else thumbnails.load(cover, 720)
    }
    val hasContent = project.timeline.tracks.any { it.clips.isNotEmpty() }
    val hasText = TextEditor.hasText(project.timeline)
    val showPlayer = preview.hasVideo && player != null

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
            if (showPlayer) {
                AndroidView(
                    factory = { context ->
                        PlayerView(context).apply {
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                            setKeepContentOnPlayerReset(true)
                        }
                    },
                    update = { view -> view.player = player },
                    onRelease = { view -> view.player = null },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // The player surface is black until the first frame is decoded, so
            // the cover thumbnail stays on top for that moment.
            if (!showPlayer || !preview.hasFrame) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail!!,
                        contentDescription = "Pratinjau ${project.name}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (!isEmpty && !hasText) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(ZcutSpacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (preview.hasAudio) {
                                "Pratinjau audio"
                            } else {
                                "Pratinjau belum tersedia"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (preview.hasAudio) {
                            Text(
                                text = "Tekan putar untuk mendengarkan audio.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = ZcutSpacing.xs)
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(ZcutSpacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Belum ada media",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
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

            // Text is composited here, not by Media3; the overlay follows the
            // playhead so scrub and playback show the same animation.
            TextOverlayLayer(
                project = project,
                playhead = playhead,
                modifier = Modifier.fillMaxSize()
            )

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

            preview.error?.let { message ->
                Surface(
                    color = Color.Black.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(ZcutSpacing.sm)
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            if (hasContent) {
                val playEnabled = preview.isPlayable && player != null && preview.error == null
                Surface(
                    color = Color.Black.copy(alpha = 0.38f),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(52.dp)
                        .clip(CircleShape)
                        .clickable(enabled = playEnabled, onClick = onTogglePlay)
                ) {
                    Icon(
                        imageVector = if (preview.isPlaying) {
                            ZcutIcons.Pause
                        } else {
                            Icons.Filled.PlayArrow
                        },
                        contentDescription = if (preview.isPlaying) "Jeda" else "Putar",
                        tint = Color.White.copy(alpha = if (playEnabled) 0.92f else 0.45f),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = ZcutSpacing.md, vertical = ZcutSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PreviewClock(playhead)
                if (preview.isPlaying && preview.renderedFps > 0) {
                    Text(
                        text = " · ${preview.renderedFps} fps",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
                Spacer(Modifier.weight(1f))
                if (preview.hasAudio) {
                    IconButton(
                        onClick = onToggleMute,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (preview.isMuted) {
                                ZcutIcons.VolumeOff
                            } else {
                                ZcutIcons.VolumeOn
                            },
                            contentDescription = if (preview.isMuted) {
                                "Nyalakan suara"
                            } else {
                                "Bisukan suara"
                            },
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewClock(playhead: StateFlow<Long>) {
    val position by playhead.collectAsStateWithLifecycle()
    Text(
        text = TimeFormat.clock(position),
        style = MaterialTheme.typography.labelMedium,
        color = Color.White
    )
}

@Composable
private fun ToolStrip(onOpenTool: (String) -> Unit) {
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
                    .clickable { onOpenTool(tool.route) }
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
private fun TimelineFooter(
    selectedClipId: String?,
    onOpenTool: (String) -> Unit,
    onAddMedia: () -> Unit,
    onSplit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = ZcutSpacing.screen, vertical = ZcutSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
    ) {
        FooterChip(label = "+ Media", primary = true, onClick = onAddMedia)
        if (selectedClipId != null) {
            FooterChip(label = "Split", onClick = onSplit)
            FooterChip(label = "Duplicate", onClick = onDuplicate)
            FooterChip(label = "Hapus", onClick = onDelete)
        }
        FooterChip(label = "Trim", onClick = { onOpenTool(ZcutRoutes.Trim) })
        FooterChip(label = "Speed", onClick = { onOpenTool(ZcutRoutes.Speed) })
        FooterChip(label = "Volume", onClick = { onOpenTool(ZcutRoutes.Audio) })
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

private data class EditorTool(val label: String, val route: String)

private val EditorTools = listOf(
    EditorTool("Edit", ZcutRoutes.Trim),
    EditorTool("Audio", ZcutRoutes.Audio),
    EditorTool("Teks", ZcutRoutes.Text),
    EditorTool("Sticker", ZcutRoutes.Stickers),
    EditorTool("Efek", ZcutRoutes.Effects),
    EditorTool("Filter", ZcutRoutes.Filters),
    EditorTool("Adjust", ZcutRoutes.Adjust),
    EditorTool("Mask", ZcutRoutes.Mask),
    EditorTool("Keyframe", ZcutRoutes.Keyframe),
    EditorTool("Transisi", ZcutRoutes.Transition)
)
