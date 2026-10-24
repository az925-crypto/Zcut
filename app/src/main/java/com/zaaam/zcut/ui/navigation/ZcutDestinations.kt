package com.zaaam.zcut.ui.navigation

import com.zaaam.zcut.core.config.FeatureRegistry

/** Route constants for the destinations referenced from code. */
object ZcutRoutes {
    const val Splash = "splash"
    const val Onboarding = "onboarding"
    const val Home = "home"
    const val Projects = "projects"
    const val Editor = "editor"
    const val MediaPicker = "media-picker"
    const val Trim = "trim"
    const val Speed = "speed"
    const val Audio = "audio"
    const val Text = "text"
    const val Stickers = "stickers"
    const val Effects = "effects"
    const val Filters = "filters"
    const val Adjust = "adjust"
    const val Mask = "mask"
    const val Export = "export"
    const val Templates = "templates"
    const val AiStudio = "ai-studio"
    const val Profile = "profile"
    const val Settings = "settings"

    /** NavHost pattern; the destination id itself stays `editor` for parity. */
    const val EditorPattern = "editor/{projectId}"

    /** `projectId` is absent when the picker starts a brand-new draft. */
    const val MediaPickerPattern = "media-picker?projectId={projectId}"

    const val ArgProjectId = "projectId"

    fun editor(projectId: String): String = "$Editor/${encode(projectId)}"

    fun mediaPicker(projectId: String? = null): String =
        if (projectId.isNullOrBlank()) {
            MediaPicker
        } else {
            "$MediaPicker?$ArgProjectId=${encode(projectId)}"
        }

    private fun encode(value: String): String =
        java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}

/**
 * One entry per approved Stage 2 mockup screen. Routes are the mockup ids, so
 * the parity test can assert that the Android graph covers all 42 screens.
 *
 * `implemented` flips to true as each milestone lands; everything else is
 * routed to an honest "coming in a later milestone" placeholder rather than
 * being hidden or paywalled.
 */
data class ZcutDestination(
    val route: String,
    val title: String,
    val featureId: String?,
    val implemented: Boolean = false
)

object ZcutDestinations {

    val all: List<ZcutDestination> = listOf(
        ZcutDestination("splash", "Zcut", null, implemented = true),
        ZcutDestination("onboarding", "Selamat datang", null, implemented = true),
        ZcutDestination("home", "Home", "F01", implemented = true),
        ZcutDestination("projects", "Draft", "F20", implemented = true),
        ZcutDestination("templates", "Template", "F21"),
        ZcutDestination("template-detail", "Detail template", "F21"),
        ZcutDestination("discover", "Discover", "F21"),
        ZcutDestination("ai-studio", "AI Studio", "F23"),
        ZcutDestination("editor", "Editor", "F01", implemented = true),
        ZcutDestination("media-picker", "Tambah media", "F02", implemented = true),
        ZcutDestination("trim", "Trim & split", "F04"),
        ZcutDestination("speed", "Speed & curve", "F05"),
        ZcutDestination("keyframe", "Keyframe", "F06"),
        ZcutDestination("transition", "Transisi", "F07"),
        ZcutDestination("filters", "Filter", "F08"),
        ZcutDestination("adjust", "Adjust", "F08"),
        ZcutDestination("effects", "Efek", "F09"),
        ZcutDestination("mask", "Mask & chroma", "F10"),
        ZcutDestination("matting", "Cutout", "F11"),
        ZcutDestination("beauty", "Retouch", "F12"),
        ZcutDestination("stickers", "Sticker", "F13"),
        ZcutDestination("text", "Teks", "F14"),
        ZcutDestination("captions", "Caption", "F15"),
        ZcutDestination("audio", "Audio", "F16"),
        ZcutDestination("tts", "Text to speech", "F17"),
        ZcutDestination("ai-music", "AI Music", "F18"),
        ZcutDestination("export", "Export", "F19"),
        ZcutDestination("export-done", "Export selesai", "F19"),
        ZcutDestination("drafts", "Kelola draft", "F20"),
        ZcutDestination("ai-clipper", "AI Clipper", "F22"),
        ZcutDestination("ai-agent", "Zcut Agent", "F23"),
        ZcutDestination("ai-generate", "Generate", "F24"),
        ZcutDestination("digital-human", "Digital Human", "F25"),
        ZcutDestination("camera", "Kamera", "F26"),
        ZcutDestination("photo-editor", "Photo editor", "F26"),
        ZcutDestination("share", "Bagikan", "F27"),
        ZcutDestination("profile", "Profil", "F28", implemented = true),
        ZcutDestination("settings", "Pengaturan", "F29", implemented = true),
        ZcutDestination("split-screen", "Split screen", "F30"),
        ZcutDestination("gameplay", "Gameplay", "F30"),
        ZcutDestination("live-photo", "Live photo", "F30"),
        ZcutDestination("ad-maker", "Ad Maker", "F31")
    )

    private val byRoute: Map<String, ZcutDestination> = all.associateBy { it.route }

    fun get(route: String): ZcutDestination? = byRoute[route]

    val placeholders: List<ZcutDestination> = all.filterNot { it.implemented }

    /** Routes shown in the bottom navigation bar, in order. */
    val bottomNav: List<String> = listOf(
        ZcutRoutes.Home,
        ZcutRoutes.Templates,
        ZcutRoutes.Projects,
        ZcutRoutes.AiStudio,
        ZcutRoutes.Profile
    )

    fun bottomNavTitle(route: String): String =
        when (route) {
            ZcutRoutes.Templates -> "Template"
            ZcutRoutes.Projects -> "Draft"
            ZcutRoutes.AiStudio -> "AI"
            ZcutRoutes.Profile -> "Profil"
            else -> "Home"
        }

    /** Feature requirement for a route, used by [com.zaaam.zcut.ui.components.BackendBadge]. */
    fun backendRequirement(route: String) =
        get(route)?.featureId?.let { FeatureRegistry.get(it)?.requirement }
}
