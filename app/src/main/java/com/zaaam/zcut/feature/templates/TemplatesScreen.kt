package com.zaaam.zcut.feature.templates

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.zaaam.zcut.core.domain.TemplateCatalog
import com.zaaam.zcut.core.domain.TemplateCategory
import com.zaaam.zcut.core.i18n.LocalZcutLanguage
import com.zaaam.zcut.core.i18n.ZcutText
import com.zaaam.zcut.core.i18n.zt
import com.zaaam.zcut.core.domain.VideoTemplate
import com.zaaam.zcut.ui.components.BackendBadge
import com.zaaam.zcut.ui.components.StatusBadge
import com.zaaam.zcut.ui.components.ZcutCard
import com.zaaam.zcut.ui.components.ZcutChip
import com.zaaam.zcut.ui.components.ZcutIconButton
import com.zaaam.zcut.ui.components.ZcutScaffold
import com.zaaam.zcut.ui.components.ZcutTopBar
import com.zaaam.zcut.ui.components.ZcutBottomNav
import com.zaaam.zcut.ui.theme.ZcutLime
import com.zaaam.zcut.ui.theme.ZcutSpacing

/**
 * Templates tab (F21). All packs are built into the APK, so the grid renders
 * offline and never shows a lock or a price. Marketplace / AI templates are
 * honestly flagged as backend work instead of being hidden.
 */
@Composable
fun TemplatesScreen(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onOpenTemplate: (String) -> Unit
) {
    var category by remember { mutableStateOf(TemplateCategory.FOR_YOU) }
    val templates = remember(category) { TemplateCatalog.inCategory(category) }
    val language = LocalZcutLanguage.current

    ZcutScaffold(
        topBar = {
            ZcutTopBar(
                title = "Templates",
                subtitle = "Local packs · tanpa gembok marketplace",
                actions = {
                    ZcutIconButton(
                        icon = Icons.Filled.Search,
                        contentDescription = "Discover",
                        onClick = { onNavigate("discover") }
                    )
                }
            )
        },
        bottomBar = {
            ZcutBottomNav(currentRoute = currentRoute, onNavigate = onNavigate)
        }
    ) { insets ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(insets),
            contentPadding = PaddingValues(
                start = ZcutSpacing.screen,
                end = ZcutSpacing.screen,
                top = ZcutSpacing.sm,
                bottom = ZcutSpacing.xxl
            ),
            horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
                ) {
                    TemplateCatalog.categories.forEach { option ->
                        ZcutChip(
                            label = option.label,
                            selected = option == category,
                            onClick = { category = option }
                        )
                    }
                }
            }

            items(templates, key = VideoTemplate::id) { template ->
                TemplateTile(
                    template = template,
                    onClick = { onOpenTemplate(template.id) }
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)) {
                    Text(
                        text = zt(ZcutText.MarketplaceNote, language),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    BackendBadge(featureId = "F21")
                }
            }
        }
    }
}

@Composable
private fun TemplateTile(
    template: VideoTemplate,
    onClick: () -> Unit
) {
    ZcutCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                TemplateArtBox(artKey = template.artKey)
                StatusBadge(
                    text = "LOCAL",
                    accent = ZcutLime,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(ZcutSpacing.sm)
                )
            }
            Column(
                modifier = Modifier.padding(ZcutSpacing.md),
                verticalArrangement = Arrangement.spacedBy(ZcutSpacing.xs)
            ) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = template.slotSummary,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}