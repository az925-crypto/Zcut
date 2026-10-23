# 30 — Split Screen, Gameplay & Live Photo

> **Zcut policy:** fitur ini gratis, tanpa paywall/watermark, dan tidak memanggil server aplikasi asli.  
> Implementasi inti on-device; lihat `docs/feature-index.md` untuk status backend.

## Bukti

- **Split screen**: asset `assets/splitscreen/split_screen_{2..9}.zip`
  (masing-masing `template.json` + `config.json`); module
  `InitSplitScreen`, `SplitScreenPreLoadTemplate`, `SplitScreenPrepareVideo`,
  `SplitScreenReplaceApplyTemplate`, `SplitScreenReplaceSegmentVideo`,
  `SplitScreenReplaceUpdateRatio`, `SplitScreenGetCenterCropParam`,
  `SplitScreenExportJson`, `SplitScreenInitPlayerAndRestoreDraft`,
  `SplitScreenRelease`.
- **Gameplay**: `com.vega.edit.gameplay` (226); module `GameplayVideo`,
  `GameplayAdjustableInfo`, `GameplayBachInfo`, `GameplayV2VInfo`,
  `SetGameplayConfig`, `SetGameplayPath`, `TemplateSetGameplayPath`,
  `UpdateGameplayReferenceMaterial`.
- **Live photo**: module `GetLivePhotoInfo`, `SplitLivePhoto`,
  `LivePhotoAddCombineImages`, `LivePhotoAlignCombineImagesTimeRange`,
  `LivePhotoUpdateCanvasSize`; dukungan galeri/MediaStore motion photo.

## Split screen

Template berisi grid (2–9 sel), masing-masing satu track video dengan
`target_timerange` dan `render_index`. User memilih video per sel; engine
melakukan **center crop** agar rasio sel terpenuhi.

```
pilih layout (2/3/4/…/9)
  → pilih video per sel
  → center crop + scale
  → (opsional) border/background/musik
  → export
```

Center crop:

```kotlin
fun centerCrop(srcW: Int, srcH: Int, dstW: Int, dstH: Int): Rect {
    val srcRatio = srcW.toFloat() / srcH
    val dstRatio = dstW.toFloat() / dstH
    return if (srcRatio > dstRatio) {
        val w = (srcH * dstRatio).toInt()
        val x = (srcW - w) / 2
        Rect(x, 0, x + w, srcH)
    } else {
        val h = (srcW / dstRatio).toInt()
        val y = (srcH - h) / 2
        Rect(0, y, srcW, y + h)
    }
}
```

Template split screen memakai `canvas_config` persegi (contoh 1440×1440,
ratio 1:1) dan `config` yang menyinkronkan subtitle/lyrics.

## Gameplay

Fitur untuk konten gaming: rekam gameplay + facecam, lalu compose:

- gameplay sebagai background,
- facecam di pojok (mask bulat/persegi),
- chroma key facecam (jika green screen),
- template intro/outro, musik, subtitle,
- `V2V` = video-to-video (mungkin gaya/efek pada gameplay).

`GameplayAdjustableInfo`/`GameplayBachInfo` menunjukkan parameter yang bisa
disesuaikan per segmen; `UpdateGameplayReferenceMaterial` mengganti materi
referensi (mis. gameplay baru) tanpa mengubah layout.

## Live photo

Motion photo = JPEG + video pendek. Alur:

```
deteksi live photo (MediaStore / XMP)
  → SplitLivePhoto: pisahkan still + video
  → edit still (photo editor) dan/atau video
  → LivePhotoAddCombineImages + align time range
  → gabung kembali / export sebagai video
```

`LivePhotoUpdateCanvasSize` penting karena still dan video bisa berbeda
resolusi/rasio; kanvas harus disamakan sebelum combine.

## Implementasi referensi (clean-room)

Split screen composite:

```kotlin
data class Cell(val rect: RectF, val video: MediaAsset)

fun renderSplit(cells: List<Cell>, canvasW: Int, canvasH: Int): Bitmap {
    val out = Bitmap.createBitmap(canvasW, canvasH, Bitmap.Config.ARGB_8888)
    val c = Canvas(out)
    c.drawColor(Color.BLACK)
    for (cell in cells) {
        val frame = decodeFrame(cell.video, timeUs = 0)
        val cropped = crop(frame, centerCrop(frame.width, frame.height,
            cell.rect.width().toInt(), cell.rect.height().toInt()))
        c.drawBitmap(cropped, null, cell.rect, Paint(Paint.FILTER_BITMAP_FLAG))
    }
    return out
}
```

Live photo split:

```kotlin
data class LivePhoto(val still: File, val video: File, val durationUs: Long)

fun splitLivePhoto(source: File): LivePhoto {
    val still = extractFirstFrame(source)   // atau JPEG utama
    val video = extractVideoTrack(source)
    return LivePhoto(still, video, probeDuration(video))
}
```

## Catatan paritas

- **P0**: split screen 2–4 sel + center crop + export.
- **P1**: 5–9 sel, musik/subtitle, gameplay facecam overlay, live photo split.
- **P2**: V2V gameplay, template gameplay, live photo recombine native,
  motion photo export.

## Unknown

- Format layout split screen di `template.json` (grid eksplisit vs posisi
  absolut) tidak terbaca; contoh hanya memuat 2 track.
- Apakah live photo di-recombine menjadi motion photo atau selalu video.
- `GameplayV2VInfo` bisa berarti video-to-video AI; tidak ada bukti model.
