package com.zaaam.zcut.feature.home

import com.zaaam.zcut.core.model.DraftSummary

data class HomeUiState(
    val isLoading: Boolean = true,
    val drafts: List<DraftSummary> = emptyList(),
    val error: String? = null
) {
    val recentDrafts: List<DraftSummary> get() = drafts.take(MAX_RECENT)

    val hasDrafts: Boolean get() = drafts.isNotEmpty()

    companion object {
        const val MAX_RECENT = 8
    }
}
