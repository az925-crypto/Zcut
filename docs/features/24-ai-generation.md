# 24 — AI Image/Video Generation & Editing

> **Zcut policy:** fitur ini gratis, tanpa paywall/watermark, dan tidak memanggil server aplikasi asli.  
> Seluruh generasi gambar/video ditandai **BUTUH BACKEND**; UI dan import hasil tetap lokal.

## Bukti

- `com.vega.edit.aigenerator` (1.667 class):
  `fragment/promptedit/{multireference,headtailframe,legacy,multiframe}`,
  `v3/page/{home,inspiration,request,view}`, `viewmodel/history/*`,
  `fragment/execution/history`.
- `com.vega.edit.materialgeneration` (481), `inpainting` (411),
  `outpainting` (357), `aivideoextend` (127), `aifacecover` (274),
  `aitranslator` (230), `imageinterpretation` (212), `aigceffect` (104).
- Client: `AigcGenerateClient`, `AigcWorkflowClient`, `AiCreateClient`,
  `AiEditClient`, `VideoAlgorithmClient`.
- ClipFlow node: `AIEffectClipflowPrepareReferenceNode`,
  `AIEffectClipflowReferenceUploadNode`, `AIEffectClipflowDownloadNode`,
  `AIEffectRemoteTaskParamBuildNode`, `AIEffectClipflowResultParseNode`,
  `AIEffectClipflowHistorySaveNode`, `AIGCEffectStartVETaskMixNode`,
  `AIGCEffectResultNode`.
- String: `ai writer credits failed`, `img_gen_type`, `template_id`,
  `img_md5`.
- Endpoint AI: `aivideo-normal.capcutapi.com`.

## Daftar fitur

| Fitur | Input | Output |
|---|---|---|
| Text to image | prompt + aspect | gambar |
| Text to video | prompt + durasi | video |
| Image to video | 1 gambar + prompt | video |
| Head-tail frame | gambar awal + akhir | video transisi |
| Multi-reference | beberapa gambar acuan | gambar/video konsisten |
| Multi-frame | beberapa keyframe | video |
| Inpainting | gambar + mask | isi area yang dihapus |
| Outpainting | gambar + arah | perluas kanvas |
| Video extend | klip pendek | versi lebih panjang |
| Face cover | wajah sumber + target | face swap |
| Video translate | video + bahasa | audio/subtitle baru |
| Image interpretation | gambar | deskripsi/adegan → video |
| AI effect | klip | effect generated |

## Pipeline generik

```
prompt / referensi
  → validasi + preprocess (resize, crop, mask)
  → upload referensi (jika ada)
  → bangun request (model, aspect, duration, seed)
  → remote task
  → polling status
  → download hasil
  → simpan ke history + material
  → insert ke draft
```

Node `AIEffectClipflowReferenceUploadNode` dan `...DownloadNode` menegaskan
alur upload/download. `AIEffectClipflowHistorySaveNode` menyimpan hasil agar
bisa dipakai ulang.

## Model request

```json
{
  "type": "text_to_video",
  "prompt": "a cat surfing at sunset, cinematic",
  "negative_prompt": "blurry, low quality",
  "aspect_ratio": "9:16",
  "duration_us": 5000000,
  "seed": 12345,
  "model": "default",
  "reference_images": [],
  "mask": null
}
```

## Implementasi referensi (clean-room)

```kotlin
data class GenerationRequest(
    val type: GenerationType,
    val prompt: String,
    val negativePrompt: String = "",
    val aspect: AspectRatio = AspectRatio.PORTRAIT,
    val durationUs: Long? = null,
    val referenceImages: List<File> = emptyList(),
    val mask: File? = null,
    val seed: Long? = null,
)

sealed interface GenerationStatus {
    data class Running(val progress: Int) : GenerationStatus
    data class Done(val files: List<File>) : GenerationStatus
    data class Failed(val code: String, val message: String) : GenerationStatus
}

class GenerationController(
    private val api: AigcApi,
    private val uploader: Uploader,
    private val downloader: FileDownloader,
    private val history: GenerationHistory,
) {
    suspend fun generate(req: GenerationRequest): List<File> {
        val refs = req.referenceImages.map { uploader.upload(it) }
        val task = api.create(req.copy(referenceImages = refs))

        var status: GenerationStatus
        do {
            delay(backoff())
            status = api.status(task.id)
        } while (status is GenerationStatus.Running)

        val done = status as? GenerationStatus.Done
            ?: error("generation failed: $status")
        val local = done.files.map { downloader.download(it, "aigc/${task.id}") }
        history.save(task.id, req, local)
        return local
    }
}
```

Inpainting mask:

```kotlin
fun buildMask(bitmap: Bitmap, strokes: List<Stroke>): Bitmap {
    val mask = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val c = Canvas(mask)
    c.drawColor(Color.BLACK)
    val p = Paint().apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 40f }
    strokes.forEach { c.drawPath(it.path, p) }
    return mask
}
```

## Catatan paritas

- **P0**: text-to-image + insert ke draft; history.
- **P1**: image-to-video, inpainting, aspect/seed, credit handling.
- **P2**: multi-reference, head-tail, video extend, face cover, translate,
  outpainting, AI effect.

## Unknown

- Model/provider yang dipakai (nama model tidak muncul di string publik).
- Batas durasi/resolusi, watermark, dan kebijakan konten tidak terbaca.
- Apakah hasil disimpan permanen di cloud atau hanya URL sementara.
