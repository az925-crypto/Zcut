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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.zaaam.zcut.core.domain.DiscoverFeed
import com.zaaam.zcut.core.domain.DiscoverPost
import com.zaaam.zcut.ui.components.BackendBadge
import com.zaaam.zcut.ui.components.ZcutCard
import com.zaaam.zcut.ui.components.ZcutChip
import com.zaaam.zcut.ui.components.ZcutIconButton
import com.zaaam.zcut.ui.components.ZcutScaffold
import com.zaaam.zcut.ui.components.ZcutTopBar
import com.zaaam.zcut.ui.theme.ZcutLime
import com.zaaam.zcut.ui.theme.ZcutSpacing

/**
 * Discover (F21). The feed is a local showcase: every card points at a built-in
 * template and nothing is fetched or published. Community publishing is flagged
 * as backend work rather than faked.
 */
@Composable
fun DiscoverScreen(
    onBack: () -> Unit,
    onOpenTemplate: (String) -> Unit
) {
    var tag by remember { mutableStateOf(DiscoverFeed.tags.first()) }
    val posts = remember(tag) { DiscoverFeed.inTag(tag) }

    ZcutScaffold(
        topBar = {
            ZcutTopBar(
                title = "Discover",
                subtitle = "Dibuat lokal, dibagikan bebas",
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
                DiscoverHero()
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
                ) {
                    DiscoverFeed.tags.forEach { option ->
                        ZcutChip(
                            label = option,
                            selected = option == tag,
                            onClick = { tag = option }
                        )
                    }
                }
            }

            items(posts, key = DiscoverPost::id) { post ->
                DiscoverCard(post = post, onClick = { onOpenTemplate(post.templateId) })
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)) {
                    Text(
                        text = "Feed ini adalah contoh lokal. Zcut tidak menghubungi " +
                            "server CapCut dan tidak mengirim data penggunaan.",
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
private fun DiscoverHero() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
    ) {
        TemplateArtBox(artKey = "city")
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(ZcutSpacing.md)
        ) {
            Text(
                text = "Small city, big cut.",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Text(
                text = "@localcreator · 24s · dibuat dengan Zcut",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun DiscoverCard(post: DiscoverPost, onClick: () -> Unit) {
    ZcutCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                TemplateArtBox(artKey = post.artKey)
                Text(
                    text = "TEMPLATE",
                    style = MaterialTheme.typography.labelSmall,
                    color = ZcutLime,
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
                    text = post.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${post.author} · ${post.template?.durationLabel ?: "—"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}