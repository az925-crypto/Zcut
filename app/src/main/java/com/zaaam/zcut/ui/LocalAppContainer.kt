package com.zaaam.zcut.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zaaam.zcut.core.AppContainer
import com.zaaam.zcut.feature.editor.EditorViewModel
import com.zaaam.zcut.feature.export.ExportViewModel
import com.zaaam.zcut.feature.home.HomeViewModel
import com.zaaam.zcut.feature.media.MediaPickerViewModel
import com.zaaam.zcut.feature.profile.ProfileViewModel
import com.zaaam.zcut.feature.projects.ProjectsViewModel
import com.zaaam.zcut.feature.settings.SettingsViewModel
import com.zaaam.zcut.feature.templates.TemplateDetailViewModel
import com.zaaam.zcut.feature.tools.AiMusicViewModel
import com.zaaam.zcut.feature.tools.AudioViewModel
import com.zaaam.zcut.feature.tools.CaptionViewModel
import com.zaaam.zcut.feature.tools.ClipToolViewModel
import com.zaaam.zcut.feature.tools.StickerViewModel
import com.zaaam.zcut.feature.tools.TextViewModel
import com.zaaam.zcut.feature.tools.TtsViewModel

/**
 * Manual dependency injection, in the same spirit as [AppContainer]: one
 * composition local and one factory, no DI framework.
 */
val LocalAppContainer = androidx.compose.runtime.staticCompositionLocalOf<AppContainer> {
    error("AppContainer was not provided. Wrap the UI in ZcutApp().")
}

@Composable
fun zcutViewModelFactory(container: AppContainer): ViewModelProvider.Factory = remember(container) {
    viewModelFactory {
        initializer { HomeViewModel(container.projectRepository) }
        initializer { ProjectsViewModel(container.projectRepository) }
        initializer {
            ProfileViewModel(
                repository = container.projectRepository,
                storage = container.storage
            )
        }
        initializer {
            SettingsViewModel(
                preferencesRepository = container.preferencesRepository,
                repository = container.projectRepository,
                storage = container.storage
            )
        }
    }
}

/** Editor is scoped to one draft, so its factory carries the id. */
@Composable
fun editorViewModelFactory(
    container: AppContainer,
    projectId: String
): ViewModelProvider.Factory = remember(container, projectId) {
    viewModelFactory {
        initializer {
            EditorViewModel(
                projectId = projectId,
                repository = container.projectRepository,
                previewEngine = container.newPreviewEngine()
            )
        }
    }
}

/** Clip tool panels share one ViewModel; the key includes the route and clip. */
@Composable
fun clipToolViewModelFactory(
    container: AppContainer,
    projectId: String,
    clipId: String?
): ViewModelProvider.Factory = remember(container, projectId, clipId) {
    viewModelFactory {
        initializer {
            ClipToolViewModel(
                projectId = projectId,
                clipId = clipId,
                repository = container.projectRepository
            )
        }
    }
}

/** Text can create a clip, so it carries the optional selected clip too. */
@Composable
fun textViewModelFactory(
    container: AppContainer,
    projectId: String,
    clipId: String?
): ViewModelProvider.Factory = remember(container, projectId, clipId) {
    viewModelFactory {
        initializer {
            TextViewModel(
                projectId = projectId,
                initialClipId = clipId,
                repository = container.projectRepository
            )
        }
    }
}

/** Stickers can create a clip too, so they carry the optional selected clip. */
@Composable
fun stickerViewModelFactory(
    container: AppContainer,
    projectId: String,
    clipId: String?
): ViewModelProvider.Factory = remember(container, projectId, clipId) {
    viewModelFactory {
        initializer {
            StickerViewModel(
                projectId = projectId,
                initialClipId = clipId,
                repository = container.projectRepository
            )
        }
    }
}

/** Captions belong to the draft, so they only need its id. */
@Composable
fun captionViewModelFactory(
    container: AppContainer,
    projectId: String
): ViewModelProvider.Factory = remember(container, projectId) {
    viewModelFactory {
        initializer {
            CaptionViewModel(
                projectId = projectId,
                repository = container.projectRepository
            )
        }
    }
}

/** Audio can create clips, so it carries the optional clip and the playhead. */
@Composable
fun audioViewModelFactory(
    container: AppContainer,
    projectId: String,
    clipId: String?
): ViewModelProvider.Factory = remember(container, projectId, clipId) {
    viewModelFactory {
        initializer {
            AudioViewModel(
                projectId = projectId,
                initialClipId = clipId,
                repository = container.projectRepository
            )
        }
    }
}

/** TTS inserts at the playhead the audio screen was opened with. */
@Composable
fun ttsViewModelFactory(
    container: AppContainer,
    projectId: String,
    atMs: Long
): ViewModelProvider.Factory = remember(container, projectId, atMs) {
    viewModelFactory {
        initializer {
            TtsViewModel(
                projectId = projectId,
                atMs = atMs,
                repository = container.projectRepository,
                speechEngine = container.speechEngine,
                cacheDir = container.ttsCacheDir
            )
        }
    }
}

/** AI Music renders a loop locally and inserts it at the opening playhead. */
@Composable
fun aiMusicViewModelFactory(
    container: AppContainer,
    projectId: String,
    atMs: Long
): ViewModelProvider.Factory = remember(container, projectId, atMs) {
    viewModelFactory {
        initializer {
            AiMusicViewModel(
                projectId = projectId,
                atMs = atMs,
                repository = container.projectRepository,
                musicDir = container.aiMusicDir
            )
        }
    }
}

/** Template detail only reads the built-in catalogue; favourites are local. */
@Composable
fun templateDetailViewModelFactory(
    container: AppContainer,
    templateId: String
): ViewModelProvider.Factory = remember(container, templateId) {
    viewModelFactory {
        initializer {
            TemplateDetailViewModel(
                templateId = templateId,
                preferences = container.preferencesRepository
            )
        }
    }
}

/** Export is scoped to one draft and owns its render engine. */
@Composable
fun exportViewModelFactory(
    container: AppContainer,
    projectId: String
): ViewModelProvider.Factory = remember(container, projectId) {
    viewModelFactory {
        initializer {
            ExportViewModel(
                projectId = projectId,
                repository = container.projectRepository,
                renderEngine = container.newRenderEngine(),
                exportDir = container.exportDir
            )
        }
    }
}

/** `projectId` is null when the picker starts a new draft. */
@Composable
fun mediaPickerViewModelFactory(
    container: AppContainer,
    projectId: String?,
    templateId: String? = null
): ViewModelProvider.Factory = remember(container, projectId, templateId) {
    viewModelFactory {
        initializer {
            MediaPickerViewModel(
                projectId = projectId,
                templateId = templateId,
                repository = container.projectRepository,
                importer = container.mediaImporter
            )
        }
    }
}
