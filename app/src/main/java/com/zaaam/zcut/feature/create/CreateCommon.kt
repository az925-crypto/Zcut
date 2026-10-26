package com.zaaam.zcut.feature.create

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zaaam.zcut.core.domain.FacecamPosition
import com.zaaam.zcut.core.domain.SplitLayout
import com.zaaam.zcut.core.model.AspectRatio
import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.feature.media.rememberThumbnail
import com.zaaam.zcut.feature.tools.ToolChip
import com.zaaam.zcut.feature.tools.ToolChipRow
import com.zaaam.zcut.ui.LocalAppContainer
import com.zaaam.zcut.ui.components.ZcutCard

/** One media frame with a thumbnail, shared by the three Create tools. */
@Composable
internal fun CreateThumb(
    asset: MediaAsset?,
    modifier: Modifier = Modifier,
    sizePx: Int = 480
) {
    val container = LocalAppContainer.current
    val thumbnail by rememberThumbnail(key = asset?.id) {
        if (asset == null) null else container.mediaThumbnails.load(asset, sizePx)
    }
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail!!,
                contentDescription = asset?.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = "+",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Five layout cards, matching the mockup's 1 / 2 / 4 / 6 / 9 panel picker. */
@Composable
internal fun SplitLayoutPicker(
    selected: SplitLayout,
    onSelect: (SplitLayout) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SplitLayout.entries.forEach { layout ->
            LayoutCard(
                layout = layout,
                selected = layout == selected,
                onClick = { onSelect(layout) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun LayoutCard(
    layout: SplitLayout,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ZcutCard(modifier = modifier, onClick = onClick) {
        Column(
            modifier = Modifier.padding(5.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                repeat(layout.rows) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        repeat(layout.columns) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.35f
                                            )
                                        }
                                    )
                            )
                        }
                    }
                }
            }
            Text(
                text = layout.label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
internal fun AspectChips(
    selected: AspectRatio,
    onSelect: (AspectRatio) -> Unit
) {
    ToolChipRow {
        AspectRatio.entries.forEach { aspect ->
            ToolChip(
                label = aspect.label,
                selected = aspect == selected,
                onClick = { onSelect(aspect) }
            )
        }
    }
}

/** 2×2 corner picker for the gameplay facecam. */
@Composable
internal fun FacecamPositionPicker(
    selected: FacecamPosition,
    onSelect: (FacecamPosition) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FacecamPosition.entries.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { position ->
                    PositionCard(
                        position = position,
                        selected = position == selected,
                        onClick = { onSelect(position) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(2 - row.size) { SpacerWeight() }
            }
        }
    }
}

@Composable
private fun RowScope.SpacerWeight() {
    Box(Modifier.weight(1f))
}

@Composable
private fun PositionCard(
    position: FacecamPosition,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ZcutCard(modifier = modifier, onClick = onClick) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(5.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .size(15.dp)
                    .align(position.toAlignment())
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
            )
        }
    }
}

internal fun FacecamPosition.toAlignment(): Alignment = when (this) {
    FacecamPosition.TOP_LEFT -> Alignment.TopStart
    FacecamPosition.TOP_RIGHT -> Alignment.TopEnd
    FacecamPosition.BOTTOM_LEFT -> Alignment.BottomStart
    FacecamPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
}
