package com.zaaam.zcut.feature.tools

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.zcut.core.domain.StickerCatalog
import com.zaaam.zcut.core.domain.StickerCategory
import com.zaaam.zcut.core.domain.StickerEditor
import com.zaaam.zcut.core.domain.StickerSpec
import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.model.Transform
import com.zaaam.zcut.core.model.coverAsset
import com.zaaam.zcut.core.util.TimeFormat
import com.zaaam.zcut.feature.media.rememberThumbnail
import com.zaaam.zcut.ui.LocalAppContainer
import com.zaaam.zcut.ui.components.EmptyState
import com.zaaam.zcut.ui.components.ZcutIconButton
import com.zaaam.zcut.ui.components.ZcutPrimaryButton
import com.zaaam.zcut.ui.components.ZcutScaffold
import com.zaaam.zcut.ui.components.ZcutSecondaryButton
import com.zaaam.zcut.ui.components.ZcutTopBar
import com.zaaam.zcut.ui.stickerViewModelFactory
import com.zaaam.zcut.ui.theme.ZcutLime
import com.zaaam.zcut.ui.theme.ZcutSpacing
import kotlinx.coroutines.delay

@Composable
fun StickerScreen(
    projectId: String,
    clipId: String?,
    atMs: Long,
    onBack: () -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: StickerViewModel = viewModel(
        key = "sticker:$projectId:${clipId.orEmpty()}",
        factory = stickerViewModelFactory(container, projectId, clipId)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ZcutScaffold(
        topBar = {
            ZcutTopBar(
                title = "Stickers",
                subtitle = state.project?.let { "${it.name} · stiker lokal" },
                navigationIcon = {
                    ZcutIconButton(
                        icon = Icons.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        onClick = onBack
                    )
                }
            )
        }
    ) { insets ->
        val modifier = Modifier
            .fillMaxSize()
            .padding(insets)
        when {
            state.isLoading -> Box(modifier, contentAlignment = Alignment.Center) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.5f))
            }

            state.error != null -> Box(modifier, contentAlignment = Alignment.Center) {
                EmptyState(
                    title = "Gagal memuat draft",
                    message = state.error!!,
                    actionLabel = "Kembali",
                    onAction = onBack
                )
            }

            state.project == null -> Box(modifier, contentAlignment = Alignment.Center) {
                EmptyState(
                    title = "Draft tidak ditemukan",
                    message = "Draft ini mungkin sudah dihapus dari perangkat.",
                    actionLabel = "Kembali",
                    onAction = onBack
                )
            }

            else -> StickerBody(
                project = state.project!!,
                clip = state.clip,
                atMs = atMs,
                onPersist = { stickerId, transform, opacity ->
                    viewModel.save(stickerId, transform, opacity, atMs)
                },
                onCommit = { stickerId, transform, opacity ->
                    viewModel.save(stickerId, transform, opacity, atMs) { onBack() }
                },
                onDelete = { viewModel.delete { onBack() } },
                modifier = modifier
            )
        }
    }
}

