package com.zaaam.zcut.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.zcut.core.model.DraftSummary
import com.zaaam.zcut.ui.LocalAppContainer
import com.zaaam.zcut.ui.components.BackendBadge
import com.zaaam.zcut.ui.components.EmptyState
import com.zaaam.zcut.ui.components.HeroBanner
import com.zaaam.zcut.ui.components.ProjectRailCard
import com.zaaam.zcut.ui.components.SectionHeader
import com.zaaam.zcut.ui.components.ZcutCard
import com.zaaam.zcut.ui.components.ZcutScaffold
import com.zaaam.zcut.ui.components.ZcutBottomNav
import com.zaaam.zcut.ui.components.ZcutTopBar
import com.zaaam.zcut.ui.navigation.ZcutRoutes
import com.zaaam.zcut.ui.theme.ZcutSpacing
import com.zaaam.zcut.ui.zcutViewModelFactory

@Composable
fun HomeScreen(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: HomeViewModel = viewModel(factory = zcutViewModelFactory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    HomeContent(
        state = state,
        currentRoute = currentRoute,
        onNavigate = onNavigate
    )
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val nowMs = System.currentTimeMillis()
    val recentDrafts = state.recentDrafts

    ZcutScaffold(
        topBar = {
            ZcutTopBar(
                title = "zcut.",
                subtitle = "Studio video lokal · tanpa watermark"
            )
        },
        bottomBar = {
            ZcutBottomNav(currentRoute = currentRoute, onNavigate = onNavigate)
        }
    ) { insets ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets),
            contentPadding = PaddingValues(
                start = ZcutSpacing.screen,
                end = ZcutSpacing.screen,
                top = ZcutSpacing.sm,
                bottom = ZcutSpacing.xxl
            ),
            verticalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
        ) {
            item {
                HeroBanner(
                    title = "Edit video tanpa batas",
                    message = "Semua fitur terbuka, diproses di perangkat, hasil tanpa watermark.",
                    actionLabel = "Project baru",
                    onAction = { onNavigate(ZcutRoutes.Editor) }
                )
            }

            if (state.error != null) {
                item { ErrorBanner(state.error) }
            }

            item {
                SectionHeader(
                    title = "Draft terakhir",
                    actionLabel = "Lihat semua",
                    onAction = { onNavigate(ZcutRoutes.Projects) }
                )
            }

            if (recentDrafts.isEmpty() && !state.isLoading) {
                item {
                    EmptyState(
                        title = "Belum ada draft",
                        message = "Mulai project baru untuk membuat draft pertama di perangkat ini.",
                        actionLabel = "Project baru",
                        onAction = { onNavigate(ZcutRoutes.Editor) }
                    )
                }
            } else {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
                    ) {
                        items(recentDrafts, key = DraftSummary::id) { draft ->
                            ProjectRailCard(
                                draft = draft,
                                nowMs = nowMs,
                                onClick = { onNavigate(ZcutRoutes.Editor) }
                            )
                        }
                    }
                }
            }

            item {
                SectionHeader(title = "Tool AI")
            }
            item {
                AiToolsRow(onNavigate = onNavigate)
            }
        }
    }
}

@Composable
private fun AiToolsRow(onNavigate: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
    ) {
        AiToolCard(
            title = "AI Studio",
            featureId = "F23",
            route = "ai-studio",
            onNavigate = onNavigate,
            modifier = Modifier.weight(1f)
        )
        AiToolCard(
            title = "AutoCut",
            featureId = "F22",
            route = "ai-clipper",
            onNavigate = onNavigate,
            modifier = Modifier.weight(1f)
        )
        AiToolCard(
            title = "Generate",
            featureId = "F24",
            route = "ai-generate",
            onNavigate = onNavigate,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AiToolCard(
    title: String,
    featureId: String,
    route: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ZcutCard(
        modifier = modifier,
        onClick = { onNavigate(route) }
    ) {
        Column(
            modifier = Modifier.padding(ZcutSpacing.md),
            verticalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
        ) {
            Box(
                modifier = Modifier.height(20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            BackendBadge(featureId = featureId)
            Spacer(Modifier.height(0.dp))
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(ZcutSpacing.md)
        )
    }
}
