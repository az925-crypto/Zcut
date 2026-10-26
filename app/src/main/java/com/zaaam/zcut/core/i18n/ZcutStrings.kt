package com.zaaam.zcut.core.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import com.zaaam.zcut.core.data.AppLanguage
import java.util.Locale

/**
 * In-app copy for the primary shell.
 *
 * The mockup ships an ID/EN switch, so the preference must do something real.
 * These are the strings for the navigation, onboarding and the five top-level
 * screens; the editor tool panels keep their Indonesian labels in this build
 * and are documented as a limitation. Keeping the table in Kotlin (instead of
 * Android resources) means the switch works on every supported API without an
 * AppCompat dependency and without recreating the activity.
 */
enum class ZcutText(val id: String, val en: String) {
    // Navigation and shared actions.
    NavHome("Home", "Home"),
    NavTemplates("Template", "Templates"),
    NavDrafts("Draft", "Drafts"),
    NavAi("AI", "AI"),
    NavProfile("Profil", "Profile"),
    Back("Kembali", "Back"),
    Close("Tutup", "Close"),
    Cancel("Batal", "Cancel"),
    Save("Simpan", "Save"),
    Delete("Hapus", "Delete"),
    Rename("Ganti nama", "Rename"),
    Duplicate("Duplikat", "Duplicate"),
    Import("Impor", "Import"),
    EmptyTrash("Kosongkan", "Empty trash"),
    NewProject("Project baru", "New project"),
    SeeAll("Lihat semua", "See all"),
    SearchDrafts("Cari draft", "Search drafts"),
    All("Semua", "All"),
    Trash("Sampah", "Trash"),

    // Home.
    HomeSubtitle("Studio video lokal · tanpa watermark", "Local video studio · no watermark"),
    HeroTitle("Edit video tanpa batas", "Edit without limits"),
    HeroMessage(
        "Semua fitur terbuka, diproses di perangkat, hasil tanpa watermark.",
        "Every feature unlocked, processed on device, no watermark."
    ),
    ToolCreate("Tool create", "Create tools"),
    RecentDrafts("Draft terakhir", "Recent drafts"),
    NoDraftsTitle("Belum ada draft", "No drafts yet"),
    NoDraftsMessage(
        "Mulai project baru untuk membuat draft pertama di perangkat ini.",
        "Start a new project to create your first draft on this device."
    ),
    ToolAi("Tool AI", "AI tools"),
    QuickVideo("Video", "Video"),
    QuickPhoto("Photo", "Photo"),
    QuickAiVideo("AI video", "AI video"),
    QuickTemplate("Template", "Template"),
    CreateSplit("Split screen", "Split screen"),
    CreateSplitBlurb("Grid still", "Grid still"),
    CreateGameplay("Gameplay", "Gameplay"),
    CreateGameplayBlurb("Facecam", "Facecam"),
    CreateLivePhoto("Live photo", "Live photo"),
    CreateLivePhotoBlurb("Motion photo", "Motion photo"),

    // Settings.
    SettingsTitle("Pengaturan", "Settings"),
    SettingsSubtitle("Preferensi lokal", "Local preferences"),
    NoAds("TANPA IKLAN", "NO ADS"),
    Appearance("Tampilan", "Appearance"),
    Theme("Tema", "Theme"),
    Language("Bahasa", "Language"),
    LanguageSubtitle("Indonesia · English", "Indonesia · English"),
    Storage("Penyimpanan", "Storage"),
    Workspace("Ruang kerja", "Workspace"),
    DraftCount("%d draft", "%d drafts"),
    Clean("Bersihkan", "Clean"),
    Backup("Cadangan", "Backup"),
    BackupSubtitle("Zip lokal · tanpa cloud", "Local zip · no cloud"),
    Local("LOKAL", "LOCAL"),
    ExportDefaults("Default ekspor", "Export defaults"),
    Resolution("Resolusi", "Resolution"),
    Audio("Audio", "Audio"),
    Privacy("Privasi", "Privacy"),
    Telemetry("Telemetri", "Telemetry"),
    TelemetrySubtitle("Nonaktif · tanpa SDK analytics", "Off · no analytics SDK"),
    NetworkAccess("Akses jaringan", "Network access"),
    NoNetworkSubtitle(
        "Tidak ada izin jaringan di build ini",
        "No network permission in this build"
    ),
    None("TIDAK ADA", "NONE"),
    Removed("DIHAPUS", "REMOVED"),
    Billing("Billing / iklan / kredit", "Billing / ads / credits"),
    MonetizationNote(
        "Modul monetisasi sengaja tidak direkonstruksi. Semua fitur tetap terbuka.",
        "Monetization modules were intentionally not reconstructed. Every feature stays unlocked."
    ),
    FrameRate("Frame rate", "Frame rate"),
    ThemeDark("Gelap", "Dark"),
    ThemeLight("Terang", "Light"),
    ThemeSystem("Ikuti sistem", "Follow system"),
    ThemeDarkBadge("GELAP", "DARK"),
    ThemeLightBadge("TERANG", "LIGHT"),
    ThemeSystemBadge("SISTEM", "SYSTEM"),
    ResolutionAndFrameRate("Resolusi & frame rate", "Resolution & frame rate"),

