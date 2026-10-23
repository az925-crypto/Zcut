package com.zaaam.zcut.feature.projects

import com.zaaam.zcut.core.model.AspectRatio
import com.zaaam.zcut.core.model.DraftSummary

data class ProjectsUiState(
    val isLoading: Boolean = true,
    val drafts: List<DraftSummary> = emptyList(),
    /** Unfiltered count, so headers do not lie while a search is active. */
    val totalCount: Int = 0,
    val query: String = "",
    val aspectRatio: AspectRatio? = null,
    val error: String? = null
) {
    val isEmpty: Boolean get() = !isLoading && drafts.isEmpty()

    /** True when the stored list is empty, not merely filtered out. */
    val hasNoDraftsAtAll: Boolean get() = !isLoading && totalCount == 0
}

/** Aspect ratio chips shown above the draft list. `null` means "all". */
val ZcutAspectFilters: List<AspectRatio?> = listOf(
    null,
    AspectRatio.PORTRAIT_9_16,
    AspectRatio.SQUARE_1_1,
    AspectRatio.LANDSCAPE_16_9,
    AspectRatio.PORTRAIT_4_5
)
