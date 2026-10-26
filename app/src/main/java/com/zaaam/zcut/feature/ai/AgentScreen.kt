package com.zaaam.zcut.feature.ai

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.zcut.core.domain.AgentPlanner
import com.zaaam.zcut.feature.tools.ToolChip
import com.zaaam.zcut.feature.tools.ToolChipRow
import com.zaaam.zcut.feature.tools.ToolHint
import com.zaaam.zcut.feature.tools.ToolSection
import com.zaaam.zcut.feature.tools.ToolValueRow
import com.zaaam.zcut.ui.LocalAppContainer
import com.zaaam.zcut.ui.agentViewModelFactory
import com.zaaam.zcut.ui.components.EmptyState
import com.zaaam.zcut.ui.components.StatusBadge
import com.zaaam.zcut.ui.components.ZcutIconButton
import com.zaaam.zcut.ui.components.ZcutScaffold
import com.zaaam.zcut.ui.components.ZcutSecondaryButton
import com.zaaam.zcut.ui.components.ZcutTopBar
import com.zaaam.zcut.ui.theme.ZcutCoral
import com.zaaam.zcut.ui.theme.ZcutLime
import com.zaaam.zcut.ui.theme.ZcutSpacing

/**
 * Zcut Agent (F23). Chat, suggestion chips and one honest apply button; the plan
 * bubbles list exactly which steps run on device and which ones need a backend.
 */
@Composable
fun AgentScreen(
    projectId: String?,
    onBack: () -> Unit,
    onOpenEditor: (String) -> Unit,
    onOpenRoute: (String) -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: AgentViewModel = viewModel(
        key = "ai-agent:${projectId.orEmpty()}",
        factory = agentViewModelFactory(container, projectId)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ZcutScaffold(
        topBar = {
            ZcutTopBar(
                title = "Zcut Agent",
                subtitle = "Tool-calling assistant · local rules",
                navigationIcon = {
                    ZcutIconButton(
                        icon = Icons.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        onClick = onBack
                    )
                },
                actions = { StatusBadge(text = "OFFLINE", accent = ZcutLime) }
            )
        }
    ) { insets ->
        val modifier = Modifier
            .fillMaxSize()
            .padding(insets)
        if (state.loading) {
            Box(modifier, contentAlignment = Alignment.Center) {
                EmptyState(
                    title = "Menyiapkan agent",
                    message = "Membaca draft di perangkat ini."
                )
            }
        } else {
            AgentBody(
                state = state,
                onInput = viewModel::setInput,
                onSend = { viewModel.send() },
                onSuggest = viewModel::send,
                onDismissNotice = viewModel::clearNotice,
                onApply = { viewModel.applyPlan { id -> if (id != null) onOpenEditor(id) } },
                onOpenRoute = onOpenRoute,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun AgentBody(
    state: AgentUiState,
    onInput: (String) -> Unit,
    onSend: () -> Unit,
    onSuggest: (String) -> Unit,
    onDismissNotice: () -> Unit,
    onApply: () -> Unit,
    onOpenRoute: (String) -> Unit,
    modifier: Modifier
) {
    val listState = rememberLazyListState()
    // The newest bubble is the interesting one; keep it in view.
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.scrollToItem(state.messages.lastIndex)
        }
    }

    Column(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(
                start = ZcutSpacing.screen,
                end = ZcutSpacing.screen,
                top = ZcutSpacing.sm,
                bottom = ZcutSpacing.sm
            ),
            verticalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
        ) {
            items(state.messages) { message -> ChatBubble(message) }

            val plan = state.plan
            if (plan != null && plan.steps.isNotEmpty()) {
                item {
                    ToolSection(title = "Tool calls") {
                        plan.steps.forEach { step -> PlanStepRow(step, onOpenRoute) }
                    }
                }
            }

            item {
                ToolSection {
                    ToolValueRow("Target draft", state.targetName ?: "—")
                    ToolHint(
                        if (state.targetId == null) {
                            "Belum ada draft di perangkat ini. Buat draft dulu, lalu " +
                                "agent bisa menerapkan rencananya."
                        } else {
                            "Plan hanya menyentuh draft ini. Langkah berlabel BACKEND " +
                                "tidak dijalankan."
                        }
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ZcutSpacing.screen, vertical = ZcutSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
        ) {
            ToolChipRow {
                AgentPlanner.SUGGESTIONS.forEach { suggestion ->
                    ToolChip(
                        label = suggestion,
                        selected = false,
                        onClick = { onSuggest(suggestion) },
                        enabled = !state.busy
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
            ) {
                OutlinedTextField(
                    value = state.input,
                    onValueChange = onInput,
                    placeholder = { Text("Describe an edit...") },
                    label = { Text("Message Zcut Agent") },
                    maxLines = 3,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.weight(1f)
                )
                ZcutIconButton(
                    icon = Icons.Filled.PlayArrow,
                    contentDescription = "Kirim",
                    onClick = onSend,
                    enabled = !state.busy && state.input.isNotBlank()
                )
            }

            ToolSection {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "LLM backend",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    StatusBadge(text = "OPTIONAL", accent = ZcutCoral)
                }
                ToolHint(
                    "Tool calls, timeline validation, dan preview berjalan lokal. " +
                        "Kualitas bahasa penuh bisa ditingkatkan dengan backend sendiri."
                )
            }

            ZcutSecondaryButton(
                text = if (state.busy) "Menerapkan..." else "Apply plan to draft",
                onClick = onApply,
                enabled = state.canApply,
                modifier = Modifier.fillMaxWidth()
            )

            if (state.notice != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.notice.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDismissNotice) { Text("Tutup") }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: AgentMessage) {
    val shape = RoundedCornerShape(12.dp)
    val bubbleColor = if (message.fromUser) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (message.fromUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = bubbleColor,
            shape = shape,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(shape)
        ) {
            Column(
                modifier = Modifier
                    .background(bubbleColor)
                    .padding(ZcutSpacing.md),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                message.tag?.let { tag ->
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/** One plan line: the detail plus whether it runs here or needs a backend. */
@Composable
private fun PlanStepRow(step: AgentPlanner.Step, onOpenRoute: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = step.tool.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            StatusBadge(
                text = if (step.runnable) "LOCAL" else "BACKEND",
                accent = if (step.runnable) ZcutLime else ZcutCoral
            )
        }
        Text(
            text = step.detail,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        step.route?.let { route ->
            TextButton(onClick = { onOpenRoute(route) }) { Text("Buka layarnya") }
        }
    }
}