package com.zaaam.zcut.feature.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.zaaam.zcut.core.i18n.LocalZcutLanguage
import com.zaaam.zcut.core.i18n.ZcutText
import com.zaaam.zcut.core.i18n.zt
import com.zaaam.zcut.ui.components.StatusBadge
import com.zaaam.zcut.ui.components.ZcutBottomNav
import com.zaaam.zcut.ui.components.ZcutCard
import com.zaaam.zcut.ui.components.ZcutPrimaryButton
import com.zaaam.zcut.ui.components.ZcutScaffold
import com.zaaam.zcut.ui.components.ZcutTopBar
import com.zaaam.zcut.ui.navigation.ZcutRoutes
import com.zaaam.zcut.ui.theme.ZcutCoral
import com.zaaam.zcut.ui.theme.ZcutLime
import com.zaaam.zcut.ui.theme.ZcutSpacing

/** One card of the hub grid; the badge states honestly where the work runs. */
private data class AiToolCard(
    val route: String,
    val title: String,
    val blurb: String,
    val badge: String,
    val accent: Color
)

private val toolCards = listOf(
    AiToolCard(
        route = ZcutRoutes.AiClipper,
        title = "AI Clipper",
        blurb = "Find short moments in long video.",
        badge = "LOCAL",
        accent = ZcutLime
    ),
    AiToolCard(
        route = ZcutRoutes.AiAgent,
        title = "Agent",
        blurb = "Describe an edit in plain language.",
        badge = "LOCAL",
        accent = ZcutLime
    ),
    AiToolCard(
        route = "ai-generate",
        title = "Generate",
        blurb = "Text to image or video.",
        badge = "BACKEND",
        accent = ZcutCoral
    ),
    AiToolCard(
        route = "digital-human",
        title = "Digital Human",
        blurb = "Avatar presenter + lip sync.",
        badge = "BACKEND",
        accent = ZcutCoral
    ),
    AiToolCard(
        route = ZcutRoutes.AiMusic,
        title = "AI Music",
        blurb = "Procedural loops on device.",
        badge = "LOCAL*",
        accent = ZcutLime
    ),
    AiToolCard(
        route = "ad-maker",
        title = "Ad Maker",
        blurb = "Script to a first ad cut.",
        badge = "LOCAL*",
        accent = ZcutLime
    )
)

/**
 * AI Studio hub (F23). Every card is a real destination — the local ones open
 * the working screens, the backend ones open the honest "needs a backend"
 * placeholder. Nothing here is locked, priced, or credit-metered.
 */
@Composable
fun AiStudioScreen(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val language = LocalZcutLanguage.current
    ZcutScaffold(
        topBar = {
            ZcutTopBar(
                title = "AI Studio",
                subtitle = "Local tools first · backend only when needed",
                actions = { StatusBadge(text = "NO CREDITS", accent = ZcutLime) }
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
                ZcutCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(ZcutSpacing.lg),
                        verticalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
                    ) {
                        Text(
                            text = "Make the impossible cut.",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Start with on-device helpers. Generative models are " +
                                "optional and never use CapCut servers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        ZcutPrimaryButton(
                            text = "Ask Zcut Agent",
                            onClick = { onNavigate(ZcutRoutes.AiAgent) }
                        )
                    }
                }
            }

            items(toolCards, key = AiToolCard::route) { card ->
                AiToolTile(card = card, onClick = { onNavigate(card.route) })
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                ZcutCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(ZcutSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
                    ) {
                        Text(
                            text = zt(ZcutText.AiStudioNote, language),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)) {
                            StatusBadge(text = "LOCAL = on device", accent = ZcutLime)
                            StatusBadge(
                                text = zt(ZcutText.BackendNeedsServer, language),
                                accent = ZcutCoral
                            )
                        }
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Recently used",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = ZcutSpacing.sm)
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                ZcutCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onNavigate(ZcutRoutes.AiClipper) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(ZcutSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.md)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Long vlog → 3 shorts",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "AI Clipper · local heuristic",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusBadge(text = "DONE", accent = ZcutLime)
                    }
                }
            }
        }
    }
}

@Composable
private fun AiToolTile(card: AiToolCard, onClick: () -> Unit) {
    ZcutCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(
            modifier = Modifier.padding(ZcutSpacing.md),
            verticalArrangement = Arrangement.spacedBy(ZcutSpacing.xs)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                StatusBadge(text = card.badge, accent = card.accent)
            }
            Text(
                text = card.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = card.blurb,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                minLines = 3,
                lineHeight = 14.sp
            )
        }
    }
}