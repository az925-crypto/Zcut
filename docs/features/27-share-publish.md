# 27 — Share & Publish

> **Zcut policy:** fitur ini gratis, tanpa paywall/watermark, dan tidak memanggil server aplikasi asli.  
> Share sheet Android lokal; publish langsung ke platform membutuhkan SDK/backend masing-masing.

## Bukti

- `com.vega.share`: `ShareManager`, `ShareChannelManager`,
  `ShareChannelApiService`, `ShareFactory`, `SharePanel`, `ShareFragment`,
  `ShareApiImpl`, `ShareCallbackManager`, `SharePolicy`, `ShareReviewPanel`.
- Implementasi kanal: `ShareAweme` (TikTok), `ShareTikTokLite`,
  `ShareInstagram`, `ShareInstagramFeed`, `ShareInstagramStory`,
  `ShareYoutube`, `ShareLinkedIn`, `ShareLemon8Config`, `ShareOther`.
- Package `com.vega.publish`: `template/publish/view/qpresenter` (237),
  `...view/base` (114), `...view/select` (97), `aipost/preview` (94),
  `...view/intelligent` (93), `...view/popularscript` (62),
  `...view/pipvideo` (35), `template/publisher` (24),
  `template/replicate` (28), `template/agent` (29),
  `template/origintemplate` (25), `template/musicreplace` (21).
- Result page: `com.vega.export.edit.resultpage.share` (58),
  `...resultpage/aisuggest` (42), `...resultpage/quickaction` (96).

## Dua jalur

1. **Share hasil export** — file MP4/gambar dikirim ke aplikasi lain.
   Memakai `Intent.ACTION_SEND` / SDK kanal (TikTok share SDK, Instagram
   token, YouTube API).
2. **Publish ke platform/CapCut** — upload ke akun, dengan metadata
   (caption, cover, hashtag, privacy), atau publish template ke marketplace.

## Kanal & mekanisme

| Kanal | Mekanisme |
|---|---|
| TikTok | share SDK / `ShareAweme`, OAuth |
| TikTok Lite | `ShareTikTokLite` |
| Instagram Feed/Story | `ShareInstagram*`, token/URL scheme |
| YouTube | `ShareYoutube`, API/OAuth |
| LinkedIn | `ShareLinkedIn` |
| Lemon8 | `ShareLemon8Config` |
| Other | `Intent` chooser (`ShareOther`) |

`SharePolicy` dan `ShareReviewPanel` menunjukkan ada aturan konten/review
sebelum share (mis. watermark, moderasi, region).

## Publish template / AI post

`com.vega.publish.template`:
- `publisher` — unggah template (draft + cover + metadata),
- `replicate` — buat salinan/variasi template,
- `origintemplate` — atribusi template asli,
- `musicreplace` — ganti musik agar lolos lisensi,
- `agent` — publish dibantu AI.

`aipost` — buat postingan/deskripsi otomatis dari video.
`intelligent` — saran judul/cover/hashtag.
`popularscript` — skrip populer untuk konten.

## Alur share

```
export selesai
  → result page
  → pilih kanal
  → cek login/OAuth
  → isi caption/cover/privacy
  → (opsional) review policy
  → kirim file / upload
  → callback sukses/gagal
```

## Implementasi referensi (clean-room)

```kotlin
data class SharePayload(
    val file: File,
    val mime: String,
    val caption: String = "",
    val hashtags: List<String> = emptyList(),
)

interface ShareTarget {
    val id: String
    fun isInstalled(context: Context): Boolean
    suspend fun share(context: Context, payload: SharePayload): ShareResult
}

class ShareDispatcher(private val targets: Map<String, ShareTarget>) {
    suspend fun share(context: Context, targetId: String, p: SharePayload): ShareResult {
        val target = targets[targetId] ?: return ShareResult.Unsupported
        if (!target.isInstalled(context)) return ShareResult.NotInstalled
        return target.share(context, p)
    }
}
```

Generic intent:

```kotlin
fun shareGeneric(context: Context, p: SharePayload) {
    val uri = FileProvider.getUriForFile(context,
        "${context.packageName}.fileprovider", p.file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = p.mime
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, p.caption)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share"))
}
```

## Catatan paritas

- **P0**: share sheet generic + simpan galeri.
- **P1**: TikTok/Instagram/YouTube direct share, caption/cover.
- **P2**: publish template, AI post, review policy, analytics attribution.

## Unknown

- Apakah upload video ke TikTok memakai SDK resmi atau API internal.
- Aturan `SharePolicy` (watermark, region, moderasi) tidak terbaca.
- Persyaratan template marketplace dan moderasi tidak terbaca.
