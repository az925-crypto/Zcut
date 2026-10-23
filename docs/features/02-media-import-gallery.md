# 02 — Media Import & Gallery

> **Zcut policy:** fitur ini gratis, tanpa paywall/watermark, dan tidak memanggil server aplikasi asli.  
> Implementasi inti on-device; lihat `docs/feature-index.md` untuk status backend.

## Bukti

- Package: `com.vega.gallery` (51 subpackage: `ui/local`, `ui/cloud`,
  `ui/creatives`, `ui/material`, `local/repo`, `utils/transfer`).
- Permission manifest: `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`,
  `READ_MEDIA_AUDIO`, `READ_MEDIA_VISUAL_USER_SELECTED`,
  `ACCESS_MEDIA_LOCATION`.
- Native: `libttheif_dec`, `libstatic-webp`, `libgifimage`, `libyuv`,
  `libttdemux`, `libttffmpeg`.
- Room: `MediaDataTransEntity` (cache hasil transcode).

## Perilaku

1. **Sumber**: lokal (MediaStore/Photo Picker), cloud (`ui/cloud`), material
   library CapCut, kreatif AI, dan enterprise/team.
2. **Filter**: tipe (video/foto/audio), album, durasi, resolusi, source.
3. **Preview cepat**: thumbnail + metadata; video di-probe tanpa decode penuh.
4. **Import**: URI disalin ke workspace privat (bukan dipakai langsung), lalu
   metadata + hash dicatat. File yang tidak kompatibel ditranscode.
5. **Proxy**: video 4K/berat dibuat proxy rendah untuk timeline; export tetap
   memakai sumber asli.
6. **Format**: MP4/MOV (H.264/HEVC/ByteVC1/VC2), JPEG/PNG/WebP/HEIF, GIF,
   audio AAC/MP3/Opus/WAV.

Alasan menyalin ke workspace: stabilitas URI, izin scoped storage, dan agar
draft bisa dibuka ulang walau file asli dipindah.

## Model data

```kotlin
data class MediaAsset(
    val id: String,
    val uri: String,              // content:// saat import
    val localPath: String?,       // hasil copy
    val type: MediaType,          // VIDEO, IMAGE, AUDIO, GIF, LIVE_PHOTO
    val mime: String,
    val width: Int, val height: Int,
    val durationUs: Long,
    val rotation: Int,
    val hasAudio: Boolean,
    val sizeBytes: Long,
    val md5: String,
    val proxyPath: String? = null,
)
```

`MediaDataTransEntity` menyimpan pemetaan sumber → hasil transcode supaya
import kedua tidak mengulang pekerjaan.

## Alur import

```
pick URI
  → probe metadata (MediaMetadataRetriever / FFprobe)
  → cek codec & resolusi
  → butuh proxy/transcode?
       ya  → MediaTranscoder → cache (Room)
       no  → copy ke workspace
  → buat MediaAsset
  → tambah ke draft sebagai material + segment
```

## Implementasi referensi (clean-room)

```kotlin
class MediaImporter(
    private val context: Context,
    private val workspace: File,
    private val transcoder: MediaTranscoder,
) {
    suspend fun import(uri: Uri): MediaAsset = withContext(Dispatchers.IO) {
        val meta = probe(uri)                       // width/height/duration/rotation
        val ext = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(meta.mime) ?: "bin"
        val target = File(workspace, "${UUID.randomUUID()}.$ext")

        context.contentResolver.openInputStream(uri)!!.use { input ->
            target.outputStream().use { input.copyTo(it) }
        }

        val proxy = if (meta.needsProxy) transcoder.makeProxy(target, meta) else null
        MediaAsset(
            id = UUID.randomUUID().toString(),
            uri = uri.toString(),
            localPath = target.absolutePath,
            type = meta.type, mime = meta.mime,
            width = meta.width, height = meta.height,
            durationUs = meta.durationUs, rotation = meta.rotation,
            hasAudio = meta.hasAudio, sizeBytes = target.length(),
            md5 = target.md5(), proxyPath = proxy?.absolutePath,
        )
    }
}
```

## Catatan paritas

- **P0**: Photo Picker + copy ke workspace + metadata + thumbnail.
- **P1**: proxy/transcode, HEIF/WebP/GIF, cloud import, album.
- **P2**: live photo, material library, AI creative assets.

## Unknown

- Ambang pasti kapan proxy dibuat (resolusi/bitrate/durasi) tidak terbaca.
- Apakah hash memakai MD5 file penuh atau sample; hanya nama `md5` yang terlihat.
