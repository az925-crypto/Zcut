package com.zaaam.zcut.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zaaam.zcut.core.model.DraftSummary
import com.zaaam.zcut.core.util.RelativeTime
import com.zaaam.zcut.core.util.TimeFormat
import com.zaaam.zcut.ui.theme.ZcutCoral
import com.zaaam.zcut.ui.theme.ZcutCyan
import com.zaaam.zcut.ui.theme.ZcutLime
import com.zaaam.zcut.ui.theme.ZcutViolet

/** Gradient cover placeholder: original Zcut art, derived from the project id. */
private val DRAFT_COVER_GRADIENTS: List<List<Color>> = listOf(
    listOf(ZcutLime, ZcutCyan),
    listOf(ZcutCoral, ZcutViolet),
    listOf(ZcutCyan, ZcutLime),
    listOf(ZcutViolet, ZcutCoral)
).map { pair -> pair.map { it.copy(alpha = 0.55f) } }

@Composable
fun DraftCover(
    seed: String,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    val index = (seed.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }) %
        DRAFT_COVER_GRADIENTS.size
    val pair = DRAFT_COVER_GRADIENTS[index]
    Box(
        modifier = modifier.background(Brush.linearGradient(pair)),
        contentAlignment = Alignment.BottomEnd
    ) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(6.dp)
            )
        }
    }
}

/** Compact card used in the Home "recent drafts" rail. */
@Composable
fun ProjectRailCard(
    draft: DraftSummary,
    nowMs: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(142.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        DraftCover(
            seed = draft.id,
            label = TimeFormat.clock(draft.durationMs),
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Spacer(Modifier.height(7.dp))
        Text(
            text = draft.name,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${draft.aspectRatio.label} · ${RelativeTime.format(nowMs, draft.updatedAtMs)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Full-width row used in the Projects list. */
@Composable
fun ProjectRowCard(
    draft: DraftSummary,
    nowMs: Long,
    onClick: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DraftCover(
                seed = draft.id,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = draft.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                MetaRow(
                    items = listOf(
                        RelativeTime.format(nowMs, draft.updatedAtMs),
                        TimeFormat.clock(draft.durationMs),
                        draft.aspectRatio.label
                    )
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(onClick = onMore),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Opsi draft ${draft.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
