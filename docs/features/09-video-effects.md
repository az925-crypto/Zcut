# 09 — Video Effects (EffectSDK / AmazingFeature)

> **Zcut policy:** fitur ini gratis, tanpa paywall/watermark, dan tidak memanggil server aplikasi asli.  
> Implementasi inti on-device; lihat `docs/feature-index.md` untuk status backend.

## Bukti

- Package: `com.vega.edit.videoeffect` (735 class), `com.vega.libeffect`,
  `com.vega.effectplatform.artist` (363 class), `com.vega.edit.aigceffect`.
- Module: `AddGlobalVideoEffect`, `AddSingleVideoEffect`,
  `AdjustGlobalVideoEffectParams`, `AdjustGlobalVideoEffectRenderIndex`,
  `AddVideoAnimation`, `AddVideoSmartMotion`, `AddVideoObjectLocked`,
  `SetEffectRenderIndexCallback`, `EnableEffectFaceRecognition`.
- Native: `com.bef.effectsdk.*` + EffectSDK di `libcccreator.so`.
- Asset: `assets/zip/*.zip` berisi `config.json` +
  `amazingfeature/{content.json,main.scene,sticker.config,Transform.lua,
  xshader/*.frag|vert,texture/*.png,material/*}`.
- Contoh `content.json`: requirement `faceDetect`, `matting`, `handDetect`,
  `3dFaceMeshPerspective`, `hairColor`.

## Format effect package

```
brightness.zip
├── config.json            → { name, version, effect.Link[] }
└── amazingfeature/
    ├── content.json       → requirement + content
    ├── sticker.config     → detectFlags, faceModelName, type=amazing
    ├── main.scene         → scene graph (binary)
    ├── Transform.lua      → event handler (intensity)
    ├── xshader/           → vertex/fragment shader
    ├── texture/           → LUT / mask / sprite
    └── material/          → material binding
```

`config.json` menautkan beberapa `AmazingFeature` dengan `zorder` berbeda.
`Transform.lua` menerima `Amaz.AppEventType.SetEffectIntensity` lalu mengubah
`material["intensity"]`.

## Kategori efek

- Visual: glow, glitch, VHS, light leak, particle, shake, 3D, blur.
- AR/face: face reshape, makeup, hair color, sticker wajah.
- Body: body reshape, long legs, slim.
- AI effect: hasil generate (AIGC) di-apply sebagai effect/segment.
- Effect global (seluruh timeline) vs single clip.

## Alur apply

```
panel effect → fetch list (Artist API)
  → download package (cache by resourceId+md5)
  → validasi requirement vs device (face/matting/CPU)
  → add effect ke material/segment + intensity
  → render graph memasang EffectSDK node
```

`EnableEffectFaceRecognition` + `GetEffectFaceBoundingBoxInfos` menunjukkan
effect bisa meminta face detection dan mendapatkan bounding box untuk
positioning sticker/AR.

## Implementasi referensi (clean-room)

```kotlin
data class EffectPackage(
    val id: String,
    val name: String,
    val version: String,
    val root: File,
    val requirements: EffectRequirements,
    val zOrder: Int,
)

data class EffectRequirements(
    val faceDetect: Boolean = false,
    val matting: Boolean = false,
    val handDetect: Boolean = false,
    val faceMesh3d: Boolean = false,
)

class EffectRepository(private val cache: File, private val api: ArtistApi) {
    suspend fun resolve(id: String): EffectPackage {
        val dir = File(cache, id)
        if (!File(dir, "config.json").exists()) download(id, dir)
        val cfg = Json.decodeFromString<EffectConfig>(File(dir, "config.json").readText())
        return EffectPackage(id, cfg.name, cfg.version, dir,
            parseRequirements(File(dir, "amazingfeature/content.json")),
            cfg.effect.link.first().zorder)
    }
}
```

Render node:

```kotlin
class EffectNode(
    private val packageDir: File,
    private val intensity: Float,
) : RenderNode {
    override fun render(input: Texture, ctx: RenderContext): Texture {
        ctx.effect.setIntensity("intensity", intensity)
        return ctx.effect.process(packageDir, input)
    }
}
```

## Catatan paritas

- **P0**: 5–10 efek shader sendiri (tanpa format AmazingFeature).
- **P1**: download/cache package, intensity, preview, kategori.
- **P2**: AR/face effect, body reshape, AI effect, marketplace.

## Unknown

- Format `main.scene` binary dan API Lua engine tidak didokumentasikan.
- Persyaratan device/AB test per effect tidak terbaca.
