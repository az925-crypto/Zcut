# 29 — Resource Platform, Config, Analytics & Push

> **Zcut policy:** fitur ini gratis, tanpa paywall/watermark, dan tidak memanggil server aplikasi asli.  
> Config lokal; katalog remote dan telemetry opsional ditandai **BUTUH BACKEND**.

## Bukti

- `com.vega.effectplatform` (740): `artist` (363), `utils` (87), `brand` (64),
  `resource` (49), `repository` (46), `loki` (21), `business` (16),
  `grayword` (8), `network` (6); `VimoEffectManager`.
- `com.vega.libeffect`: `repository` (392), `settings` (385).
- `com.vega.packedresource` (86); `com.ss.ugc.effectplatform` (592).
- `com.vega.property` (2.222): `optional`, `creatives`, `usecase`.
- Config: `com.vega.config`, `com.lemon.lv.config` (2.136).
- Native: `libgeckox_bspatch`, `libkeva`, `libwcdb`, `libnpth`.
- Endpoint: `mon-v2-boot.capcutapi.com`, `feed-api.capcutapi.com`,
  `editor-api.capcutapi.com`; CDN `sf16-sg.tiktokcdn.com`,
  `lf16-beecdn.ibytedtos.com`.
- Telemetry: `com.bytedance.applog`, `com.bytedance.apm6`,
  `com.bytedance.apm6.consumer.slardar`, `assets/slardar.properties`,
  `webview_monitor_js_file_v2/`.
- Push/attribution: ByteDance push, FCM, AppsFlyer.
- Asset: `assets/offline/*` (Lynx), `assets/zip/*`, `assets/fonts/*`,
  `assets/smart_crop/*`.

## Resource platform

Tiga jenis resource:

1. **Effect/Artist** — filter, effect, transition, sticker, template, font,
   makeup, music. Metadata + package download.
2. **Gecko** — bundle/patch untuk resource web/Lynx/JS.
3. **Packed resource** — asset yang dikemas dalam APK, dibuka saat runtime.

Alur download:

```
fetch manifest (version, md5, size, url)
  → cek cache (resourceId + version)
  → download paralel (resume)
  → verifikasi md5
  → ekstrak ke folder versioned
  → atomic switch pointer
  → hapus versi lama
```

`VimoEffectManager` + `parallelDownload` menunjukkan download paralel dengan
listener per item. `grayword`/`loki` menunjukkan eksperimen/gray release.

## Config & feature flag

```json
{
  "config_version": 42,
  "flags": {
    "enable_parallel_export": true,
    "enable_ai_clipper": true,
    "enable_new_paywall": false,
    "max_export_fps": 60
  },
  "ab": {"exp_123": "group_b"}
}
```

Prioritas: remote boot config → AB override → local default. Cache harus
kedaluwarsa dengan aman (jangan kunci fitur hanya dari flag client untuk
urusan keamanan/pembayaran).

## Analytics & APM

- **AppLog** — event produk (nama event, params, session).
- **APM6/Slardar** — crash, ANR, trace, memory, network.
- **Lynx monitor** — performa halaman paywall/growth.
- **Attribution** — AppsFlyer/onelink.

Event penting yang lazim: `edit_enter`, `export_start/end`, `ai_task_start`,
`paywall_show`, `purchase_success`, `share_click`.

## Implementasi referensi (clean-room)

```kotlin
data class ResourceManifest(
    val id: String,
    val version: String,
    val md5: String,
    val url: String,
    val size: Long,
)

class ResourceInstaller(private val root: File, private val http: HttpClient) {
    suspend fun install(m: ResourceManifest): File {
        val dir = File(root, "${m.id}/${m.version}")
        if (File(dir, ".ok").exists()) return dir

        val tmp = File(root, "${m.id}/${m.version}.tmp")
        http.download(m.url, tmp)
        require(tmp.md5() == m.md5) { "checksum mismatch" }

        val unpacked = File(root, "${m.id}/${m.version}.unpacked")
        unzip(tmp, unpacked)
        File(unpacked, ".ok").writeText("")
        unpacked.renameTo(dir)                 // atomic on same fs
        tmp.delete()
        return dir
    }
}
```

Feature flag:

```kotlin
class FeatureFlags(private val remote: FlagStore, private val local: FlagStore) {
    fun isEnabled(key: String, default: Boolean = false): Boolean =
        remote.get(key) ?: local.get(key) ?: default
}
```

Event:

```kotlin
fun track(name: String, params: Map<String, Any?>) {
    analytics.enqueue(Event(name, params + mapOf(
        "ts" to System.currentTimeMillis(),
        "session" to sessionId,
    )))
}
```

## Catatan paritas

- **P0**: config remote sederhana + cache, analytics event dasar, crash log.
- **P1**: resource download + checksum + versioning, feature flag, push.
- **P2**: Gecko patch, gray release, APM trace, attribution, Lynx monitor.

## Unknown

- Skema manifest Artist/Gecko persis tidak terbaca.
- Apakah resource dienkripsi per paket; tidak ada bukti kuat.
- Vendor push yang aktif per region tidak terbaca.
- Jangan menyalin endpoint internal ke produk tanpa kontrak/izin.
