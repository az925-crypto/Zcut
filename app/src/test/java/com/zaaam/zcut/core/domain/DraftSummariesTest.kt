package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.AspectRatio
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.model.Timeline
import org.junit.Assert.assertEquals
import org.junit.Test

class DraftSummariesTest {

    private fun project(
        id: String,
        updatedAtMs: Long,
        createdAtMs: Long = 0L,
        name: String = "Draft $id",
        aspectRatio: AspectRatio = AspectRatio.PORTRAIT_9_16,
        durationMs: Long = 0L
    ) = Project(
        id = id,
        name = name,
        createdAtMs = createdAtMs,
        updatedAtMs = updatedAtMs,
        aspectRatio = aspectRatio,
        timeline = Timeline(durationMs = durationMs)
    )

    @Test
    fun ordersByNewestEditFirst() {
        val result = DraftSummaries.from(
            listOf(
                project("a", updatedAtMs = 100L),
                project("b", updatedAtMs = 300L),
                project("c", updatedAtMs = 200L)
            )
        )
        assertEquals(listOf("b", "c", "a"), result.map { it.id })
    }

    @Test
    fun breaksUpdatedAtTiesByCreationThenId() {
        val result = DraftSummaries.from(
            listOf(
                project("z", updatedAtMs = 500L, createdAtMs = 1L),
                project("a", updatedAtMs = 500L, createdAtMs = 1L),
                project("m", updatedAtMs = 500L, createdAtMs = 9L)
            )
        )
        assertEquals(listOf("m", "a", "z"), result.map { it.id })
    }

    @Test
    fun summaryMirrorsProjectFields() {
        val summary = DraftSummaries.from(
            listOf(
                project(
                    id = "p1",
                    updatedAtMs = 42L,
                    name = "Night walk",
                    aspectRatio = AspectRatio.SQUARE_1_1,
                    durationMs = 1_500L
                )
            )
        ).single()

        assertEquals("p1", summary.id)
        assertEquals("Night walk", summary.name)
        assertEquals(42L, summary.updatedAtMs)
        assertEquals(1_500L, summary.durationMs)
        assertEquals(AspectRatio.SQUARE_1_1, summary.aspectRatio)
    }

    @Test
    fun filterMatchesQueryCaseInsensitively() {
        val drafts = DraftSummaries.from(
            listOf(
                project("a", updatedAtMs = 2L, name = "Sunset Reel"),
                project("b", updatedAtMs = 1L, name = "Vlog Pagi")
            )
        )

        assertEquals(listOf("a"), ProjectsFilter.apply(drafts, "sunset", null).map { it.id })
        assertEquals(listOf("b"), ProjectsFilter.apply(drafts, "  PAGI ", null).map { it.id })
        assertEquals(2, ProjectsFilter.apply(drafts, "", null).size)
    }

    @Test
    fun filterByAspectRatio() {
        val drafts = DraftSummaries.from(
            listOf(
                project("a", updatedAtMs = 2L, aspectRatio = AspectRatio.SQUARE_1_1),
                project("b", updatedAtMs = 1L, aspectRatio = AspectRatio.LANDSCAPE_16_9)
            )
        )

        assertEquals(listOf("a"), ProjectsFilter.apply(drafts, "", AspectRatio.SQUARE_1_1).map { it.id })
        assertEquals(2, ProjectsFilter.apply(drafts, "", null).size)
    }

    @Test
    fun filterReturnsEmptyWhenNothingMatches() {
        val drafts = DraftSummaries.from(listOf(project("a", updatedAtMs = 1L, name = "Alpha")))
        assertEquals(0, ProjectsFilter.apply(drafts, "zzz", null).size)
        assertEquals(0, ProjectsFilter.apply(drafts, "", AspectRatio.PORTRAIT_4_5).size)
    }
}