    // Profile.
    ProfileTitle("Profil", "Profile"),
    ProfileSubtitle("Profil lokal · tanpa akun", "Local profile · no account"),
    EditProfile("Edit profil", "Edit profile"),
    DisplayName("Nama tampilan", "Display name"),
    LocalCreator("Kreator lokal", "Local creator"),
    OnDeviceOnly("Hanya di perangkat ini", "On this device only"),
    Free("GRATIS", "FREE"),
    AllFeatures("Semua fitur terbuka", "Every feature unlocked"),
    NoSubscriptionWatermark(
        "Tanpa langganan, kredit, atau watermark.",
        "No subscription, credits, or watermark."
    ),
    TemplatesRow("Template", "Templates"),
    TemplatesRowSubtitle("Paket lokal + impor sendiri", "Local packs + your own imports"),
    DraftsRow("Draft", "Drafts"),
    DraftsRowSubtitle("Cadangkan dan pulihkan", "Back up and restore"),
    AiStudioRow("AI Studio", "AI Studio"),
    AiStudioRowSubtitle("Alat lokal + opsi backend", "Local tools + optional backend"),
    SettingsRow("Pengaturan", "Settings"),
    SettingsRowSubtitle("Tema, penyimpanan, privasi", "Theme, storage, privacy"),
    About("Tentang Zcut", "About Zcut"),
    AboutSubtitle("v%s · clean-room build", "v%s · clean-room build"),
    NoLogin(
        "Tidak ada login, iklan, atau billing di build ini.",
        "No login, ads, or billing in this build."
    ),
    StatDrafts("Draft", "Drafts"),
    StatExports("Ekspor", "Exports"),
    StatUsed("Terpakai", "Used"),

    // Draft list.
    DraftsTitle("Draft", "Drafts"),
    TrashTitle("Sampah", "Trash"),
    TrashCount("%d draft di sampah", "%d drafts in trash"),
    StoredCount("%d project tersimpan di perangkat", "%d projects stored on device"),
    TrashChip("Sampah (%d)", "Trash (%d)"),
    EmptyTrashTitle("Sampah kosong", "Trash is empty"),
    EmptyTrashMessage(
        "Draft yang dipindah ke sampah muncul di sini dan bisa dipulihkan.",
        "Drafts moved to the trash appear here and can be restored."
    ),
    NoResults("Tidak ada hasil", "No results"),
    NoResultsMessage(
        "Coba ubah kata kunci atau filter aspek.",
        "Try another keyword or aspect filter."
    ),
    Restore("Pulihkan", "Restore"),
    DeleteForever("Hapus permanen", "Delete forever"),
    BackupZip("Backup .zip", "Back up .zip"),
    MoveToTrash("Pindah ke sampah", "Move to trash"),
    MoveTrashTitle("Pindah ke sampah?", "Move to trash?"),
    MoveTrashMessage(
        "\"%s\" akan dipindah ke sampah dan bisa dipulihkan dari tab Sampah.",
        "\"%s\" will move to the trash and can be restored from the Trash tab."
    ),
    Move("Pindahkan", "Move"),
    DeleteForeverTitle("Hapus permanen?", "Delete forever?"),
    DeleteForeverMessage(
        "\"%s\" akan dihapus permanen dari perangkat ini.",
        "\"%s\" will be permanently deleted from this device."
    ),
    RenameDraftTitle("Ganti nama draft", "Rename draft"),
    Name("Nama", "Name"),

