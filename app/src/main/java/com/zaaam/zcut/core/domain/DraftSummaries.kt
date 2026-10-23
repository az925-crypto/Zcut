package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.AspectRatio
import com.zaaam.zcut.core.model.DraftSummary
import com.zaaam.zcut.core.model.Project

/**
 * Maps stored projects to the lightweight summaries the draft lists render.
 * Also fixes the ordering: newest edit first, with a deterministic tie-break so
 * two projects saved in the same millisecond never swap positions between
 * recompositions.
 */
object DraftSummaries {

    private val projectOrder =
        compareByDescending<Project> { it.updatedAtMs }
            .thenByDescending { it.createdAtMs }
            .thenBy { it.id }

    fun from(projects: List<Project>): List<DraftSummary> =
        projects.sortedWith(projectOrder).map { it.toSummary() }

    fun Project.toSummary(): DraftSummary = DraftSummary(
        id = id,
        name = name,
        updatedAtMs = updatedAtMs,
        durationMs = timeline.durationMs,
        aspectRatio = aspectRatio,
        coverAssetId = coverAssetId,
        hasExport = false
    )
}

/** Pure filtering used by the Projects screen. */
object ProjectsFilter {

    fun apply(
        drafts: List<DraftSummary>,
        query: String,
        aspectRatio: AspectRatio?
    ): List<DraftSummary> {
        val needle = query.trim().lowercase()
        return drafts.filter { draft ->
            val matchesQuery = needle.isEmpty() || draft.name.lowercase().contains(needle)
            val matchesAspect = aspectRatio == null || draft.aspectRatio == aspectRatio
            matchesQuery && matchesAspect
        }
    }
}
