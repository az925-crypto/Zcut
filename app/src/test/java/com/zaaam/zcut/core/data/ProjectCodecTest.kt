package com.zaaam.zcut.core.data

import com.zaaam.zcut.core.domain.ProjectFactory
import com.zaaam.zcut.core.domain.TimelineEditor
import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.FilterConfig
import org.junit.Assert.assertEquals
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

        val decoded = ProjectCodec.decode(
            ProjectCodec.encode(ProjectDocument(projects = listOf(project)))
        ).projects

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
    fun invalidJsonFallsBackToEmptyDocumentAndKeepsRawPayload() {
        val decoded = ProjectCodec.decode("not-json")

        assertEquals(0, decoded.projects.size)
        assertEquals(1, decoded.schemaVersion)
        assertEquals(listOf("not-json"), decoded.quarantinedProjects)
    }

    @Test
    fun quarantinedPayloadSurvivesDecodeEncodeRoundTrip() {
        val first = ProjectCodec.decode("not-json")

        val second = ProjectCodec.decode(ProjectCodec.encode(first))

        assertEquals(0, second.projects.size)
        assertEquals(listOf("not-json"), second.quarantinedProjects)
    }

    @Test
    fun semanticallyInvalidClipsAreDroppedOnDecode() {
        val raw = """
            {"schemaVersion":1,"projects":[{"timeline":{"tracks":[
              {"type":"VIDEO","clips":[{"startMs":-100,"durationMs":50},
                                       {"startMs":0,"durationMs":100}]}
            ]}}]}
        """.trimIndent()

        val decoded = ProjectCodec.decode(raw)
        val clips = decoded.projects.single().timeline.tracks.first().clips

        assertEquals(1, clips.size)
        assertEquals(0L, clips.single().startMs)
        assertEquals(100L, decoded.projects.single().timeline.durationMs)
    }

    @Test
    fun emptyPayloadReturnsEmptyDocument() {
        val decoded = ProjectCodec.decode(null)
        assertEquals(0, decoded.projects.size)
    }

    @Test
    fun wrongRootTypesDoNotCrash() {
        val decoded = ProjectCodec.decode("""{"schemaVersion":"x","projects":{}}""")
        assertEquals(0, decoded.projects.size)
        assertEquals(1, decoded.schemaVersion)
    }

    @Test
    fun damagedQuarantineFieldIsPreservedNotDropped() {
        val raw = """{"projects":[],"quarantinedProjects":"oops"}"""

        val decoded = ProjectCodec.decode(raw)

        assertEquals(0, decoded.projects.size)
        assertEquals(listOf(raw), decoded.quarantinedProjects)
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
