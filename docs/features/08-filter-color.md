# 08 — Filter & Color Adjustment

> **Zcut policy:** fitur ini gratis, tanpa paywall/watermark, dan tidak memanggil server aplikasi asli.  
> Implementasi inti on-device; lihat `docs/feature-index.md` untuk status backend.

## Bukti

- Client: `GlobalFilterClient`, `GlobalAdjustClient`;
  filter single-clip lewat `AddSingleVideoEffect`.
- Package: `com.vega.edit.filter`, `com.vega.edit.palette` (710 class).
- Module: `AddGlobalAdjust`, `AdjustGlobalVideoEffectParams`,
  `ResetGlobalAdjust`, `ResetGlobalAdjustColorCurves`, `ResetGlobalAdjustHsl`,
  `ResetGlobalColorMatchAdjust`, `GetAdjustPresetFrames`.
- Asset: `assets/zip/{brightness,contrast,curves,hsl_v1,tone,
  tone_temperature_v2,tone_temperature_v3,saturation,sharp,shadow,
  highlight,vignetting,LogWheel,PrimaryWheel}.zip`.
- Native: VESDK `ColorHslFilter` (`_ZNK5vesdk3pub14ColorHslFilter…`),
  EffectSDK LUT shader.

## Dua lapis warna

1. **Filter/preset** — LUT atau effect package yang mengubah seluruh gambar.
2. **Adjust** — slider numerik: brightness, contrast, saturation, sharpness,
   temperature, tint, shadow, highlight, vignette, HSL per warna, curves,
   color wheels.

### Parameter yang terlihat

| Grup | Parameter |
|---|---|
| Basic | brightness, contrast, saturation, sharpness, temperature, tint |
| Light | highlight, shadow, fade, vignette |
| HSL | hue/sat/luma per 8 rentang warna |
| Curves | RGB + master, 4 titik |
| Wheels | shadow/midtone/highlight (LogWheel, PrimaryWheel) |
| LUT | file `.png`/`.cube` + intensity |

## Model data

```json
{
  "material_id": "mat-1",
  "filter": {"id": "filter-1", "path": "...", "intensity": 0.8},
  "adjust": {
    "brightness": 0.1, "contrast": 0.2, "saturation": -0.1,
    "temperature": 0.15, "tint": 0.0, "sharp": 0.3,
    "highlight": -0.2, "shadow": 0.1, "fade": 0.0, "vignette": 0.2
  },
  "hsl": [{"hue": 0, "sat": 0, "luma": 0}],
  "curves": {"master": [[0,0],[64,80],[192,180],[255,255]]},
  "lut": {"path": "...", "intensity": 1.0}
}
```

## Pipeline render

```
decode → crop/transform → LUT filter → HSL → curves → wheels → vignette → output
```

Urutan penting. LUT biasanya di awal agar adjust bekerja pada warna netral.
Effect package CapCut memakai `AmazingFeature` (Lua + xshader + LUT texture),
dipanggil EffectSDK.

## Implementasi referensi (clean-room)

```glsl
// fragment shader: basic adjust
uniform sampler2D uTex;
uniform float uBrightness, uContrast, uSaturation, uTemperature;
varying vec2 vUv;

vec3 applyAdjust(vec3 c) {
    c += uBrightness;
    c = (c - 0.5) * (1.0 + uContrast) + 0.5;
    float l = dot(c, vec3(0.2126, 0.7152, 0.0722));
    c = mix(vec3(l), c, 1.0 + uSaturation);
    c.r += uTemperature * 0.1;
    c.b -= uTemperature * 0.1;
    return clamp(c, 0.0, 1.0);
}

void main() { gl_FragColor = vec4(applyAdjust(texture2D(uTex, vUv).rgb), 1.0); }
```

```kotlin
fun applyHsl(rgb: FloatArray, range: Int, dh: Float, ds: Float, dl: Float): FloatArray {
    val hsl = rgbToHsl(rgb)
    if (hueInRange(hsl[0], range)) {
        hsl[0] = (hsl[0] + dh + 1f) % 1f
        hsl[1] = (hsl[1] * (1f + ds)).coerceIn(0f, 1f)
        hsl[2] = (hsl[2] * (1f + dl)).coerceIn(0f, 1f)
    }
    return hslToRgb(hsl)
}
```

## Catatan paritas

- **P0**: brightness/contrast/saturation/temperature, LUT intensity, reset.
- **P1**: HSL 8 warna, curves, highlight/shadow, vignette, preset preview.
- **P2**: color wheels, color match, log/HDR pipeline, AI color grading.

## Unknown

- Skala slider (mis. -1..1 atau -100..100) dan kurva default tidak terbaca.
- Format LUT persis (PNG 2D/3D) tidak diverifikasi.
