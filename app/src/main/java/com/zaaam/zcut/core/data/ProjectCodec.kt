package com.zaaam.zcut.core.data

import com.zaaam.zcut.core.domain.TimelineEditor
import com.zaaam.zcut.core.model.Project
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
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
            // Container-level damage: keep the raw payload instead of reporting an
            // empty document, so the next write cannot silently erase every draft.
            ?: return ProjectDocument(quarantinedProjects = listOf(raw))

        val schemaVersion = (root["schemaVersion"] as? JsonPrimitive)
            ?.intOrNull
            ?: 1
        val projectsValue = root["projects"]
        val projectElements: List<JsonElement> = when (projectsValue) {
            null -> emptyList()
            is JsonArray -> projectsValue
            else -> return ProjectDocument(
                schemaVersion = schemaVersion,
                quarantinedProjects = listOf(raw)
            )
        }
        val quarantineValue = root["quarantinedProjects"]
        if (quarantineValue != null && quarantineValue !is JsonArray) {
            return ProjectDocument(
                schemaVersion = schemaVersion,
                quarantinedProjects = listOf(raw)
            )
        }
        val projects = mutableListOf<Project>()
        val quarantined = mutableListOf<String>()

        // Carry forward entries isolated by an earlier decode so a read-modify-write
        // cycle cannot drop them.
        (quarantineValue as? JsonArray)
            ?.mapNotNullTo(quarantined) { (it as? JsonPrimitive)?.content }

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

    private fun normalize(project: Project): Project =
        project.copy(timeline = TimelineEditor.sanitize(project.timeline))
}
