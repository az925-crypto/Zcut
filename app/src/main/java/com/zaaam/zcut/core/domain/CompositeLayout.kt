package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.AspectRatio

/**
 * Split-screen layouts (F30).
 *
 * The shipped artifact keeps its grids inside `assets/splitscreen/split_screen_{2..9}.zip`
 * (`template.json` + `config.json`); the exact JSON schema could not be read, so
 * these are the layouts the approved mockup draws: 1, 2, 4, 6 and 9 cells. The
 * cell count is the contract the rest of the feature relies on.
 */
enum class SplitLayout(val columns: Int, val rows: Int, val label: String) {
    SINGLE(1, 1, "1 panel"),
    SPLIT_2(2, 1, "2 panel"),
    GRID_4(2, 2, "4 panel"),
    GRID_6(3, 2, "6 panel"),
    GRID_9(3, 3, "9 panel");

    val cells: Int get() = columns * rows

    companion object {
        val default: SplitLayout = GRID_4

        /** The mockup offers up to nine panels. */
        const val MAX_CELLS = 9
    }
}

/** A rectangle in pixels; kept out of Android graphics so geometry is JVM-testable. */
data class CellRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int get() = (right - left).coerceAtLeast(0)
    val height: Int get() = (bottom - top).coerceAtLeast(0)
}

/**
 * One split-screen recipe. Every value is a proportion so the same recipe can be
 * previewed small and exported large.
 *
 * `borderPercent` is the gap/border as a percentage of the canvas short side;
 * `cornerPercent` is the corner radius as a percentage of the smallest cell.
 * Both ranges are **inferred** from the mockup sliders (Border 0–20, Corner 0–30).
 */
data class SplitScreenConfig(
    val layout: SplitLayout = SplitLayout.default,
    val aspectRatio: AspectRatio = AspectRatio.SQUARE_1_1,
    val borderPercent: Int = DEFAULT_BORDER_PERCENT,
    val cornerPercent: Int = DEFAULT_CORNER_PERCENT,
    val backgroundArgb: Long = DEFAULT_BACKGROUND_ARGB,
    val centerCrop: Boolean = true
) {
    fun sanitize(): SplitScreenConfig = copy(
        borderPercent = borderPercent.coerceIn(0, MAX_BORDER_PERCENT),
        cornerPercent = cornerPercent.coerceIn(0, MAX_CORNER_PERCENT),
        backgroundArgb = backgroundArgb or 0xFF000000L
    )

    companion object {
        const val MAX_BORDER_PERCENT = 20
        const val MAX_CORNER_PERCENT = 30
        const val DEFAULT_BORDER_PERCENT = 4
        const val DEFAULT_CORNER_PERCENT = 12
        const val DEFAULT_BACKGROUND_ARGB = 0xFF08090DL
    }
}

object SplitGeometry {

    /**
     * Divides the canvas into [layout] cells with [gapPx] between neighbours.
     * Cells never overlap; the last row/column absorbs integer rounding.
     */
    fun cellRects(
        layout: SplitLayout,
        width: Int,
        height: Int,
        gapPx: Int
    ): List<CellRect> {
        if (width <= 0 || height <= 0) return emptyList()
        val safeGap = gapPx.coerceIn(0, minOf(width, height) / 8)
        val cellWidth = (width - safeGap * (layout.columns - 1)) / layout.columns
        val cellHeight = (height - safeGap * (layout.rows - 1)) / layout.rows
        if (cellWidth <= 0 || cellHeight <= 0) return emptyList()

        return buildList(layout.cells) {
            for (row in 0 until layout.rows) {
                for (column in 0 until layout.columns) {
                    val left = column * (cellWidth + safeGap)
                    val top = row * (cellHeight + safeGap)
                    add(
                        CellRect(
                            left = left,
                            top = top,
                            right = left + cellWidth,
                            bottom = top + cellHeight
                        )
                    )
                }
            }
        }
    }

    /** Source rectangle that fills [dstWidth]×[dstHeight] without stretching. */
    fun centerCropRect(
        srcWidth: Int,
        srcHeight: Int,
        dstWidth: Int,
        dstHeight: Int
    ): CellRect {
        if (srcWidth <= 0 || srcHeight <= 0 || dstWidth <= 0 || dstHeight <= 0) {
            return CellRect(0, 0, 0, 0)
        }
        val srcRatio = srcWidth.toFloat() / srcHeight
        val dstRatio = dstWidth.toFloat() / dstHeight
        return if (srcRatio > dstRatio) {
            val width = (srcHeight * dstRatio).toInt().coerceIn(1, srcWidth)
            val left = (srcWidth - width) / 2
            CellRect(left, 0, left + width, srcHeight)
        } else {
            val height = (srcWidth / dstRatio).toInt().coerceIn(1, srcHeight)
            val top = (srcHeight - height) / 2
            CellRect(0, top, srcWidth, top + height)
        }
    }
}

