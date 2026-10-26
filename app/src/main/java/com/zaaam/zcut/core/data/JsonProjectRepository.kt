package com.zaaam.zcut.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zaaam.zcut.core.model.DraftSummary
import com.zaaam.zcut.core.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

private val Context.projectDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "zcut_projects"
)

class JsonProjectRepository(
    private val context: Context
) : ProjectRepository {

    private val documentKey = stringPreferencesKey("project_document")

    // ponytail: one JSON document per device is fine for the scaffold. Move to
    // Room or one file per project when the draft list or clip count grows.
    override val allProjects: Flow<List<Project>> = context.projectDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences ->
            ProjectCodec.decode(preferences[documentKey]).projects
        }
        .flowOn(Dispatchers.IO)

    override suspend fun get(projectId: String): Project? =
        allProjects.first().firstOrNull { it.id == projectId }

    override suspend fun upsert(project: Project) {
        withContext(Dispatchers.IO) {
            context.projectDataStore.edit { preferences ->
                val document = ProjectCodec.decode(preferences[documentKey])
                val now = System.currentTimeMillis()
                val existing = document.projects.firstOrNull { it.id == project.id }
                val updated = project.copy(
                    updatedAtMs = now,
                    // Editing a draft never silently takes it out of the trash;
                    // only [restore] does that.
                    trashedAtMs = project.trashedAtMs ?: existing?.trashedAtMs
                )
                val next = (document.projects.filterNot { it.id == updated.id } + updated)
                    .sortedByDescending { it.updatedAtMs }
                preferences[documentKey] = ProjectCodec.encode(document.copy(projects = next))
            }
        }
    }

    override suspend fun delete(projectId: String) {
        withContext(Dispatchers.IO) {
            context.projectDataStore.edit { preferences ->
                val document = ProjectCodec.decode(preferences[documentKey])
                val next = document.projects.filterNot { it.id == projectId }
                preferences[documentKey] = ProjectCodec.encode(document.copy(projects = next))
            }
        }
    }

    override suspend fun duplicate(projectId: String): Project? {
        var result: Project? = null
        withContext(Dispatchers.IO) {
            context.projectDataStore.edit { preferences ->
                val document = ProjectCodec.decode(preferences[documentKey])
                val source = document.projects.firstOrNull { it.id == projectId }
                    ?: return@edit
                val now = System.currentTimeMillis()
                val duplicate = source.copy(
                    id = UUID.randomUUID().toString(),
                    name = "${source.name} copy",
                    createdAtMs = now,
                    updatedAtMs = now,
                    trashedAtMs = null
                )
                val next = (document.projects + duplicate)
                    .sortedByDescending { it.updatedAtMs }
                preferences[documentKey] = ProjectCodec.encode(document.copy(projects = next))
                result = duplicate
            }
        }
        return result
    }

    override suspend fun summaries(): List<DraftSummary> =
        projects.first().map { it.toSummary() }

    override suspend fun rename(projectId: String, name: String): Boolean {
        val safe = name.trim().take(MAX_NAME_LENGTH)
        if (safe.isBlank()) return false
        return update(projectId) { it.copy(name = safe) }
    }

    override suspend fun trash(projectId: String): Boolean {
        val now = System.currentTimeMillis()
        return update(projectId) { it.copy(trashedAtMs = now) }
    }

    override suspend fun restore(projectId: String): Boolean =
        update(projectId) { it.copy(trashedAtMs = null) }

    override suspend fun emptyTrash(): Int {
        var removed = 0
        withContext(Dispatchers.IO) {
            context.projectDataStore.edit { preferences ->
                val document = ProjectCodec.decode(preferences[documentKey])
                val keep = document.projects.filter { it.trashedAtMs == null }
                removed = document.projects.size - keep.size
                if (removed > 0) {
                    preferences[documentKey] = ProjectCodec.encode(
                        document.copy(projects = keep)
                    )
                }
            }
        }
        return removed
    }

    /** Applies [transform] to one stored draft; false when it is not there. */
    private suspend fun update(
        projectId: String,
        transform: (Project) -> Project
    ): Boolean {
        var changed = false
        withContext(Dispatchers.IO) {
            context.projectDataStore.edit { preferences ->
                val document = ProjectCodec.decode(preferences[documentKey])
                val target = document.projects.firstOrNull { it.id == projectId }
                    ?: return@edit
                val updated = transform(target).copy(updatedAtMs = System.currentTimeMillis())
                val next = (document.projects.filterNot { it.id == projectId } + updated)
                    .sortedByDescending { it.updatedAtMs }
                preferences[documentKey] = ProjectCodec.encode(document.copy(projects = next))
                changed = true
            }
        }
        return changed
    }

    private fun Project.toSummary(): DraftSummary = DraftSummary(
        id = id,
        name = name,
        updatedAtMs = updatedAtMs,
        durationMs = timeline.durationMs,
        aspectRatio = aspectRatio,
        coverAssetId = coverAssetId
    )

    private companion object {
        const val MAX_NAME_LENGTH = 80
    }
}
