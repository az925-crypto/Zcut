package com.zaaam.zcut.feature.tools

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.zcut.core.domain.TextAnimationFrame
import com.zaaam.zcut.core.domain.TextAnimator
import com.zaaam.zcut.core.domain.TextEditor
import com.zaaam.zcut.core.domain.TextFonts
import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.model.TextAlignment
import com.zaaam.zcut.core.model.TextAnimation
import com.zaaam.zcut.core.model.TextConfig
import com.zaaam.zcut.core.model.TextStylePreset
import com.zaaam.zcut.core.model.coverAsset
import com.zaaam.zcut.core.util.TimeFormat
import com.zaaam.zcut.feature.media.rememberThumbnail
import com.zaaam.zcut.ui.LocalAppContainer
import com.zaaam.zcut.ui.components.EmptyState
import com.zaaam.zcut.ui.components.ZcutIconButton
import com.zaaam.zcut.ui.components.ZcutPrimaryButton
import com.zaaam.zcut.ui.components.ZcutScaffold
import com.zaaam.zcut.ui.components.ZcutSecondaryButton
import com.zaaam.zcut.ui.components.ZcutText
import com.zaaam.zcut.ui.components.ZcutTopBar
import com.zaaam.zcut.ui.textViewModelFactory
import com.zaaam.zcut.ui.theme.ZcutLime
import com.zaaam.zcut.ui.theme.ZcutSpacing
import kotlinx.coroutines.delay

