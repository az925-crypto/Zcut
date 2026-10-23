package com.zaaam.zcut.core.data

import com.zaaam.zcut.core.domain.ProjectFactory
import com.zaaam.zcut.core.domain.TimelineEditor
import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.FilterConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectCodecTest {

    @Test
    fun roundTripPreservesTimelineAndEffects() {
        val base = ProjectFactory.create("Night walk")
        val videoTrack = base.timeline.tracks.first()
        val clip = Clip(
            assetId = "asset-1",
            startMs = 0L,
            durationMs = 1_200L,
            sourceDurationMs = 1_200L,
            filters = listOf(FilterConfig(id = "tokyo", intensity = 0.8f))
        )
        val project = base.copy(
            timeline = TimelineEditor.addClip(base.timeline, videoTrack.id, clip)
        )

        val decoded = ProjectCodec.decodeProjects(
            ProjectCodec.encodeProjects(listOf(project))
        )

        assertEquals(1, decoded.size)
        assertEquals("Night walk", decoded[0].name)
        assertEquals(1_200L, decoded[0].timeline.durationMs)
        assertEquals(
            "tokyo",
            decoded[0].timeline.tracks
                .flatMap { it.clips }
                .first()
                .filters
                .first()
                .id
        )
    }

    @Test
    fun invalidJsonFallsBackToEmptyDocument() {
        val decoded = ProjectCodec.decode("not-json")
        assertEquals(0, decoded.projects.size)
        assertTrue(decoded.schemaVersion >= 1)
    }

    @Test
    fun emptyPayloadReturnsEmptyDocument() {
        val decoded = ProjectCodec.decodeProjects(null)
        assertEquals(0, decoded.size)
    }

    @Test
    fun wrongRootTypesDoNotCrash() {
        val decoded = ProjectCodec.decode("""{"schemaVersion":"x","projects":{}}""")
        assertEquals(0, decoded.projects.size)
        assertEquals(1, decoded.schemaVersion)
    }

    @Test
    fun damagedProjectIsQuarantinedWithoutLosingValidOnes() {
        val raw = """{"schemaVersion":1,"projects":[{},{"frameRate":"not-an-int"}]}"""

        val decoded = ProjectCodec.decode(raw)

        assertEquals(1, decoded.projects.size)
        assertEquals("Untitled", decoded.projects.single().name)
        assertEquals(1, decoded.quarantinedProjects.size)
    }
}
