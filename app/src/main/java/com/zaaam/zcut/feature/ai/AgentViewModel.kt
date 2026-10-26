package com.zaaam.zcut.feature.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zcut.core.data.ProjectRepository
import com.zaaam.zcut.core.domain.AgentEditor
import com.zaaam.zcut.core.domain.AgentPlanner
import com.zaaam.zcut.core.domain.AudioEditor
import com.zaaam.zcut.core.domain.MusicCatalog
import com.zaaam.zcut.core.domain.MusicGenerator
import com.zaaam.zcut.core.domain.MusicMood
import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.core.model.MediaKind
import com.zaaam.zcut.core.model.Project
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** One chat bubble. [tag] is the mockup's small label above the text. */
data class AgentMessage(
    val fromUser: Boolean,
    val tag: String? = null,
    val text: String
)

data class AgentUiState(
    val loading: Boolean = true,
    /** Draft the plan will be applied to, or null when there is none. */
    val targetId: String? = null,
    val targetName: String? = null,
    val messages: List<AgentMessage> = listOf(
        AgentMessage(fromUser = false, tag = "Zcut Agent", text = AgentPlanner.GREETING)
    ),
    val input: String = "",
    val plan: AgentPlanner.Plan? = null,
    val busy: Boolean = false,
    val notice: String? = null
) {
    val canApply: Boolean get() = plan?.applies == true && targetId != null && !busy

    companion object {
        const val MAX_INPUT = 400
    }
}

/**
 * Zcut Agent (F23). Keyword routing plus two real local tools; the language model
 * itself is the mockup's optional backend and is not bundled.
 *
 * When the AI Studio hub opens it without a draft, the agent targets the most
 * recently updated draft and says so in the panel — it never touches a draft the
 * user cannot see.
 */
class AgentViewModel(
    private val projectId: String?,
    private val repository: ProjectRepository,
    private val musicDir: File
) : ViewModel() {

    private val editMutex = Mutex()
    private val _uiState = MutableStateFlow(AgentUiState())
    val uiState: StateFlow<AgentUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.projects
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            notice = error.message ?: "Gagal memuat draft"
                        )
                    }
                }
                .collect { projects ->
                    val explicit = projectId?.let { id -> projects.firstOrNull { it.id == id } }
                    val target = explicit ?: projects.maxByOrNull { it.updatedAtMs }
                    _uiState.update {
                        it.copy(
                            loading = false,
                            targetId = target?.id,
                            targetName = target?.name
                        )
                    }
                }
        }
    }

    fun setInput(text: String) {
        _uiState.update { it.copy(input = text.take(AgentUiState.MAX_INPUT)) }
    }

    fun clearNotice() {
        _uiState.update { it.copy(notice = null) }
    }

    /** Sends one message and appends the plan + ready bubbles it produces. */
    fun send(text: String = _uiState.value.input) {
        val prompt = text.trim()
        if (prompt.isEmpty() || _uiState.value.busy) return
        val plan = AgentPlanner.plan(prompt)
        val ready = if (plan.applies) {
            "${plan.runnableSteps.size} tool call(s) ready. Review the plan before I " +
                "touch the timeline."
        } else {
            // No step here can run and none has a screen to open (a caption-only
            // plan needs a draft and a backend), so do not promise a button.
            "Nothing here can run on device yet. The steps above say what each one " +
                "still needs."
        }
        _uiState.update { current ->
            current.copy(
                input = "",
                plan = plan,
                notice = null,
                messages = current.messages +
                    AgentMessage(fromUser = true, text = prompt) +
                    AgentMessage(fromUser = false, tag = AgentPlanner.PLAN_TAG, text = plan.reply) +
                    AgentMessage(fromUser = false, tag = "Ready", text = ready)
            )
        }
    }

    /**
     * Runs the plan's local steps against the target draft and stores the result.
     * [onDone] receives the draft id that changed, or null when nothing did.
     */
    fun applyPlan(onDone: (String?) -> Unit) {
        val snapshot = _uiState.value
        val plan = snapshot.plan ?: return onDone(null)
        val draftId = snapshot.targetId ?: return onDone(null)
        if (snapshot.busy) return onDone(null)
        val runnable = plan.runnableSteps
        if (runnable.isEmpty()) return onDone(null)

        _uiState.update { it.copy(busy = true, notice = null) }
        viewModelScope.launch {
            try {
                val stored = editMutex.withLock {
                    val project = repository.get(draftId) ?: return@withLock null
                    var next = project

                    if (runnable.any { it.tool == AgentPlanner.Tool.TIGHTEN_INTRO }) {
                        next = AgentEditor.tightenIntro(next)
                    }
                    if (runnable.any { it.tool == AgentPlanner.Tool.ADD_MUSIC }) {
                        val asset = renderMusic(next)
                        if (asset != null) {
                            next = AudioEditor.insertAudio(
                                project = next,
                                asset = asset,
                                atMs = 0L,
                                clipId = UUID.randomUUID().toString()
                            )
                        }
                    }
                    if (next == project) null else next.also { repository.upsert(it) }
                }
                _uiState.update {
                    it.copy(
                        busy = false,
                        notice = if (stored != null) {
                            "Rencana diterapkan ke \"${stored.name}\"."
                        } else {
                            "Tidak ada yang berubah di draft ini."
                        }
                    )
                }
                onDone(stored?.id)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        busy = false,
                        notice = "Gagal menerapkan rencana: " +
                            (error.message ?: "kesalahan tak dikenal")
                    )
                }
                onDone(null)
            }
        }
    }

    /**
     * Renders a local loop as long as the draft, then wraps it as an asset.
     * Ponytail: this repeats the render/write half of [com.zaaam.zcut.feature.tools.AiMusicViewModel];
     * pulling it into a shared helper would need a new core/media type for two
     * call sites, so the duplication is cheaper than the abstraction.
     */
    private suspend fun renderMusic(project: Project): MediaAsset? {
        val duration = project.timeline.durationMs
        if (duration <= 0L) return null
        val style = MusicCatalog.style(MusicMood.LOFI)
        val target = duration.coerceIn(
            MusicGenerator.MIN_DURATION_MS,
            MusicGenerator.MAX_DURATION_MS
        )
        val seed = project.id.hashCode()
        return withContext(Dispatchers.Default) {
            val bytes = MusicGenerator.wav(style, target, seed)
            val base = MusicGenerator.fileName(style, target, seed)
            val file = withContext(Dispatchers.IO) {
                runCatching { MusicGenerator.writeUnique(musicDir, base, bytes) }.getOrNull()
            } ?: return@withContext null
            MediaAsset(
                id = UUID.randomUUID().toString(),
                uri = "",
                displayName = file.name,
                mimeType = "audio/wav",
                kind = MediaKind.AUDIO,
                durationMs = target,
                sizeBytes = file.length(),
                localPath = file.absolutePath
            )
        }
    }
}