    // Onboarding.
    Skip("Lewati", "Skip"),
    Next("Lanjut", "Next"),
    Start("Mulai", "Start"),
    OnboardingOneTitle("Semua fitur terbuka", "Every feature unlocked"),
    OnboardingOneMessage(
        "Tidak ada langganan, kredit, iklan, atau watermark. Semua tool aktif sejak awal.",
        "No subscription, credits, ads, or watermark. Every tool is active from the start."
    ),
    OnboardingTwoTitle("Diproses di perangkat", "Processed on device"),
    OnboardingTwoMessage(
        "Edit, render, dan simpan berjalan lokal. Draft tidak pernah dikirim ke server mana pun.",
        "Editing, rendering and saving run locally. Drafts are never sent to any server."
    ),
    OnboardingThreeTitle("Siap dibagikan", "Ready to share"),
    OnboardingThreeMessage(
        "Ekspor resolusi penuh lalu bagikan langsung ke aplikasi favoritmu.",
        "Export at full resolution, then share straight to your favourite app."
    ),

    // Templates and AI Studio copy that is not already English.
    TemplatesSubtitle(
        "Local packs · tanpa gembok marketplace",
        "Local packs · no marketplace lock"
    ),
    MarketplaceNote(
        "Marketplace dan AI template tidak memanggil server asli. Template bawaan " +
            "tetap lokal; publikasi komunitas ditandai BUTUH BACKEND.",
        "Marketplace and AI templates never call the original servers. Bundled " +
            "templates stay local; community publishing is marked BACKEND ONLY."
    ),
    AiStudioSubtitle(
        "Local tools first · backend only when needed",
        "Local tools first · backend only when needed"
    ),
    AiStudioNote(
        "Model generatif, voice clone, dan lip sync membutuhkan backend/model " +
            "sendiri. Tool tetap terbuka; tidak ada paywall atau kredit.",
        "Generative models, voice clone and lip sync need their own backend/model. " +
            "Tools stay open; there is no paywall or credits."
    ),
    BackendNeedsServer("BACKEND = perlu server", "BACKEND = needs server")
}

/** True when the copy should be English. */
val LocalZcutLanguage = staticCompositionLocalOf { AppLanguage.ID }

/** Picks the active copy for [text]. */
fun zt(text: ZcutText, language: AppLanguage = AppLanguage.ID): String =
    if (language == AppLanguage.EN) text.en else text.id

/**
 * Same as [zt] but formats `%s`/`%d` placeholders. The template is app-owned,
 * so formatting with [Locale.ROOT] cannot leak a device locale into the copy.
 */
fun ztf(text: ZcutText, language: AppLanguage, vararg args: Any): String =
    String.format(Locale.ROOT, zt(text, language), *args)

/**
 * Screen titles/subtitles that are static literals in the tool panels. The
 * lookup only translates known app copy; a dynamic title (a project name, a
 * template name) falls through untouched.
 */
object ZcutScreenText {

    private val titles = mapOf(
        "Pengaturan" to "Settings",
        "Profil" to "Profile",
        "Draft" to "Drafts",
        "Sampah" to "Trash",
        "Kamera" to "Camera",
        "Tambah media" to "Add media",
        "Template" to "Template",
        "Share" to "Share",
        "Export" to "Export",
        "Captions" to "Captions",
        "Text to speech" to "Text to speech",
        "Text to image / video" to "Text to image / video"
    )

    private val subtitles = mapOf(
        "Preferensi lokal" to "Local preferences",
        "Profil lokal · tanpa akun" to "Local profile · no account",
        "Studio video lokal · tanpa watermark" to "Local video studio · no watermark",
        "Render lokal · tanpa watermark" to "Local render · no watermark",
        "Local packs · tanpa gembok marketplace" to "Local packs · no marketplace lock",
        "Game + facecam overlay" to "Game + facecam overlay",
        "Motion photo toolkit" to "Motion photo toolkit",
        "Script → first cut · local" to "Script → first cut · local",
        "Share sheet · no watermark" to "Share sheet · no watermark",
        "Local photo editor" to "Local photo editor"
    )

    fun title(raw: String, language: AppLanguage): String =
        if (language == AppLanguage.EN) titles[raw] ?: raw else raw

    fun subtitle(raw: String, language: AppLanguage): String =
        if (language == AppLanguage.EN) subtitles[raw] ?: raw else raw
}
