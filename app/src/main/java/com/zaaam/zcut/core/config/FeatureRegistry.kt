package com.zaaam.zcut.core.config

import kotlinx.serialization.Serializable

@Serializable
enum class BackendRequirement {
    NONE,
    OPTIONAL,
    REQUIRED
}

@Serializable
data class FeatureSpec(
    val id: String,
    val title: String,
    val requirement: BackendRequirement,
    val localFallback: Boolean,
    val note: String = ""
)

/**
 * Single source of truth for the 31 feature areas. The UI can render a
 * "BUTUH BACKEND" badge from this list instead of inventing paywalls.
 */
object FeatureRegistry {
    val all: List<FeatureSpec> = listOf(
        FeatureSpec("F01", "Draft & timeline", BackendRequirement.NONE, true),
        FeatureSpec("F02", "Media import", BackendRequirement.NONE, true),
        FeatureSpec("F03", "Preview player", BackendRequirement.NONE, true),
        FeatureSpec("F04", "Timeline editing", BackendRequirement.NONE, true),
        FeatureSpec("F05", "Speed & motion", BackendRequirement.NONE, true),
        FeatureSpec("F06", "Keyframes", BackendRequirement.NONE, true),
        FeatureSpec("F07", "Transitions", BackendRequirement.NONE, true),
        FeatureSpec("F08", "Filters & color", BackendRequirement.NONE, true),
        FeatureSpec("F09", "Video effects", BackendRequirement.NONE, true, "AR effects optional"),
        FeatureSpec("F10", "Mask & chroma", BackendRequirement.NONE, true),
        FeatureSpec("F11", "Matting & background", BackendRequirement.OPTIONAL, true),
        FeatureSpec("F12", "Beauty & retouch", BackendRequirement.NONE, true),
        FeatureSpec("F13", "Stickers & tracking", BackendRequirement.OPTIONAL, true),
        FeatureSpec("F14", "Text & typography", BackendRequirement.NONE, true),
        FeatureSpec("F15", "Captions & transcript", BackendRequirement.OPTIONAL, true),
        FeatureSpec("F16", "Audio editing", BackendRequirement.OPTIONAL, true),
        FeatureSpec("F17", "TTS & voice clone", BackendRequirement.OPTIONAL, true, "TTS local; clone backend"),
        FeatureSpec("F18", "AI music", BackendRequirement.OPTIONAL, true, "Procedural local loops"),
        FeatureSpec("F19", "Export & render", BackendRequirement.NONE, true),
        FeatureSpec("F20", "Drafts & cloud", BackendRequirement.OPTIONAL, true, "Local backup; cloud optional"),
        FeatureSpec("F21", "Templates & CutSame", BackendRequirement.OPTIONAL, true),
        FeatureSpec("F22", "AI clipper", BackendRequirement.OPTIONAL, true),
        FeatureSpec("F23", "AI agent", BackendRequirement.OPTIONAL, true),
        FeatureSpec("F24", "AI generation", BackendRequirement.REQUIRED, false),
        FeatureSpec("F25", "Digital human", BackendRequirement.REQUIRED, false),
        FeatureSpec("F26", "Camera & photo", BackendRequirement.NONE, true),
        FeatureSpec("F27", "Share & publish", BackendRequirement.OPTIONAL, true),
        FeatureSpec("F28", "Profile & settings", BackendRequirement.NONE, true),
        FeatureSpec("F29", "Resource & analytics", BackendRequirement.NONE, true, "No telemetry"),
        FeatureSpec("F30", "Split, gameplay & live photo", BackendRequirement.NONE, true),
        FeatureSpec("F31", "Ad maker", BackendRequirement.OPTIONAL, true)
    )

    private val byId: Map<String, FeatureSpec> = all.associateBy { it.id }

    fun get(id: String): FeatureSpec? = byId[id]

    fun backendRequired(): List<FeatureSpec> =
        all.filter { it.requirement == BackendRequirement.REQUIRED }

    fun backendOptional(): List<FeatureSpec> =
        all.filter { it.requirement == BackendRequirement.OPTIONAL }
}
