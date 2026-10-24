package com.zaaam.zcut.feature.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zcut.core.data.ProjectRepository
import com.zaaam.zcut.core.domain.DraftSummaries
import com.zaaam.zcut.core.domain.ProjectsFilter
import com.zaaam.zcut.core.model.AspectRatio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProjectsViewModel(
    private val repository: ProjectRepository
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val aspectRatio = MutableStateFlow<AspectRatio?>(null)

    val uiState: StateFlow<ProjectsUiState> = combine(
        repository.projects,
        query,
        aspectRatio
    ) { projects, currentQuery, currentAspect ->
        val all = DraftSummaries.from(projects)
        ProjectsUiState(
            isLoading = false,
            drafts = ProjectsFilter.apply(all, currentQuery, currentAspect),
            totalCount = all.size,
            query = currentQuery,
            aspectRatio = currentAspect
        )
    }
        .catch { throwable ->
            emit(
                ProjectsUiState(
                    isLoading = false,
                    error = throwable.message ?: "Gagal memuat draft"
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProjectsUiState()
        )

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onAspectChange(value: AspectRatio?) {
        aspectRatio.value = if (aspectRatio.value == value) null else value
    }

    fun duplicate(projectId: String) {
        viewModelScope.launch { repository.duplicate(projectId) }
    }

    fun delete(projectId: String) {
        viewModelScope.launch { repository.delete(projectId) }
    }
}
