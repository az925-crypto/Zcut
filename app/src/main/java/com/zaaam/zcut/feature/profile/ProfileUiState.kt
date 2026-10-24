package com.zaaam.zcut.feature.profile

data class ProfileUiState(
    val isLoading: Boolean = true,
    val draftCount: Int = 0,
    /** Filled from export history in M13; honest zero until then. */
    val exportCount: Int = 0,
    val storageBytes: Long = 0L,
    val error: String? = null
)
