package com.zaaam.zcut.core.data

import com.zaaam.zcut.core.domain.TimelineEditor
import com.zaaam.zcut.core.model.Project
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

@Serializable
data class ProjectDocument(
    val schemaVersion: Int = 1,
    val projects: List<Project> = emptyList(),
    /** Raw JSON for projects that failed validation; preserved so a rewrite does not erase them. */
    val quarantinedProjects: List<String> = emptyList()
)

/**
 * Versioned JSON envelope for local drafts. Projects are decoded one at a time
 * so one damaged project cannot erase every other draft on the device.
 */
object ProjectCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    fun encode(document: ProjectDocument): String =
        json.encodeToString(ProjectDocument.serializer(), document)

    fun decode(raw: String?): ProjectDocument {
        if (raw.isNullOrBlank()) return ProjectDocument()
        val root = runCatching { json.parseToJsonElement(raw) }
            .getOrNull() as? JsonObject
            ?: return ProjectDocument()

        val schemaVersion = (root["schemaVersion"] as? JsonPrimitive)
            ?.intOrNull
            ?: 1
        val projectElements = (root["projects"] as? JsonArray).orEmpty()
        val projects = mutableListOf<Project>()
        val quarantined = mutableListOf<String>()

        projectElements.forEach { element ->
            val project = runCatching {
                json.decodeFromJsonElement(Project.serializer(), element)
            }.getOrNull()
            if (project == null) {
                quarantined += element.toString()
            } else {
                projects += normalize(project)
            }
        }

        return ProjectDocument(
            schemaVersion = schemaVersion,
            projects = projects,
            quarantinedProjects = quarantined
        )
    }

    fun encodeProjects(projects: List<Project>): String =
        encode(ProjectDocument(projects = projects))

    fun decodeProjects(raw: String?): List<Project> =
        decode(raw).projects

    private fun normalize(project: Project): Project =
        project.copy(timeline = TimelineEditor.recalculate(project.timeline))
}
