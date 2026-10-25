package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.BeautyConfig

/**
 * Shipped beauty groups, read from
 * `assets/local_beauty_all.json` → `beauty_all_info.category[]`
 * (`name` + `report_name`) of the analysed artifact.
 */
enum class BeautyGroup(val shippedName: String, val reportName: String) {
    SMOOTH("Smooth skin", "skin_grinding"),
    CONCEALER("Concealer", "concealer"),
    SHAPE("Shape", "beauty_face"),
    MAKEUP("Makeup", "beauty_makeup")
}

/**
 * One beauty slider: the label comes from the approved mockup, everything else
 * from the shipped config (`category[].items[]` → `key`, `relative_min_value`,
 * `relative_max_value`). `type_*` items are named `type_auto_beauty`,
 * `type_beauty_face`, `type_beauty_makeup` there; that split is what decides how
 * a 0–100 slider position maps to an engine value (see [toSdk]).
 *
 * Shipped evidence for every row below (key → min/max):
 * `auto_buffing` 0–0.8, `auto_balance_skin` 0–0.8, `thin_face` 0.5–0.8,
 * `eye_size` 0.5–0.8, `nose_wing` 0.5–0.6, `type_lips` 0–1.
 */
enum class BeautySlider(
    val label: String,
    val key: String,
    val group: BeautyGroup,
    val relativeMin: Float,
    val relativeMax: Float
) {
    SMOOTH("Skin smooth", "auto_buffing", BeautyGroup.SMOOTH, 0f, 0.8f),
    WHITEN("Whiten", "auto_balance_skin", BeautyGroup.SMOOTH, 0f, 0.8f),
    FACE_SLIM("Face slim", "thin_face", BeautyGroup.SHAPE, 0.5f, 0.8f),
    EYES("Eyes", "eye_size", BeautyGroup.SHAPE, 0.5f, 0.8f),
    NOSE("Nose", "nose_wing", BeautyGroup.SHAPE, 0.5f, 0.6f),
    LIPS("Lips", "type_lips", BeautyGroup.MAKEUP, 0f, 1f);

    /**
     * A shipped range that starts above zero is a neutral baseline, not "off":
     * the engine expects 0.5 to mean "no reshape" for `thin_face`/`eye_size`, so
     * the slider there spans neutral → shipped maximum.
     */
    val neutralBaseline: Boolean get() = relativeMin > 0f

    /**
     * Engine value for a 0–100 slider position.
     *
     * - auto-beauty / makeup items (range starts at 0) send the position itself,
     *   capped by the shipped range: the shipped config stores `_ui_value: 40`
     *   with `_sdk_value: 0.4`, i.e. position/100.
     * - shape items (range starts at the neutral baseline) interpolate across
     *   the shipped range, so 0 = no change and 100 = the shipped maximum.
     *
     * The shipped presets also store shape values that fit neither rule (29 →
     * 0.65 is `0.5 + 29/200`); the exact original mapping is not recoverable
     * from the config alone, so it is left as a documented Unknown and the
     * range-spanning rule is used instead. See `docs/features/12-retouch-beauty.md`.
     */
    fun toSdk(ui: Float): Float {
        val position = position(ui)
        return if (neutralBaseline) {
            relativeMin + position * (relativeMax - relativeMin)
        } else {
            position.coerceIn(relativeMin, relativeMax)
        }
    }

    /** Slider fraction 0..1; what the local pixel operations scale by. */
    fun strength(ui: Float): Float = position(ui)

    fun uiValue(config: BeautyConfig): Float = when (this) {
        SMOOTH -> config.smooth
        WHITEN -> config.whiten
        FACE_SLIM -> config.faceSlim
        EYES -> config.eyes
        NOSE -> config.nose
        LIPS -> config.lips
    }

    fun withValue(config: BeautyConfig, ui: Float): BeautyConfig {
        val value = if (ui.isNaN()) 0f else ui.coerceIn(0f, 100f)
        return when (this) {
            SMOOTH -> config.copy(smooth = value)
            WHITEN -> config.copy(whiten = value)
            FACE_SLIM -> config.copy(faceSlim = value)
            EYES -> config.copy(eyes = value)
            NOSE -> config.copy(nose = value)
            LIPS -> config.copy(lips = value)
        }
    }

    private fun position(ui: Float): Float =
        if (ui.isNaN()) 0f else ui.coerceIn(0f, 100f) / 100f
}

