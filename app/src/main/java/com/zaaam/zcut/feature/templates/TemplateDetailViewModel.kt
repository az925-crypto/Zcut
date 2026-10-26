package com.zaaam.zcut.feature.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zcut.core.data.PreferencesRepository
import com.zaaam.zcut.core.domain.TemplateCatalog
import com.zaaam.zcut.core.domain.VideoTemplate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TemplateDetailUiState(
    val template: VideoTemplate? = null,
    val notFound: Boolean = false,
    val favorite: Boolean = false
)

/**
 * Template detail only reads the built-in catalogue and remembers the heart
 * state. Favourites live in local preferences: no account, no sync, no upload.
 */
class TemplateDetailViewModel(
    private val templateId: String,
    private val preferences: PreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<TemplateDetailUiState> = preferences.preferences
        .map { stored ->
            TemplateDetailUiState(
                template = template,
                notFound = template == null,
                favorite = templateId in stored.favoriteTemplateIds
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            // The preferences flow already swallows a corrupt store, so this is
            // only the first frame before the read lands.
            initialValue = TemplateDetailUiState(template = template, notFound = template == null)
        )

    fun toggleFavorite() {
        if (template == null) return
        viewModelScope.launch {
            val current = runCatching { preferences.preferences.first() }.getOrNull()
                ?: return@launch
            runCatching {
                preferences.setTemplateFavorite(templateId, templateId !in current.favoriteTemplateIds)
            }
        }
    }

    private val template: VideoTemplate? get() = TemplateCatalog.byId(templateId)
}