enum class FacecamPosition(val label: String) {
    TOP_LEFT("Kiri atas"),
    TOP_RIGHT("Kanan atas"),
    BOTTOM_LEFT("Kiri bawah"),
    BOTTOM_RIGHT("Kanan bawah")
}

enum class FacecamShape(val label: String) {
    ROUNDED("Rounded"),
    CIRCLE("Circle")
}

/**
 * Gameplay overlay recipe: one full-bleed gameplay frame plus a facecam bubble.
 *
 * The artifact (`GameplayAdjustableInfo`, `SetGameplayConfig`) exposes many more
 * knobs, but the approved mockup only draws position, size, border and shape.
 * Size/border ranges are **inferred** from that mockup; chroma key is kept in
 * the recipe for parity but is not applied yet (stated in the UI).
 */
data class GameplayConfig(
    val position: FacecamPosition = FacecamPosition.BOTTOM_RIGHT,
    val sizePercent: Int = DEFAULT_SIZE_PERCENT,
    val borderPercent: Int = DEFAULT_BORDER_PERCENT,
    val shape: FacecamShape = FacecamShape.ROUNDED,
    val chromaKey: Boolean = false,
    val backgroundArgb: Long = DEFAULT_BACKGROUND_ARGB
) {
    fun sanitize(): GameplayConfig = copy(
        sizePercent = sizePercent.coerceIn(MIN_SIZE_PERCENT, MAX_SIZE_PERCENT),
        borderPercent = borderPercent.coerceIn(0, MAX_BORDER_PERCENT),
        backgroundArgb = backgroundArgb or 0xFF000000L
    )

    companion object {
        const val MIN_SIZE_PERCENT = 20
        const val MAX_SIZE_PERCENT = 60
        const val DEFAULT_SIZE_PERCENT = 30
        const val MAX_BORDER_PERCENT = 20
        const val DEFAULT_BORDER_PERCENT = 2
        const val DEFAULT_BACKGROUND_ARGB = 0xFF08090DL
    }
}

object GameplayGeometry {

    /** Facecam square inside the frame, inset from the chosen corner. */
    fun facecamRect(config: GameplayConfig, width: Int, height: Int): CellRect {
        if (width <= 0 || height <= 0) return CellRect(0, 0, 0, 0)
        val safe = config.sanitize()
        val shortSide = minOf(width, height)
        val margin = (shortSide * MARGIN_PERCENT / 100f).toInt().coerceAtLeast(1)
        val size = (shortSide * safe.sizePercent / 100)
            .coerceAtMost(shortSide - margin * 2)
            .coerceAtLeast(1)
        val left = if (safe.position == FacecamPosition.TOP_LEFT ||
            safe.position == FacecamPosition.BOTTOM_LEFT
        ) {
            margin
        } else {
            width - margin - size
        }
        val top = if (safe.position == FacecamPosition.TOP_LEFT ||
            safe.position == FacecamPosition.TOP_RIGHT
        ) {
            margin
        } else {
            height - margin - size
        }
        return CellRect(
            left = left.coerceIn(0, width - size),
            top = top.coerceIn(0, height - size),
            right = (left.coerceIn(0, width - size) + size),
            bottom = (top.coerceIn(0, height - size) + size)
        )
    }

    private const val MARGIN_PERCENT = 4
}

/** How a live photo should leave Zcut; only a plain video is rendered locally. */
enum class LivePhotoExport(val label: String, val supported: Boolean) {
    VIDEO("Video", true),
    GIF("GIF", false),
    STILL_MOTION("Still + motion", false)
}

/**
 * Live-photo recipe (F30). `durationMs` is the still's clip length when the
 * still and the extracted motion clip are combined; `motionStrengthPercent`
 * only matters for the unsupported motion/GIF exports.
 */
data class LivePhotoConfig(
    val durationMs: Long = DEFAULT_DURATION_MS,
    val motionStrengthPercent: Int = DEFAULT_MOTION_PERCENT,
    val export: LivePhotoExport = LivePhotoExport.VIDEO
) {
    fun sanitize(): LivePhotoConfig = copy(
        durationMs = durationMs.coerceIn(MIN_DURATION_MS, MAX_DURATION_MS),
        motionStrengthPercent = motionStrengthPercent.coerceIn(0, 100),
        export = if (export.supported) export else LivePhotoExport.VIDEO
    )

    companion object {
        const val MIN_DURATION_MS = 1_000L
        const val MAX_DURATION_MS = 10_000L
        const val DEFAULT_DURATION_MS = 3_200L
        const val DEFAULT_MOTION_PERCENT = 60
    }
}
