package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.BeautyConfig
import com.zaaam.zcut.core.model.ColorAdjustments
import com.zaaam.zcut.core.model.HslBand
import com.zaaam.zcut.core.model.MattingBackground
import com.zaaam.zcut.core.model.MattingConfig
import com.zaaam.zcut.core.model.TextConfig

/**
 * Non-destructive photo edit state (F26 parity).
 *
 * The artifact's photo editor (`com.vega.image`, `com.xt.retouch`, `InitImageEditor`,
 * `ImageEditorCommonClient`) is a layered, non-destructive editor. Zcut stores the
 * *recipe* — crop, transform, colours, filter, retouch, cutout, overlays — and
 * renders pixels only for preview and export, so every step stays reversible.
 *
 * Colour and filter reuse [ColorAdjustments]/[com.zaaam.zcut.core.model.FilterConfig]
 * (the same contracts as video clips) and [ColorPipeline] builds the matrix, so
 * there is one colour implementation, not two.
 */
object PhotoEditLimits {

    /** Longest side kept in memory while editing; export keeps the same bound. */
    const val MAX_DIMENSION = 2_048

    /** Smallest crop window, as a fraction of the image. */
    const val MIN_CROP = 0.05f

    /** Cap on freehand strokes, so a runaway gesture cannot grow the state. */
    const val MAX_STROKES = 200

    /** Cap on points per stroke. */
    const val MAX_STROKE_POINTS = 400
}

/** One normalized point (0..1) of a brush stroke. */
data class PhotoPoint(val x: Float, val y: Float) {
    fun sanitized(): PhotoPoint = PhotoPoint(
        x = if (x.isNaN()) 0f else x.coerceIn(0f, 1f),
        y = if (y.isNaN()) 0f else y.coerceIn(0f, 1f)
    )
}

/** One freehand stroke drawn over the photo. */
data class BrushStroke(
    val points: List<PhotoPoint>,
    val colorArgb: Long = 0xFFFF3B30,
    val widthFraction: Float = 0.02f,
    val opacity: Float = 0.9f
) {
    fun sanitized(): BrushStroke = copy(
        points = points
            .take(PhotoEditLimits.MAX_STROKE_POINTS)
            .map { it.sanitized() },
        colorArgb = colorArgb,
        widthFraction = widthFraction.coerceIn(0.002f, 0.25f),
        opacity = opacity.coerceIn(0.05f, 1f)
    )

    val isDrawable: Boolean get() = points.size >= 2
}

/** Normalized crop window inside the source image. */
data class CropWindow(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
) {
    fun sanitized(): CropWindow {
        val l = left.coerceIn(0f, 1f - PhotoEditLimits.MIN_CROP)
        val t = top.coerceIn(0f, 1f - PhotoEditLimits.MIN_CROP)
        val r = right.coerceIn(l + PhotoEditLimits.MIN_CROP, 1f)
        val b = bottom.coerceIn(t + PhotoEditLimits.MIN_CROP, 1f)
        return CropWindow(left = l, top = t, right = r, bottom = b)
    }

    val widthRatio: Float get() = (right - left).coerceAtLeast(0f)
    val heightRatio: Float get() = (bottom - top).coerceAtLeast(0f)

    companion object {

        /** Centred window with a target width/height ratio for a source ratio. */
        fun centered(sourceRatio: Float, targetRatio: Float): CropWindow {
            if (sourceRatio <= 0f || targetRatio <= 0f) return CropWindow()
            return if (sourceRatio > targetRatio) {
                val width = (targetRatio / sourceRatio).coerceIn(
                    PhotoEditLimits.MIN_CROP,
                    1f
                )
                val left = (1f - width) / 2f
                CropWindow(left = left, top = 0f, right = left + width, bottom = 1f)
            } else {
                val height = (sourceRatio / targetRatio).coerceIn(
                    PhotoEditLimits.MIN_CROP,
                    1f
                )
                val top = (1f - height) / 2f
                CropWindow(left = 0f, top = top, right = 1f, bottom = top + height)
            }
        }
    }
}

/** Crop presets offered by the photo tool. */
enum class PhotoAspect(val label: String, val ratio: Float?) {
    ORIGINAL("Original", null),
    SQUARE("1:1", 1f),
    PORTRAIT_4_5("4:5", 4f / 5f),
    PORTRAIT_9_16("9:16", 9f / 16f),
    LANDSCAPE_16_9("16:9", 16f / 9f)
}

