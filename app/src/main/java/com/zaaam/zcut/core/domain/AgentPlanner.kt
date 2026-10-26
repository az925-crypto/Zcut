package com.zaaam.zcut.core.domain

/**
 * Local rule-based planner behind the Zcut Agent (F23 parity).
 *
 * The artifact (`com.vega.ailabagent`, `com.vega.edit.videoagent`:
 * `AgentToolMessageHelper`, `sendTaskMessage`, `publishDeferredEditMore`,
 * `onMessagePendingTask`) is a tool-calling assistant that turns a message into
 * draft edits and shows a preview before applying them. The language model is
 * not bundled — the mockup itself labels it `LLM backend OPTIONAL` — so what runs
 * on device is keyword routing to a fixed tool set. Every step states whether it
 * can actually run here, and the ones that cannot are pointed at the screen that
 * does the local part.
 *
 * Pure JVM: the same prompt always produces the same plan.
 */
object AgentPlanner {

    /** Route ids are the approved mockup screen ids, resolved by the UI. */
    const val ROUTE_CLIPPER = "ai-clipper"

    enum class Tool(val label: String, val action: String) {
        TIGHTEN_INTRO("Tighten the intro", "tighten the intro by 0.8s"),
        ADD_MUSIC("Add a local music bed", "add a local lo-fi music bed"),
        AUTO_CUT("Find highlights", "find the best highlight windows"),
        ADD_CAPTION("Add captions", "add captions")
    }

    /**
     * One line of the plan. [runnable] is false when the step needs something
     * Zcut does not have locally; [route] then points at the screen that covers
     * the local half of it.
     */
    data class Step(
        val tool: Tool,
        val detail: String,
        val runnable: Boolean,
        val route: String? = null
    )

    data class Plan(val reply: String, val steps: List<Step>) {
        val runnableSteps: List<Step> = steps.filter { it.runnable }

        /** Something the user can actually watch happen. */
        val applies: Boolean get() = runnableSteps.isNotEmpty()
    }

    /** The mockup's opening bot line, kept verbatim. */
    const val GREETING =
        "What are we making? I can build a rough cut, add captions, or find the " +
            "best 15 seconds."

    /** The mockup's suggestion chips. */
    val SUGGESTIONS = listOf("Tighten the intro", "Add captions", "Find a hook")

    /** Message tag for the plan bubble, matching the mockup. */
    const val PLAN_TAG = "Plan · local tools"

    /**
     * Routes a prompt to a plan. An unrecognised prompt falls back to the two
     * local tools rather than inventing an answer, and says so.
     */
    fun plan(prompt: String): Plan {
        val text = prompt.lowercase()
        val steps = buildList {
            if (TIGHTEN_INTRO.matches(text)) {
                add(
                    Step(
                        tool = Tool.TIGHTEN_INTRO,
                        detail = "Trim ${AgentEditor.INTRO_HEAD_MS / 1000f}s off the head of the " +
                            "first clip and ripple the rest left.",
                        runnable = true
                    )
                )
            }
            if (ADD_MUSIC.matches(text)) {
                add(
                    Step(
                        tool = Tool.ADD_MUSIC,
                        detail = "Render a lo-fi loop on device and place it at 0.0s.",
                        runnable = true
                    )
                )
            }
            if (AUTO_CUT.matches(text)) {
                add(
                    Step(
                        tool = Tool.AUTO_CUT,
                        detail = "Score highlight windows from the video's own energy " +
                            "envelope (open AI Clipper).",
                        runnable = false,
                        route = ROUTE_CLIPPER
                    )
                )
            }
            if (ADD_CAPTION.matches(text)) {
                add(
                    Step(
                        tool = Tool.ADD_CAPTION,
                        detail = "Captioning the video's own audio needs an ASR backend; " +
                            "the Caption screen transcribes the microphone locally.",
                        runnable = false
                    )
                )
            }
        }

        val resolved = steps.ifEmpty { defaults() }
        return Plan(reply = replyFor(resolved, recognised = steps.isNotEmpty()), steps = resolved)
    }

    private fun defaults(): List<Step> = listOf(
        Step(
            tool = Tool.TIGHTEN_INTRO,
            detail = "Trim ${AgentEditor.INTRO_HEAD_MS / 1000f}s off the head of the first " +
                "clip and ripple the rest left.",
            runnable = true
        ),
        Step(
            tool = Tool.ADD_MUSIC,
            detail = "Render a lo-fi loop on device and place it at 0.0s.",
            runnable = true
        )
    )

    private fun replyFor(steps: List<Step>, recognised: Boolean): String {
        val actions = steps.joinToString(", ") { it.tool.action }
        val opener = if (recognised) {
            "I will $actions."
        } else {
            "I did not catch a specific request, so I will $actions."
        }
        val local = steps.count { it.runnable }
        val backend = steps.size - local
        val ready = when {
            local == 0 -> "Nothing here can run on device yet."
            backend == 0 -> "$local local tool call(s) ready. Review them before I " +
                "touch the timeline."
            else -> "$local local tool call(s) ready; $backend step(s) need a backend."
        }
        return "$opener $ready"
    }

    private fun Tool.matches(text: String): Boolean = keywords.any { it in text }

    /** Lowercase keywords, kept in one place so routing stays readable. */
    private val Tool.keywords: List<String>
        get() = when (this) {
            Tool.TIGHTEN_INTRO ->
                listOf("tighten", "intro", "trim", "shorten", "rough cut", "cold open")
            Tool.ADD_MUSIC ->
                listOf("music", "beat", "song", "soundtrack", "audio bed", "lo-fi", "lofi")
            Tool.AUTO_CUT ->
                listOf("highlight", "hook", "shorts", "find", "best moment", "key moment")
            Tool.ADD_CAPTION ->
                listOf("caption", "subtitle", "sub-title", "text on screen")
        }
}