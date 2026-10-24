package com.zaaam.zcut.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zcut.core.data.AppStorage
import com.zaaam.zcut.core.data.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    repository: ProjectRepository,
    private val storage: AppStorage
) : ViewModel() {

    private val storageBytes = MutableStateFlow(0L)

    val uiState: StateFlow<ProfileUiState> = combine(
        repository.projects,
        storageBytes
    ) { projects, bytes ->
        ProfileUiState(
            isLoading = false,
            draftCount = projects.size,
            storageBytes = bytes
        )
    }
        .catch { throwable ->
            emit(
                ProfileUiState(
                    isLoading = false,
                    error = throwable.message ?: "Gagal memuat profil"
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProfileUiState()
        )

    /** Cheap enough to re-run on entry; sizes change as drafts are edited. */
    fun refreshStorage() {
        viewModelScope.launch {
            storageBytes.value = runCatching { storage.usedBytes() }.getOrDefault(0L)
        }
    }
}
