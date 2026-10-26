package com.zaaam.zcut.core.data

import com.zaaam.zcut.core.model.DraftSummary
import com.zaaam.zcut.core.model.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ProjectRepository {
    /** Every stored draft, including the ones sitting in the trash. */
    val allProjects: Flow<List<Project>>

    /** Active drafts only; every list/count outside the trash view uses this. */
    val projects: Flow<List<Project>>
        get() = allProjects.map { list -> list.filter { it.trashedAtMs == null } }

    suspend fun get(projectId: String): Project?

    suspend fun upsert(project: Project)

    suspend fun delete(projectId: String)

    suspend fun duplicate(projectId: String): Project?

    suspend fun summaries(): List<DraftSummary>

    /** Renames an active or trashed draft; false when the name is blank. */
    suspend fun rename(projectId: String, name: String): Boolean

    /** Soft delete: moves a draft to the trash. */
    suspend fun trash(projectId: String): Boolean

    /** Brings a trashed draft back to the list. */
    suspend fun restore(projectId: String): Boolean

    /** Removes every trashed draft for good; returns how many were removed. */
    suspend fun emptyTrash(): Int
}