/** The mockup's Light / Color / Detail segmented control. */
enum class PhotoAdjustTab(val label: String) {
    LIGHT("Light"),
    COLOR("Color"),
    DETAIL("Detail")
}

/** Export formats offered by the photo editor's Export action. */
enum class PhotoExportFormat(
    val label: String,
    val extension: String,
    val mimeType: String,
    val quality: Int,
    val lossless: Boolean = false,
    val supportsAlpha: Boolean = true
) {
    JPG("JPG", "jpg", "image/jpeg", 95, supportsAlpha = false),
    PNG("PNG", "png", "image/png", 100, lossless = true),
    WEBP("WebP", "webp", "image/webp", 95)
}

/**
 * The whole recipe. Defaults are neutral: nothing to render, so the editor can
 * say "no edits yet" instead of writing an identical file.
 */
data class PhotoEditState(
    val crop: CropWindow = CropWindow(),
    val aspect: PhotoAspect = PhotoAspect.ORIGINAL,
    /** 0..3 clockwise quarter turns. */
    val quarterTurns: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val filterId: String? = null,
    val filterIntensity: Float = 1f,
    val color: ColorAdjustments = ColorAdjustments(),
    val beauty: BeautyConfig = BeautyConfig(),
    val matting: MattingConfig = MattingConfig(),
    /** At most one text layer on a photo, which is what the mockup shows. */
    val text: TextConfig? = null,
    val stickerId: String? = null,
    val brush: List<BrushStroke> = emptyList()
) {

    fun sanitized(): PhotoEditState = copy(
        crop = crop.sanitized(),
        quarterTurns = ((quarterTurns % 4) + 4) % 4,
        filterIntensity = filterIntensity.coerceIn(0f, 1f),
        color = sanitizeColor(color),
        beauty = BeautyEditor.normalize(beauty),
        matting = MattingEditor.normalize(matting),
        text = text?.let(TextEditor::normalize),
        stickerId = stickerId?.let { StickerCatalog.canonical(it).id },
        brush = brush.take(PhotoEditLimits.MAX_STROKES).map { it.sanitized() }
    )

    /** True when the recipe would change pixels; the export sheet warns otherwise. */
    val isEdited: Boolean
        get() = crop != CropWindow() ||
            quarterTurns != 0 ||
            flipHorizontal ||
            flipVertical ||
            filterId != null ||
            !color.isNeutral ||
            BeautyEditor.isActive(beauty) ||
            matting.enabled && matting.background != MattingBackground.TRANSPARENT ||
            text != null ||
            stickerId != null ||
            brush.any { it.isDrawable }

    companion object {

        /** Adjustments are -1..1; bands keep a valid hue and -1..1 strengths. */
        fun sanitizeColor(color: ColorAdjustments): ColorAdjustments = color.copy(
            brightness = color.brightness.coerceIn(-1f, 1f),
            contrast = color.contrast.coerceIn(-1f, 1f),
            saturation = color.saturation.coerceIn(-1f, 1f),
            temperature = color.temperature.coerceIn(-1f, 1f),
            tint = color.tint.coerceIn(-1f, 1f),
            highlights = color.highlights.coerceIn(-1f, 1f),
            shadows = color.shadows.coerceIn(-1f, 1f),
            vignette = color.vignette.coerceIn(0f, 1f),
            hslBands = color.hslBands
                .map { band ->
                    HslBand(
                        hue = if (band.hue.isNaN()) 0f else ((band.hue % 360f) + 360f) % 360f,
                        saturation = band.saturation.coerceIn(-1f, 1f),
                        luminance = band.luminance.coerceIn(-1f, 1f)
                    )
                }
                .take(12)
        )
    }
}

/** Choosing a crop preset also rewrites the window; kept pure so tests can pin it. */
fun PhotoEditState.withAspect(aspect: PhotoAspect, sourceRatio: Float): PhotoEditState {
    val next = aspect.ratio?.let { CropWindow.centered(sourceRatio, it) } ?: CropWindow()
    return copy(aspect = aspect, crop = next).sanitized()
}