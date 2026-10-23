package com.zaaam.zcut.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class AspectRatio(val width: Int, val height: Int, val label: String) {
    PORTRAIT_9_16(9, 16, "9:16"),
    SQUARE_1_1(1, 1, "1:1"),
    LANDSCAPE_16_9(16, 9, "16:9"),
    PORTRAIT_4_5(4, 5, "4:5");

    val ratio: Float
        get() = if (height == 0) 1f else width.toFloat() / height.toFloat()

    companion object {
        fun fromLabel(label: String): AspectRatio =
            entries.firstOrNull { it.label == label } ?: PORTRAIT_9_16
    }
}
