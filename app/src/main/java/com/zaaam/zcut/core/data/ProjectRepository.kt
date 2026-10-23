package com.zaaam.zcut.core.data

import com.zaaam.zcut.core.model.DraftSummary
import com.zaaam.zcut.core.model.Project
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    val projects: Flow<List<Project>>

    suspend fun get(projectId: String): Project?

    suspend fun upsert(project: Project)

    suspend fun delete(projectId: String)

    suspend fun duplicate(projectId: String): Project?

    suspend fun summaries(): List<DraftSummary>
}