@Composable
private fun StickerBody(
    project: Project,
    clip: Clip?,
    atMs: Long,
    onPersist: (String, Transform, Float) -> Unit,
    onCommit: (String, Transform, Float) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier
) {
    val initialId = remember(clip?.id) {
        StickerCatalog.canonical(clip?.sticker?.stickerId.orEmpty()).id
    }
    val initialTransform = remember(clip?.id) { clip?.transform ?: Transform() }
    var stickerId by remember(clip?.id) { mutableStateOf(initialId) }
    var transform by remember(clip?.id) { mutableStateOf(initialTransform) }
    var opacity by remember(clip?.id) { mutableFloatStateOf(clip?.opacity ?: 1f) }
    var category by remember { mutableStateOf(StickerCategory.TRENDING) }

    val latestId by rememberUpdatedState(stickerId)
    val latestTransform by rememberUpdatedState(transform)
    val latestOpacity by rememberUpdatedState(opacity)
    // Editing an existing clip persists after a short idle; the draft is seeded
    // per clip id so a repository echo cannot overwrite the current gesture.
    LaunchedEffect(clip?.id, stickerId, transform, opacity) {
        val current = clip ?: return@LaunchedEffect
        if (stickerId == initialId &&
            transform == current.transform &&
            opacity == current.opacity
        ) {
            return@LaunchedEffect
        }
        delay(400)
        onPersist(latestId, latestTransform, latestOpacity)
    }

    val stickerCount = remember(project.timeline) {
        StickerEditor.stickerClips(project.timeline).size
    }
    val spec = StickerCatalog.canonical(stickerId)

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ZcutSpacing.screen, vertical = ZcutSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
    ) {
        StickerPreviewCard(
            project = project,
            glyph = spec.glyph,
            transform = transform,
            opacity = opacity,
            count = stickerCount
        )

        ToolSection(title = "Kategori") {
            ToolChipRow {
                StickerCatalog.categories.forEach { option ->
                    ToolChip(
                        label = categoryLabel(option),
                        selected = category == option,
                        onClick = { category = option }
                    )
                }
            }
        }

        ToolSection(title = "Stiker · ${StickerCatalog.byCategory(category).size}") {
            StickerGrid(
                specs = StickerCatalog.byCategory(category),
                selectedId = spec.id,
                onPick = { picked -> stickerId = picked }
            )
            ToolHint("Semua stiker digambar sendiri dari glyph; tidak ada aset CapCut.")
        }

        ToolSection(title = "Transform") {
            ToolSlider(
                label = "Size",
                value = transform.scale,
                valueRange = 0.1f..4f,
                onValueChange = { transform = transform.copy(scale = it) },
                onValueChangeFinished = {},
                valueLabel = { "${(it * 100).toInt()}%" }
            )
            ToolSlider(
                label = "Rotasi",
                value = transform.rotationDegrees,
                valueRange = -180f..180f,
                onValueChange = { transform = transform.copy(rotationDegrees = it) },
                onValueChangeFinished = {},
                valueLabel = { "${it.toInt()}°" }
            )
            ToolSlider(
                label = "Posisi X",
                value = transform.positionX,
                valueRange = -100f..100f,
                onValueChange = { transform = transform.copy(positionX = it) },
                onValueChangeFinished = {},
                valueLabel = { "${it.toInt()}%" }
            )
            ToolSlider(
                label = "Posisi Y",
                value = transform.positionY,
                valueRange = -100f..100f,
                onValueChange = { transform = transform.copy(positionY = it) },
                onValueChangeFinished = {},
                valueLabel = { "${it.toInt()}%" }
            )
            ToolSlider(
                label = "Opacity",
                value = opacity,
                valueRange = 0f..1f,
                onValueChange = { opacity = it },
                onValueChangeFinished = {},
                valueLabel = { "${(it * 100).toInt()}%" }
            )
            ToolChipRow {
                ToolChip(
                    label = "Flip H",
                    selected = transform.flipHorizontal,
                    onClick = { transform = transform.copy(flipHorizontal = !transform.flipHorizontal) }
                )
                ToolChip(
                    label = "Flip V",
                    selected = transform.flipVertical,
                    onClick = { transform = transform.copy(flipVertical = !transform.flipVertical) }
                )
            }
        }

        ToolSection(title = "Track subject") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Ikuti objek",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(checked = false, enabled = false, onCheckedChange = null)
            }
            ToolHint(
                "Tracking objek lanjutan BUTUH BACKEND (analisis per frame). " +
                    "Fallback lokal: pasang keyframe pada Posisi X/Y di panel Keyframe."
            )
        }

        ToolSection(title = "Penempatan") {
            ToolValueRow("Mulai", TimeFormat.clockMillis(atMs.coerceAtLeast(0L)))
            ToolValueRow(
                "Durasi",
                TimeFormat.clockMillis(clip?.durationMs ?: StickerEditor.DEFAULT_DURATION_MS)
            )
            ToolValueRow("Jumlah stiker di draft", stickerCount.toString())
        }

        if (clip == null) {
            ZcutPrimaryButton(
                text = "Tambah stiker",
                onClick = { onCommit(stickerId, transform, opacity) },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            ZcutPrimaryButton(
                text = "Terapkan",
                onClick = { onCommit(stickerId, transform, opacity) },
                modifier = Modifier.fillMaxWidth()
            )
            ZcutSecondaryButton(
                text = "Hapus stiker",
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth()
            )
        }

        ToolHint(
            "Stiker tampil di preview Compose. Seperti teks, stiker belum dibakar " +
                "ke file hasil export (batasan render M13)."
        )
        Spacer(Modifier.height(ZcutSpacing.xl))
    }
}

@Composable
private fun StickerPreviewCard(
    project: Project,
    glyph: String,
    transform: Transform,
    opacity: Float,
    count: Int
) {
    val thumbnails = LocalAppContainer.current.mediaThumbnails
    val cover = remember(project.id, project.assets, project.timeline) {
        project.coverAsset()
    }
    val thumbnail by rememberThumbnail(key = cover?.id) {
        if (cover == null) null else thumbnails.load(cover, 720)
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(184.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail!!,
                contentDescription = "Pratinjau stiker",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
            )
        }
        val dx = maxWidth * (transform.positionX / 100f)
        val dy = maxHeight * (transform.positionY / 100f)
        Text(
            text = glyph,
            fontSize = (40f * transform.scale).coerceIn(12f, 220f).sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = dx, y = dy)
                .graphicsLayer {
                    rotationZ = transform.rotationDegrees
                    scaleX = if (transform.flipHorizontal) -1f else 1f
                    scaleY = if (transform.flipVertical) -1f else 1f
                }
                .alpha(opacity.coerceIn(0f, 1f))
        )
        Surface(
            color = Color.Black.copy(alpha = 0.55f),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        ) {
            Text(
                text = "$count STICKER",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun StickerGrid(
    specs: List<StickerSpec>,
    selectedId: String,
    onPick: (String) -> Unit
) {
    // A plain column of rows rather than a lazy grid: the whole panel already
    // scrolls vertically and a nested scrollable would fight it.
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        specs.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { spec ->
                    StickerTile(
                        glyph = spec.glyph,
                        label = spec.label,
                        selected = spec.id == selectedId,
                        onClick = { onPick(spec.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Keep the last row's tiles the same width as a full row.
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun StickerTile(
    glyph: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(shape)
            .background(
                if (selected) ZcutLime.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) ZcutLime else MaterialTheme.colorScheme.outline,
                shape = shape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = glyph,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun categoryLabel(category: StickerCategory): String = when (category) {
    StickerCategory.TRENDING -> "Trending"
    StickerCategory.EMOJI -> "Emoji"
    StickerCategory.TEXT -> "Text"
    StickerCategory.SHAPES -> "Shapes"
    StickerCategory.GIF -> "GIF"
}