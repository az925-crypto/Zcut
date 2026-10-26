package com.zaaam.zcut.feature.templates

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.zcut.core.domain.VideoTemplate
import com.zaaam.zcut.ui.LocalAppContainer
import com.zaaam.zcut.ui.components.EmptyState
import com.zaaam.zcut.ui.components.StatusBadge
import com.zaaam.zcut.ui.components.ZcutCard
import com.zaaam.zcut.ui.components.ZcutIconButton
import com.zaaam.zcut.ui.components.ZcutPrimaryButton
import com.zaaam.zcut.ui.components.ZcutScaffold
import com.zaaam.zcut.ui.components.ZcutTopBar
import com.zaaam.zcut.ui.templateDetailViewModelFactory
import com.zaaam.zcut.ui.theme.ZcutLime
import com.zaaam.zcut.ui.theme.ZcutSpacing

/**
 * Template detail (F21). Shows what the pack contains and hands the user to the
 * media picker in template mode, where the slots get filled.
 */
@Composable
fun TemplateDetailScreen(
    templateId: String,
    onBack: () -> Unit,
    onUseTemplate: (String) -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: TemplateDetailViewModel = viewModel(
        key = "template-detail:$templateId",
        factory = templateDetailViewModelFactory(container, templateId)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ZcutScaffold(
        topBar = {
            ZcutTopBar(
                title = "Template",
                navigationIcon = {
                    ZcutIconButton(
                        icon = Icons.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        onClick = onBack
                    )
                },
                actions = {
                    val template = state.template
                    if (template != null) {
                        ZcutIconButton(
                            icon = Icons.Filled.Star,
                            contentDescription = if (state.favorite) {
                                "Hapus dari favorit"
                            } else {
                                "Simpan ke favorit"
                            },
                            onClick = viewModel::toggleFavorite
                        )
                    }
                }
            )
        }
    ) { insets ->
        val modifier = Modifier
            .fillMaxSize()
            .padding(insets)
        val template = state.template
        when {
            state.notFound || template == null -> Box(modifier, contentAlignment = Alignment.Center) {
                EmptyState(
                    title = "Template tidak ditemukan",
                    message = "Template bawaan ini tidak ada di katalog Zcut.",
                    actionLabel = "Kembali",
                    onAction = onBack
                )
            }

            else -> TemplateDetailBody(
                template = template,
                favorite = state.favorite,
                onUse = { onUseTemplate(template.id) },
                modifier = modifier
            )
        }
    }
}

@Composable
private fun TemplateDetailBody(
    template: VideoTemplate,
    favorite: Boolean,
    onUse: () -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
        ) {
            TemplateArtBox(artKey = template.artKey)
            StatusBadge(
                text = "LOCAL TEMPLATE",
                accent = ZcutLime,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(ZcutSpacing.sm)
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(ZcutSpacing.md)
            ) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Text(
                    text = "${template.slotCount} media slots · " +
                        "${template.durationLabel} · ${template.ratio.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        Column(
            modifier = Modifier.padding(
                horizontal = ZcutSpacing.screen,
                vertical = ZcutSpacing.md
            ),
            verticalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.xs)
            ) {
                StatusBadge(text = "NO WATERMARK", accent = ZcutLime)
                StatusBadge(text = "${template.slotCount} SLOTS")
                if (template.bpm != null) StatusBadge(text = "BEAT SYNC")
                if (template.captionStyle) StatusBadge(text = "CAPTIONS")
            }

            Text(
                text = template.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ZcutCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(ZcutSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
                ) {
                    Text(
                        text = "What is included",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    // Informational rows: no draft exists yet, so there is
                    // nothing to navigate to. Tapping "Use template" is the one
                    // action that moves forward.
                    IncludedRow(
                        title = "${template.slotCount} media slots",
                        detail = "Auto-trim + center crop"
                    )
                    if (template.captionStyle) {
                        IncludedRow(
                            title = "Caption style",
                            detail = "Editable, local font"
                        )
                    }
                    IncludedRow(
                        title = "Beat markers",
                        detail = if (template.bpm != null) {
                            "Pakai audio sendiri · ${template.bpm} BPM"
                        } else {
                            "Pakai audio sendiri"
                        }
                    )
                }
            }

            ZcutPrimaryButton(
                text = "Use template",
                onClick = onUse,
                modifier = Modifier.fillMaxWidth()
            )

            if (favorite) {
                Text(
                    text = "Disimpan di favorit lokal.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "Template ini dibuat untuk Zcut. Tidak ada aset CapCut yang dipakai.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(ZcutSpacing.xl))
        }
    }
}

@Composable
private fun IncludedRow(title: String, detail: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}