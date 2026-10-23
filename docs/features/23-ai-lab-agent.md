# 23 — AI Lab Agent, AI Writer & Video Agent

> **Zcut policy:** fitur ini gratis, tanpa paywall/watermark, dan tidak memanggil server aplikasi asli.  
> Assistant rule-based lokal; LLM agent ditandai **BUTUH BACKEND**.

## Bukti

- Package: `com.vega.ailabagent` (+ `unifyagent`): `msglist` (429),
  `preview` (372), `sug` (250), `settings` (217), `history` (211), `chat`
  (195), `component` (145), `input` (143), `conversation` (142),
  `panel` (70), `ability` (51), `material` (35).
- `com.vega.edit.videoagent` (392): `AgentToolMessageHelper` +
  `DraftInfo`, `sendTaskMessage`, `publishDeferredEditMore`,
  `getAIWriterFinishMessage`, `onMessagePendingTask`.
- `com.vega.edit.functionassistant` (311); `FunctionAssistantInfo`
  (`getAutoCaption`, `getFirstShowKeywordsInAutoCaption`).
- Activity: `AiLabAgentUniversalPreviewActivity`.
- Asset: `agent_input_loading.json`, `ailab_tab/data.json`,
  `ailab_tab_v2/data.json`, `ai_writer_template/ai_writer_template.json`.
- String: `ai writer credits failed`, `ai writer message, type:`,
  `ai writer text detect cancel`, `ai writer tts cancel`.

## Konsep

AI Lab Agent adalah **chat + tool calling** di atas draft. Model tidak menulis
JSON langsung; ia memanggil tool yang tervalidasi (`ability`) untuk mengubah
draft. Ini penting untuk keamanan dan undo.

```
user prompt
  → conversation state (history + draft context)
  → LLM (server)
  → tool call: add_clips / add_caption / generate_tts / apply_template …
  → validasi + konfirmasi user (untuk operasi besar)
  → eksekusi lewat DraftClient
  → preview + pesan hasil
```

`preview` (372 class) menunjukkan hasil tool ditampilkan sebagai preview
sebelum/sesudah apply, bukan langsung menimpa draft.

## Tool/ability yang mungkin

Dari nama package dan modul editor:

| Tool | Efek |
|---|---|
| `add_media` | tambah klip/foto ke timeline |
| `trim_clip` | potong klip |
| `add_caption` | ASR + subtitle |
| `add_tts` | voiceover |
| `add_music` | musik/AI music |
| `apply_template` | template |
| `auto_cut` | AI clipper |
| `add_effect`/`filter` | visual |
| `export` | render |

Nama tool persis tidak terbaca; yang terbukti adalah `ability`, `chat`,
`conversation`, dan `AgentToolMessageHelper`.

## Model pesan

```json
{
  "id": "msg-1",
  "role": "assistant",
  "type": "tool_call",
  "tool": "add_caption",
  "args": {"language": "en", "style": "default"},
  "status": "pending_confirmation",
  "preview": {"draft_patch": "..."}
}
```

## Implementasi referensi (clean-room)

```kotlin
interface AgentTool {
    val name: String
    suspend fun execute(args: JsonObject, ctx: AgentContext): ToolResult
}

data class ToolResult(
    val message: String,
    val patch: DraftPatch? = null,
    val needsConfirmation: Boolean = false,
)

class AgentRuntime(
    private val llm: LlmClient,
    private val tools: Map<String, AgentTool>,
    private val draft: DraftSession,
) {
    suspend fun run(userText: String): AgentReply {
        val context = draft.summary()          // durasi, track, media count
        val plan = llm.plan(systemPrompt, context, userText, tools.keys)

        for (call in plan.calls) {
            val tool = tools[call.name] ?: continue
            val result = tool.execute(call.args, AgentContext(draft))
            if (result.needsConfirmation) {
                return AgentReply.Confirm(call, result)
            }
            result.patch?.let { draft.apply(it) }
        }
        return AgentReply.Message(plan.text)
    }
}
```

Tool `add_caption`:

```kotlin
class AddCaptionTool(
    private val asr: AsrService,
    private val draft: DraftSession,
) : AgentTool {
    override val name = "add_caption"
    override suspend fun execute(args: JsonObject, ctx: AgentContext): ToolResult {
        val words = asr.transcribe(draft.audioTrack())
        val lines = buildCaptionLines(words)
        val patch = DraftPatch.addTextSegments(lines)
        return ToolResult("Added ${lines.size} captions", patch)
    }
}
```

## AI Writer

AI Writer membuat skrip/teks (hook, caption, ide) lalu bisa langsung
dijadikan voiceover/video. `ai writer credits failed` menunjukkan konsumsi
credit. Alurnya:

```
prompt/topik → generate teks → edit → pilih voice → TTS → segmen audio/teks
```

## Catatan paritas

- **P0**: chat + 2–3 tool aman (caption, trim, music), preview, undo.
- **P1**: konteks draft, konfirmasi tool, riwayat, AI writer + TTS.
- **P2**: multi-agent, auto-edit penuh, tool marketplace, credit.

## Unknown

- Model LLM dan prompt sistem tidak terbaca.
- Protokol streaming chat dan format `DraftPatch` tidak terverifikasi.
- Apakah agent bisa mengubah draft tanpa konfirmasi untuk operasi kecil.