/**
 * Shipped beauty keys that the approved mockup has no slot for yet. They are
 * kept here (with their shipped range) so the next milestone reuses the real
 * names instead of inventing new ones; they are not exposed in the UI.
 *
 * Evidence: `assets/local_beauty_all.json` → `beauty_all_info.category[].items[]`.
 */
data class ShippedBeautyKey(
    val key: String,
    val group: BeautyGroup,
    val relativeMin: Float,
    val relativeMax: Float
)

/**
 * Pure beauty helpers: clamping for a config that arrives from a draft file
 * (untrusted), the shipped key table, the face label and the mixing factors the
 * local pixel operations use. No Android types, so all of it is unit-tested.
 */
object BeautyEditor {

    /** The six sliders of the approved mockup, in mockup order. */
    val FACE_SLIDERS: List<BeautySlider> = BeautySlider.entries.toList()

    /** Shipped keys with no approved UI slot yet (order follows the config). */
    val SHIPPED_KEYS: List<ShippedBeautyKey> = listOf(
        ShippedBeautyKey("auto_dark_circle", BeautyGroup.CONCEALER, 0f, 1f),
        ShippedBeautyKey("auto_eye_bright", BeautyGroup.CONCEALER, 0f, 0.6f),
        ShippedBeautyKey("auto_oil_remove", BeautyGroup.CONCEALER, 0f, 0.8f),
        ShippedBeautyKey("face_little_head", BeautyGroup.SHAPE, 0f, 0.6f),
        ShippedBeautyKey("type_makeup_all", BeautyGroup.MAKEUP, 0f, 1f),
        ShippedBeautyKey("type_blush", BeautyGroup.MAKEUP, 0f, 1f)
    )

    /** Highest 0–100 position of any slider; 0 means "nothing to do". */
    fun activeCount(config: BeautyConfig): Int =
        FACE_SLIDERS.count { it.uiValue(config) > 0f }

    fun isActive(config: BeautyConfig): Boolean = activeCount(config) > 0

    /** Clamps an untrusted config (draft file, restored draft) into 0–100. */
    fun normalize(config: BeautyConfig): BeautyConfig =
        FACE_SLIDERS.fold(config) { acc, slider ->
            slider.withValue(acc, slider.uiValue(acc))
        }

    /**
     * Blend factor for the localised smoothing pass. `preserveTexture` keeps
     * more of the original, which is the whole point of the switch.
     */
    fun smoothMix(strength: Float, preserveTexture: Boolean): Float =
        strength.coerceIn(0f, 1f) * (if (preserveTexture) 0.45f else 0.85f)

    /** Blend factor for the whitening pass; deliberately gentle (see doc). */
    fun whitenMix(strength: Float): Float =
        (strength.coerceIn(0f, 1f) * 0.5f).coerceIn(0f, 0.5f)

    /** Lip tint factor for the makeup pass. */
    fun lipMix(strength: Float): Float = strength.coerceIn(0f, 1f) * 0.55f

    /** Detected-face chip: `FACE DETECTED · n`, or an honest empty state. */
    fun faceLabel(count: Int): String =
        if (count <= 0) "TIDAK ADA WAJAH" else "FACE DETECTED · $count"

    fun summary(config: BeautyConfig): String {
        val active = activeCount(config)
        if (active == 0) return "Belum ada slider yang digeser"
        val texture = if (config.preserveTexture) "tekstur dijaga" else "kulit halus penuh"
        return "$active slider aktif · $texture"
    }

    /** Tabs of the approved mockup. */
    val TABS: List<String> = listOf("Face", "Body", "Makeup")

    /**
     * Honest note for a tab whose sliders the approved mockup does not specify.
     * The shipped evidence for those groups is named instead of inventing UI.
     */
    fun tabNote(tab: String): String? = when (tab) {
        "Face" -> null

        "Body" -> "Tab Body belum punya rancangan slider di mockup yang di-ACC. Bukti asli " +
            "untuk grup ini: `BeautyBodyPreset.body_reshapes` (libmiddle-bridge.so) dan " +
            "`assets/ManualReshape/event.lua` (slider MANUAL_RESHAPE_INTENSITY_SLIM / _STRETCH). " +
            "Dipakai di milestone berikutnya — tidak dikarang sekarang."

        else -> "Tab Makeup belum punya rancangan slider di mockup yang di-ACC. Key makeup yang " +
            "sudah terbaca dari `assets/local_beauty_all.json`: " +
            SHIPPED_KEYS.filter { it.group == BeautyGroup.MAKEUP }.joinToString(", ") { it.key } +
            ". Dipakai di milestone berikutnya — tidak dikarang sekarang."
    }
}
