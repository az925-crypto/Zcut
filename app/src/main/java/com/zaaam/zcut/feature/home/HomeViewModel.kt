package com.zaaam.zcut.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zcut.core.data.ProjectRepository
import com.zaaam.zcut.core.domain.DraftSummaries
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    private val repository: ProjectRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = repository.projects
        .map { projects ->
            HomeUiState(isLoading = false, drafts = DraftSummaries.from(projects))
        }
        .catch { throwable ->
            emit(
                HomeUiState(
                    isLoading = false,
                    error = throwable.message ?: "Gagal memuat draft"
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState()
        )
}
