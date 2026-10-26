package com.zaaam.zcut.feature.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zcut.core.data.DraftBackup
import com.zaaam.zcut.core.data.ProjectRepository
import com.zaaam.zcut.core.domain.DraftSummaries
import com.zaaam.zcut.core.domain.ProjectsFilter
import com.zaaam.zcut.core.model.AspectRatio
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Draft list (F20): search/filter, rename, duplicate, trash/restore, and local
 * `.zcut.zip` backup/import. Cloud backup, shared drafts and review stay
 * BUTUH BACKEND and are not offered here.
 */
class ProjectsViewModel(
    private val repository: ProjectRepository,
    private val mediaDir: File
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val aspectRatio = MutableStateFlow<AspectRatio?>(null)
    private val trashMode = MutableStateFlow(false)
    private val notice = MutableStateFlow<String?>(null)

    private val listState = combine(
        repository.allProjects,
        query,
        aspectRatio,
        trashMode
    ) { projects, currentQuery, currentAspect, inTrash ->
        val visible = projects.filter { project ->
            if (inTrash) project.trashedAtMs != null else project.trashedAtMs == null
        }
        val summaries = DraftSummaries.from(visible)
        ProjectsUiState(
            isLoading = false,
            drafts = ProjectsFilter.apply(summaries, currentQuery, currentAspect),
            totalCount = summaries.size,
            query = currentQuery,
            aspectRatio = currentAspect,
            trashMode = inTrash,
            trashCount = projects.count { it.trashedAtMs != null }
        )
    }

    val uiState: StateFlow<ProjectsUiState> = combine(listState, notice) { state, message ->
        state.copy(notice = message)
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

    fun toggleTrash() {
        trashMode.value = !trashMode.value
    }

    fun clearNotice() {
        notice.value = null
    }

    fun duplicate(projectId: String) {
        viewModelScope.launch { repository.duplicate(projectId) }
    }

    /** Permanent removal; the screen only offers this from the trash view. */
    fun delete(projectId: String) {
        viewModelScope.launch { repository.delete(projectId) }
    }

    fun rename(projectId: String, name: String) {
        viewModelScope.launch {
            if (!repository.rename(projectId, name)) {
                notice.value = "Nama draft tidak boleh kosong."
            }
        }
    }

    fun trash(projectId: String) {
        viewModelScope.launch {
            if (repository.trash(projectId)) {
                notice.value = "Draft dipindah ke sampah. Pulihkan kapan saja dari tab Sampah."
            }
        }
    }

    fun restore(projectId: String) {
        viewModelScope.launch {
            if (repository.restore(projectId)) notice.value = "Draft dipulihkan."
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            val removed = repository.emptyTrash()
            notice.value = if (removed > 0) {
                "$removed draft dihapus permanen."
            } else {
                "Sampah sudah kosong."
            }
        }
    }

    /**
     * Writes one draft + its media to the stream the user picked. The screen
     * owns the SAF URI and hands the stream in, so the view model stays free of
     * Android content-resolver types.
     */
    fun backup(projectId: String, openOutput: () -> OutputStream?) {
        viewModelScope.launch {
            val project = repository.get(projectId)
            if (project == null) {
                notice.value = "Draft tidak ditemukan."
                return@launch
            }
            val stream = withContext(Dispatchers.IO) {
                runCatching(openOutput).getOrNull()
            }
            if (stream == null) {
                notice.value = "Tidak bisa membuka berkas backup."
                return@launch
            }
            val ok = withContext(Dispatchers.IO) {
                runCatching { DraftBackup.export(project, stream) }.getOrDefault(false)
            }
            notice.value = if (ok) {
                "Backup ${DraftBackup.fileNameFor(project)} dibuat."
            } else {
                "Gagal menulis backup."
            }
        }
    }

    fun importBackup(openInput: () -> InputStream?) {
        viewModelScope.launch {
            val stream = withContext(Dispatchers.IO) {
                runCatching(openInput).getOrNull()
            }
            if (stream == null) {
                notice.value = "Tidak bisa membaca berkas backup."
                return@launch
            }
            val project = withContext(Dispatchers.IO) {
                runCatching { DraftBackup.import(stream, mediaDir) }.getOrNull()
            }
            if (project == null) {
                notice.value = "Backup tidak valid, rusak, atau kebesaran."
                return@launch
            }
            repository.upsert(project)
            trashMode.value = false
            notice.value = "Draft ${project.name} diimpor dari backup."
        }
    }
}
