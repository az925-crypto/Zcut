package com.zaaam.zcut.feature.media

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zcut.core.data.ProjectRepository
import com.zaaam.zcut.core.domain.DraftSummaries
import com.zaaam.zcut.core.domain.MediaAppender
import com.zaaam.zcut.core.domain.ProjectFactory
import com.zaaam.zcut.core.media.MediaImporter
import com.zaaam.zcut.core.model.MediaAsset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The picker is the only M2 screen that writes a draft. The editor observes the
 * repository, so imported clips show up there without a manual refresh.
 */
class MediaPickerViewModel(
    private val projectId: String?,
    private val repository: ProjectRepository,
    private val importer: MediaImporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(MediaPickerUiState(projectId = projectId))
    val uiState: StateFlow<MediaPickerUiState> = _uiState.asStateFlow()

    // Set synchronously, before the coroutine starts: two fast taps on the CTA
    // must not create two drafts or import the same batch twice.
    private var writeInFlight = false

    init {
        if (projectId != null) {
            viewModelScope.launch {
                val name = runCatching { repository.get(projectId)?.name }.getOrNull()
                if (name != null) _uiState.update { it.copy(projectName = name) }
            }
        }
    }

    fun setFilter(filter: MediaFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun addPicked(uris: List<String>) {
        if (uris.isEmpty()) return
        _uiState.update { state ->
            val merged = (state.selected + uris).distinct()
            if (merged.size > MAX_SELECTION) {
                state.copy(
                    selected = merged.take(MAX_SELECTION),
                    notice = "Maksimal $MAX_SELECTION item per impor."
                )
            } else {
                state.copy(selected = merged)
            }
        }
    }

    fun remove(uri: String) {
        _uiState.update { it.copy(selected = it.selected.filterNot { selected -> selected == uri }) }
    }

    fun clear() {
        _uiState.update { it.copy(selected = emptyList()) }
    }

    fun dismissNotice() {
        _uiState.update { it.copy(notice = null, pendingProjectId = null) }
    }

    fun import(onDone: (String) -> Unit) {
        val snapshot = _uiState.value
        if (writeInFlight || snapshot.isImporting || snapshot.selected.isEmpty()) return
        val targetProjectId = projectId
        writeInFlight = true

        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, notice = null, pendingProjectId = null) }
            try {
                val existing = if (targetProjectId != null) repository.get(targetProjectId) else null
                if (targetProjectId != null && existing == null) {
                    _uiState.update {
                        it.copy(isImporting = false, notice = "Draft tidak ditemukan.")
                    }
                    return@launch
                }

                val imported = mutableListOf<MediaAsset>()
                var failed = 0
                snapshot.selected.forEachIndexed { index, uriString ->
                    val uri = runCatching { Uri.parse(uriString) }.getOrNull()
                    when (val result = uri?.let { importer.import(it) }) {
                        is MediaImporter.Result.Success -> imported += result.asset
                        else -> failed++
                    }
                    _uiState.update {
                        it.copy(progress = ImportProgress(index + 1, snapshot.selected.size))
                    }
                }

                if (imported.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            progress = null,
                            notice = "Tidak ada media yang bisa diimpor ($failed gagal)."
                        )
                    }
                    return@launch
                }

                val project = if (existing != null) {
                    MediaAppender.append(existing, imported)
                } else {
                    ProjectFactory.fromAssets(nextName(), imported)
                }
                repository.upsert(project)

                if (failed == 0) {
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            progress = null,
                            selected = emptyList()
                        )
                    }
                    onDone(project.id)
                } else {
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            progress = null,
                            selected = emptyList(),
                            notice = "${imported.size} media ditambahkan, $failed gagal.",
                            pendingProjectId = project.id
                        )
                    }
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        progress = null,
                        notice = "Impor gagal: " + (error.message ?: "kesalahan tak dikenal")
                    )
                }
            } finally {
                writeInFlight = false
            }
        }
    }

    fun startEmpty(onDone: (String) -> Unit) {
        if (writeInFlight || _uiState.value.isImporting) return
        writeInFlight = true
        viewModelScope.launch {
            try {
                val project = ProjectFactory.create(nextName())
                repository.upsert(project)
                onDone(project.id)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(notice = "Gagal membuat draft: " + (error.message ?: "kesalahan tak dikenal"))
                }
            } finally {
                writeInFlight = false
            }
        }
    }

    private suspend fun nextName(): String =
        DraftSummaries.nextName(repository.summaries().map { it.name }.toSet())

    private companion object {
        const val MAX_SELECTION = 20
    }
}
