package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.AspectRatio
import com.zaaam.zcut.core.model.ExportPreset
import com.zaaam.zcut.core.model.Project
import kotlin.math.roundToInt

object ExportDimensions {
    fun forProject(project: Project): Pair<Int, Int> =
        forAspectRatio(project.aspectRatio, project.settings.exportPreset)

    fun forAspectRatio(aspectRatio: AspectRatio, preset: ExportPreset): Pair<Int, Int> {
        val shortEdge = preset.resolution.shortEdge
        if (aspectRatio.width <= 0 || aspectRatio.height <= 0) {
            return even(shortEdge) to even(shortEdge)
        }
        return if (aspectRatio.width >= aspectRatio.height) {
            even((shortEdge.toFloat() * aspectRatio.width / aspectRatio.height).roundToInt()) to
                even(shortEdge)
        } else {
            even(shortEdge) to
                even((shortEdge.toFloat() * aspectRatio.height / aspectRatio.width).roundToInt())
        }
    }

    private fun even(value: Int): Int {
        val safe = value.coerceAtLeast(2)
        return if (safe % 2 == 0) safe else safe + 1
    }
}