@Composable
fun TextScreen(
    projectId: String,
    clipId: String?,
    atMs: Long,
    onBack: () -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: TextViewModel = viewModel(
        key = "text:$projectId:${clipId.orEmpty()}",
        factory = textViewModelFactory(container, projectId, clipId)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ZcutScaffold(
        topBar = {
            ZcutTopBar(
                title = "Text",
                subtitle = state.project?.let { "${it.name} · teks lokal" },
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

            else -> TextBody(
                project = state.project!!,
                clip = state.clip,
                atMs = atMs,
                onPersist = { config, opacity ->
                    viewModel.save(config, opacity, atMs)
                },
                onCommit = { config, opacity ->
                    viewModel.save(config, opacity, atMs) { onBack() }
                },
                onDelete = { viewModel.delete { onBack() } },
                modifier = modifier
            )
        }
    }
}

@Composable
private fun TextBody(
    project: Project,
    clip: Clip?,
    atMs: Long,
    onPersist: (TextConfig, Float) -> Unit,
    onCommit: (TextConfig, Float) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier
) {
    val initial = remember(clip?.id) { clip?.text ?: defaultDraft() }
    var draft by remember(clip?.id) { mutableStateOf(initial) }
    var opacity by remember(clip?.id) { mutableFloatStateOf(clip?.opacity ?: 1f) }

    val latestDraft by rememberUpdatedState(draft)
    val latestOpacity by rememberUpdatedState(opacity)
    // Editing an existing clip persists after a short idle; the draft is seeded
    // per clip id so a repository echo cannot overwrite what is being typed.
    LaunchedEffect(clip?.id, draft, opacity) {
        val current = clip ?: return@LaunchedEffect
        // Opening an existing clip must not write it back unchanged.
        if (draft == current.text && opacity == current.opacity) return@LaunchedEffect
        delay(400)
        if (latestDraft.text.isNotBlank()) {
            onPersist(latestDraft, latestOpacity)
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ZcutSpacing.screen, vertical = ZcutSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
    ) {
        TextPreviewCard(project = project, config = draft)

        OutlinedTextField(
            value = draft.text,
            onValueChange = { value ->
                draft = draft.copy(text = value.take(TextEditor.MAX_TEXT_LENGTH))
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Isi teks") },
            minLines = 2,
            maxLines = 4,
            shape = MaterialTheme.shapes.small
        )
        Text(
            text = "${draft.text.length} / ${TextEditor.MAX_TEXT_LENGTH} karakter",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )

        ToolSection(title = "Font") {
            ToolChipRow {
                TextFonts.systemIds.forEach { id ->
                    ToolChip(
                        label = TextFonts.label(id),
                        selected = TextFonts.canonical(draft.fontFamily) == id,
                        onClick = { draft = draft.copy(fontFamily = id) }
                    )
                }
                ToolChip(
                    label = "Import",
                    selected = false,
                    enabled = false,
                    onClick = {}
                )
            }
            ToolHint("Import font dari perangkat menyusul. M11 memakai font sistem.")
        }

        ToolSection(title = "Style") {
            ToolChipRow {
                StylePresets.forEach { preset ->
                    val selected = draft.stylePreset == preset
                    ToolChip(
                        label = styleLabel(preset),
                        selected = selected,
                        onClick = {
                            draft = draft.copy(
                                stylePreset = if (selected) TextStylePreset.NONE else preset
                            )
                        }
                    )
                }
            }
        }

        ToolSection(title = "Perataan") {
            ToolChipRow {
                TextAlignment.entries.forEach { alignment ->
                    ToolChip(
                        label = alignmentLabel(alignment),
                        selected = draft.alignment == alignment,
                        onClick = { draft = draft.copy(alignment = alignment) }
                    )
                }
            }
        }

        ToolSection(title = "Warna") {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                TextColors.forEach { argb ->
                    ColorSwatch(
                        argb = argb,
                        selected = draft.colorArgb == argb,
                        onClick = { draft = draft.copy(colorArgb = argb) }
                    )
                }
            }
        }

        ToolSection(title = "Ukuran & spasi") {
            ToolSlider(
                label = "Size",
                value = draft.fontSizeSp,
                valueRange = 12f..96f,
                onValueChange = { draft = draft.copy(fontSizeSp = it) },
                onValueChangeFinished = {},
                valueLabel = { "${it.toInt()} pt" }
            )
            ToolSlider(
                label = "Spacing",
                value = draft.letterSpacingEm * 100f,
                valueRange = 0f..20f,
                onValueChange = { draft = draft.copy(letterSpacingEm = it / 100f) },
                onValueChangeFinished = {},
                valueLabel = { "${it.toInt()}" }
            )
            ToolSlider(
                label = "Opacity",
                value = opacity,
                valueRange = 0f..1f,
                onValueChange = { opacity = it },
                onValueChangeFinished = {},
                valueLabel = { "${(it * 100).toInt()}%" }
            )
        }

        ToolSection(title = "Animasi") {
            ToolChipRow {
                TextAnimation.entries.forEach { animation ->
                    ToolChip(
                        label = animationLabel(animation),
                        selected = draft.animation == animation,
                        onClick = { draft = draft.copy(animation = animation) }
                    )
                }
            }
        }

        ToolSection(title = "Penempatan") {
            ToolValueRow("Mulai", TimeFormat.clockMillis(atMs.coerceAtLeast(0L)))
            ToolValueRow(
                "Durasi",
                TimeFormat.clockMillis(clip?.durationMs ?: TextEditor.DEFAULT_DURATION_MS)
            )
            ToolHint(
                "Hanya ada satu track TEXT. Bila waktunya bentrok, teks baru " +
                    "digeser ke slot bebas berikutnya. Durasi bisa diubah dari " +
                    "timeline setelah teks ditambahkan."
            )
        }

        if (draft.text.isBlank()) {
            ToolHint("Isi teks dulu untuk menyimpan.")
        }
        if (clip == null) {
            ZcutPrimaryButton(
                text = "Tambah teks",
                onClick = { onCommit(draft, opacity) },
                enabled = draft.text.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            ZcutPrimaryButton(
                text = "Terapkan",
                onClick = { onCommit(draft, opacity) },
                enabled = draft.text.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
            ZcutSecondaryButton(
                text = "Hapus teks",
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth()
            )
        }

        ToolHint(
            "Teks tampil di preview Compose dan ikut ter-render saat export " +
                "(M13). Template multi-layer dan AI font BUTUH BACKEND."
        )
        Spacer(Modifier.height(ZcutSpacing.xl))
    }
}

@Composable
private fun TextPreviewCard(project: Project, config: TextConfig) {
    val thumbnails = LocalAppContainer.current.mediaThumbnails
    val cover = remember(project.id, project.assets, project.timeline) {
        project.coverAsset()
    }
    val thumbnail by rememberThumbnail(key = cover?.id) {
        if (cover == null) null else thumbnails.load(cover, 720)
    }
    val transition = rememberInfiniteTransition(label = "text-preview")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "text-preview-progress"
    )
    // Reading `progress` only for animated presets keeps a static preview from
    // recomposing at 60 fps.
    val frame = if (config.animation == TextAnimation.NONE) {
        TextAnimationFrame.Idle
    } else {
        TextAnimator.frame(
            animation = config.animation,
            elapsedMs = (progress * 1_200f).toLong(),
            durationMs = 1_800L,
            textLength = config.text.length,
            seed = config.text.hashCode()
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail!!,
                contentDescription = "Pratinjau teks",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
            )
        }
        if (config.text.isBlank()) {
            Text(
                text = "Pratinjau teks",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            ZcutText(
                config = config,
                frameHeightDp = 176f,
                frame = frame
            )
        }
        Surface(
            color = Color.Black.copy(alpha = 0.55f),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        ) {
            Text(
                text = "JUDUL · ${config.fontSizeSp.toInt()} PT",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
internal fun ColorSwatch(argb: Long, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(argb))
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) ZcutLime else MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                )
        )
    }
}

private fun defaultDraft(): TextConfig = TextConfig(
    text = "Teks baru",
    stylePreset = TextStylePreset.BOLD,
    animation = TextAnimation.RISE
)

internal fun styleLabel(preset: TextStylePreset): String = when (preset) {
    TextStylePreset.NONE -> "Normal"
    TextStylePreset.BOLD -> "Bold"
    TextStylePreset.OUTLINE -> "Outline"
    TextStylePreset.SHADOW -> "Shadow"
    TextStylePreset.NEON -> "Neon"
    TextStylePreset.GRADIENT -> "Gradient"
}

private fun alignmentLabel(alignment: TextAlignment): String = when (alignment) {
    TextAlignment.LEFT -> "Kiri"
    TextAlignment.CENTER -> "Tengah"
    TextAlignment.RIGHT -> "Kanan"
}

private fun animationLabel(animation: TextAnimation): String = when (animation) {
    TextAnimation.NONE -> "None"
    TextAnimation.RISE -> "Rise"
    TextAnimation.TYPEWRITER -> "Typewriter"
    TextAnimation.POP -> "Pop"
    TextAnimation.GLITCH -> "Glitch"
}

internal val StylePresets = listOf(
    TextStylePreset.BOLD,
    TextStylePreset.OUTLINE,
    TextStylePreset.SHADOW,
    TextStylePreset.NEON,
    TextStylePreset.GRADIENT
)

internal val TextColors = listOf(
    0xFFFFFFFF,
    0xFFC9FF4A,
    0xFFFF6B4A,
    0xFF59D9FF,
    0xFFB69CFF,
    0xFF000000
)
