# 31 — Ad Maker, Script-to-Video & Business

> **Zcut policy:** fitur ini gratis, tanpa paywall/watermark, dan tidak memanggil server aplikasi asli.  
> Script-to-video dengan TTS/template lokal; smart ad generatif ditandai **BUTUH BACKEND**.

## Bukti

- Package `com.vega.adeditor`: `scriptvideo/dock` (377), `component/dock`
  (339), `maker/panel` (280), `scriptvideo/vm` (217), `component/view` (223),
  `scriptvideo/fragment` (190), `smartad/preview` (164), `maker/view` (132),
  `scripttovideo/repo` (110), `audio/tonecompare` (81),
  `scriptvideo/watermark` (80), `scriptvideo/edit` (78),
  `voiceover/view` (76), `maker/digitalhuman` (65),
  `scripttovideo/v2` (65), `scriptvideo/panel` (63).
- Activity: `AdComponentEditActivity`, `AdPartEditMySceneActivity`,
  `AdPartMyScenePreviewActivity`, `AdPartPromptActivity`,
  `VoiceoverEditActivity`, `SmartAdDebugActivity`, `AdCropActivity`.
- `com.vega.business.ad`: `impl` (158), `config` (52), `view` (44),
  `pixel` (6), `report` (5), `feedback` (4).
- Modul: `AddTextToVideoAudioModule`, `AddTextToVideoMaterialModule`,
  `AdComponentBatchMove`, `AdComponentBatchSplit`,
  `AdComponentUpdateTimeRange`, `AdCubeMediaChangeSpeed`,
  `AdMakerDraftManager`, `AdScriptToScriptVideoConfig`.
- Aset: `ad_maker_ripple_circle_anim.json`, `ad_script_loading/*`,
  `ad_script_loading_small/data.json`.

## Fitur

| Fitur | Deskripsi |
|---|---|
| Script to Video | skrip → scene → klip + voiceover + subtitle |
| Smart Ad | prompt/produk → draf iklan |
| Ad components | product card, CTA, harga, logo, badge |
| Voiceover | TTS + tone compare (bandingkan voice) |
| Digital human presenter | avatar membawakan script |
| Watermark | logo/brand pada iklan |
| Product album | beberapa foto produk → video |
| Ad preview/report | preview + pixel/report |

## Alur script-to-video

```
input script / produk
  → pecah jadi scene (hook, benefit, CTA)
  → pilih media per scene (album/prompt/AI)
  → voiceover (TTS) + tone compare
  → subtitle otomatis
  → overlay komponen iklan (harga/CTA/logo)
  → preview → export
```

`AdScriptToScriptVideoConfig` dan `scripttovideo/repo` menunjukkan konfigurasi
per scene, bukan satu video monolitik.

## Model

```json
{
  "ad": {
    "product": {"name": "Shoes", "price": "$49", "logo": "logo.png"},
    "scenes": [
      {"role": "hook", "text": "Tired of...", "media": "a.mp4"},
      {"role": "benefit", "text": "...", "media": "b.jpg"},
      {"role": "cta", "text": "Buy now", "media": "c.jpg"}
    ],
    "voice": {"id": "v1", "speed": 1.0},
    "components": [
      {"type": "product_card", "start": 0, "duration": 3000000},
      {"type": "cta", "start": 12000000, "duration": 3000000}
    ],
    "watermark": {"path": "brand.png", "position": "bottom_right"}
  }
}
```

## Implementasi referensi (clean-room)

```kotlin
data class AdScene(
    val role: SceneRole,
    val text: String,
    val media: File?,
    val durationUs: Long,
)

data class AdProject(
    val product: Product,
    val scenes: List<AdScene>,
    val voice: VoiceConfig,
    val components: List<AdComponent>,
)

class AdComposer(
    private val tts: TtsManager,
    private val subtitles: CaptionService,
) {
    suspend fun compose(project: AdProject): Draft {
        val draft = Draft.create(aspect = AspectRatio.PORTRAIT)
        var cursor = 0L

        for (scene in project.scenes) {
            val clip = scene.media?.let { draft.addMedia(it) }
            val audio = tts.speak(project.voice.engine,
                TtsRequest(scene.text, project.voice.id, project.voice.language))

            draft.addScene(
                text = scene.text,
                media = clip,
                audio = audio.audio,
                startUs = cursor,
            )
            cursor += audio.durationUs
        }

        project.components.forEach { draft.addOverlay(it, cursor) }
        draft.addWatermark(project.product.logo)
        draft.recomputeDuration()
        return draft
    }
}
```

Tone compare (A/B voice):

```kotlin
suspend fun compareTones(text: String, voices: List<String>): List<TtsResult> =
    voices.map { tts.speak(it, TtsRequest(text, it, "en")) }
```

## Catatan paritas

- **P0**: script → scene → TTS → subtitle → export; watermark.
- **P1**: komponen produk/CTA, tone compare, template iklan, preview.
- **P2**: smart ad AI, digital human presenter, pixel/report, A/B performa.

## Unknown

- Apakah smart ad memakai model generatif atau aturan template.
- Format komponen iklan dan dukungan platform (Meta/TikTok ads) tidak terbaca.
- Klaim performa iklan/pixel tidak dapat diverifikasi dari APK.
