package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.Resolution

/** Human labels for the export defaults shown in Settings. */
object ExportDefaults {

    fun resolutionLabel(resolution: Resolution): String = when (resolution) {
        Resolution.HD_720 -> "720p"
        Resolution.FHD_1080 -> "1080p"
        Resolution.QHD_1440 -> "1440p"
        Resolution.UHD_2160 -> "4K"
    }

    fun summary(resolution: Resolution, frameRate: Int): String =
        "${resolutionLabel(resolution)} · $frameRate fps"

    fun audioLabel(sampleRate: Int): String = when (sampleRate) {
        44_100 -> "AAC 44,1 kHz"
        else -> "AAC 48 kHz"
    }
}
