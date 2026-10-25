package com.zaaam.zcut.core.data

import com.zaaam.zcut.core.domain.ProjectFactory
import com.zaaam.zcut.core.domain.TimelineEditor
import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.FilterConfig
import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.core.model.MediaKind
import com.zaaam.zcut.core.model.TextStylePreset
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

    @Test
    fun assetsRoundTripWithTheirPrivatePaths() {
        val asset = MediaAsset(
            uri = "content://media/1",
            displayName = "clip.mp4",
            mimeType = "video/mp4",
            kind = MediaKind.VIDEO,
            durationMs = 1_200L,
            localPath = "/data/files/media/1.mp4",
            thumbnailPath = "thumbs/1.jpg",
            rotationDegrees = 90
        )
        val project = ProjectFactory.fromAssets("Assets", listOf(asset))

        val decoded = ProjectCodec.decode(
            ProjectCodec.encode(ProjectDocument(projects = listOf(project)))
        ).projects.single()

        val stored = decoded.assets.single()
        assertEquals(asset.id, stored.id)
        assertEquals("/data/files/media/1.mp4", stored.localPath)
        assertEquals("thumbs/1.jpg", stored.thumbnailPath)
        assertEquals(90, stored.rotationDegrees)
        assertEquals(asset.id, decoded.timeline.tracks.flatMap { it.clips }.single().assetId)
    }

    @Test
    fun legacyDocumentsWithoutAssetsStillDecode() {
        val raw = """
            {"schemaVersion":1,"projects":[{"id":"p1","timeline":{"tracks":[
              {"type":"VIDEO","clips":[{"assetId":"gone","startMs":0,"durationMs":500}]}
            ]}}]}
        """.trimIndent()

        val decoded = ProjectCodec.decode(raw).projects.single()

        assertEquals(0, decoded.assets.size)
        assertEquals(500L, decoded.timeline.durationMs)
        assertEquals("gone", decoded.timeline.tracks.flatMap { it.clips }.single().assetId)
    }

    @Test
    fun unusableAndDuplicateAssetsAreDroppedButTheirClipsAreKept() {
        val raw = """
            {"schemaVersion":1,"projects":[{
              "id":"p1",
              "assets":[
                {"id":"a1","uri":"content://a","displayName":"a","kind":"VIDEO"},
                {"id":"a1","uri":"content://b","displayName":"b","kind":"VIDEO"},
                {"id":"","uri":"content://c","displayName":"c","kind":"VIDEO"},
                {"id":"a2","displayName":"no-source","kind":"VIDEO"}
              ],
              "timeline":{"tracks":[
                {"type":"VIDEO","clips":[{"assetId":"missing","startMs":0,"durationMs":500}]}
              ]}
            }]}
        """.trimIndent()

        val decoded = ProjectCodec.decode(raw).projects.single()

        assertEquals(1, decoded.assets.size)
        assertEquals("a1", decoded.assets.single().id)
        // Missing media is a UI placeholder, never a reason to delete user clips.
        assertEquals(1, decoded.timeline.tracks.flatMap { it.clips }.size)
    }

    @Test
    fun assetWithOnlyAPrivateCopyIsStillUsable() {
        val raw = """
            {"schemaVersion":1,"projects":[{
              "id":"p1",
              "assets":[{
                "id":"a1",
                "displayName":"clip.mp4",
                "kind":"VIDEO",
                "localPath":"/data/files/media/a1.mp4"
              }]
            }]}
        """.trimIndent()

        val decoded = ProjectCodec.decode(raw).projects.single()

        assertEquals("/data/files/media/a1.mp4", decoded.assets.single().localPath)
    }

    @Test
    fun textConfigWithoutM11FieldsDecodesWithDefaults() {
        val raw = """
            {"schemaVersion":1,"projects":[{"id":"p1","timeline":{"tracks":[
              {"type":"TEXT","clips":[{
                "type":"TEXT","startMs":0,"durationMs":1000,
                "text":{"text":"Halo"}
              }]}
            ]}}]}
        """.trimIndent()

        val decoded = ProjectCodec.decode(raw).projects.single()
        val text = decoded.timeline.tracks
            .first { it.type.name == "TEXT" }
            .clips
            .single()
            .text!!

        assertEquals("Halo", text.text)
        assertEquals(TextStylePreset.NONE, text.stylePreset)
        assertEquals(0f, text.letterSpacingEm, 0.001f)
        assertEquals(1.2f, text.lineHeight, 0.001f)
        assertEquals("sans", text.fontFamily)
    }
}
