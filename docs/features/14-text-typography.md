# 14 — Text, Typography & Text Template

> **Zcut policy:** fitur ini gratis, tanpa paywall/watermark, dan tidak memanggil server aplikasi asli.  
> Implementasi inti on-device; lihat `docs/feature-index.md` untuk status backend.

## Bukti

- Package: `com.vega.libsticker.view.text`, `com.vega.libsticker.texttemplate`,
  `com.vega.libsticker.view.text.texttemplate`, `com.vega.edit.textstart`.
- Client: `TextClient`, `TextTemplateClient`, `TextTemplateEditorClient`.
- Module: `AddText`, `AddMultiText`, `AddTextTemplate`, `AddMultiTextTemplate`,
  `BatchApplyTextTemplate`, `BeginEditTextTemplate`, `EndEditTextTemplate`,
  `BreakUpTextTemplate`, `ApplyAiFont`, `GetRichStrByRangeText`,
  `GetCursorRectText`, `GetFontIdsParser`, `AddLyricEffect`.
- Asset: `assets/fonts/*` (latin, cyrillic, greek, maths, euler; 49 file),
  `assets/textStyle.txt`, `textStyle_v1/v2.txt`, `textStyle.txt`,
  `text_anim_icon_lottie.json`, `text_anim_images/`.
- Native text: `libtttext_lite`, `libserval_svg`, `libskity`.

## Fitur teks

- Input teks, multiline, alignment, spacing, line height.
- Font (bundled + download), weight, italic, underline, color, stroke,
  shadow, background, gradient.
- Text style preset (`.txt` di asset).
- Animasi masuk/keluar/loop (in/out/loop), per karakter/kata/baris.
- Text template (desain kompleks: multi-layer teks + shape + sticker).
- AI font (generate font dari sample), AI text template.
- Rich text per range (sebagian kata beda style).
- Curve text, handwrite.

## Model

```json
{
  "type": "segment_text",
  "content": "Hello {name}",
  "style": {
    "font_id": "font-1", "font_size": 48,
    "color": "#FFFFFF", "stroke": {"width": 2, "color": "#000000"},
    "shadow": {"blur": 4, "offset": [2,2], "color": "#80000000"},
    "align": "center", "line_height": 1.2, "letter_spacing": 0
  },
  "animation": {"in": "fade", "out": "slide", "loop": "none"},
  "ranges": [{"start": 0, "end": 5, "color": "#FF0000"}]
}
```

Template teks lebih kaya: `text_template_id`, layer, slot, parameter ikon,
dan animasi per layer. `BeginEditTextTemplate` / `EndEditTextTemplate`
menunjukkan editing dilakukan pada satu template utuh, bukan per segmen teks.

## Alur render

```
layout teks (Skia/TextShaper)
  → glyph run + font fallback
  → effect (stroke/shadow/background)
  → animation transform per glyph/word
  → composite ke frame
```

## Implementasi referensi (clean-room)

```kotlin
data class TextStyle(
    val fontId: String,
    val sizeSp: Float,
    val color: Int,
    val strokeWidth: Float = 0f,
    val strokeColor: Int = 0,
    val align: TextAlign = TextAlign.Center,
    val letterSpacing: Float = 0f,
    val lineHeight: Float = 1.2f,
)

fun buildTextLayout(text: String, style: TextStyle, maxWidth: Float): Layout {
    val paint = Paint().apply {
        typeface = fontManager.get(style.fontId)
        textSize = style.sizeSp
        color = style.color
    }
    return StaticLayout.Builder.obtain(text, 0, text.length, paint,
        maxWidth.toInt())
        .setAlignment(style.align)
        .setLineSpacing(0f, style.lineHeight)
        .setLetterSpacing(style.letterSpacing)
        .build()
}
```

Animasi per kata:

```kotlin
fun wordProgress(index: Int, count: Int, elapsed: Float): Float {
    val stagger = 0.08f
    val start = index * stagger
    return ((elapsed - start) / 0.3f).coerceIn(0f, 1f)
}
```

## Catatan paritas

- **P0**: input, font, size, color, align, stroke, shadow, animasi dasar.
- **P1**: style preset, rich text, multiline, background, template teks,
  font download, curve text.
- **P2**: AI font, multi-layer template editor, lyric effect, handwrite,
  text-to-video.

## Unknown

- Format `textStyle*.txt` dan text template package tidak terbaca penuh.
- Jumlah dan lisensi font bundled; jangan salin font ke produk tanpa lisensi.

## Status implementasi Zcut (M11)

Yang sudah berjalan di Android:

| Bagian | Implementasi |
|---|---|
| Model | `TextConfig` + `TextStylePreset` di `core/model/Clip.kt` (aditif, schema tetap 1) |
| Operasi timeline | `core/domain/TextEditor.kt` — add/set/remove/sanitize/activeAt/label |
| Animasi | `core/domain/TextAnimator.kt` — RISE, TYPEWRITER, POP, GLITCH (murni, deterministik) |
| Gaya | `core/domain/TextStyle.kt` — resolver preset, alias font sistem, kontrak ukuran 260 dp |
| Render | `ui/components/ZcutText.kt` — fill/stroke/shadow/gradient/background + transform animasi |
| Overlay editor | `feature/editor/TextOverlay.kt` — mengikuti playhead + keyframe opacity/posisi/skala/rotasi |
| Layar tool | `feature/tools/TextScreen.kt` + `TextViewModel.kt`, rute `text?projectId=&clipId=&atMs=` |
| Timeline | Klip teks memakai baris pertama sebagai label |

Batas yang disengaja (bukan paywall):

- **Burn-in teks ke MP4 baru M13.** Preview Media3 tidak menggambar teks;
  overlay teks adalah Compose. UI menyebutkan ini.
- **Import font perangkat ditunda**; hanya font sistem Android. Tidak ada font
  atau aset CapCut yang disalin.
- **Template teks multi-layer** dan **AI font** belum ada. AI font ditandai
  **BUTUH BACKEND** karena butuh model generatif.
- **Satu track `TEXT`**; teks baru yang bentrok waktu digeser ke slot bebas.
- Rich text per-range, curve text, handwrite, lyric effect tetap backlog P1/P2.

Unit test: `TextEditorTest`, `TextAnimatorTest`, `TextStyleTest`, plus perluasan
`ProjectCodecTest` (decode draft pra-M11) dan `PreviewPlanTest` (edit teks tidak
mengubah komposisi media).
