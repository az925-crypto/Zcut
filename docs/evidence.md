# Evidence Index — CapCut 19.8.0

Dokumen ini menghubungkan klaim di `docs/features/*` ke artifact asal.
Semua analisis **statis**; tidak ada eksekusi aplikasi.

## 1. Metodologi

| Langkah | Tool | Output |
|---|---|---|
| Ekstrak APKS | `unzip` | `work/extracted/{base,split_*}.apk` |
| Metadata | `aapt dump badging` | package, sdk, versi |
| Manifest | `aapt dump xmltree` | permission, activity, service |
| Dex inventory | `unzip` + `strings` | 35 dex, 275.246 descriptor |
| Class inventory | regex descriptor | `/tmp/classes2.txt` |
| Native symbols | `nm -D --defined-only` | JNI, SWIG module, struct field |
| Native strings | `strings` | versi engine, error, endpoint |
| Asset | `unzip -l`, baca JSON/zip | template, effect, config, font |
| Draft schema | cari string JSON di dex | mini-draft `142.0.0` |

Tidak dipakai: JADX penuh, `dexdump`, emulator. Alasannya keterbatasan RAM
(device 7.4 GB, tersedia <2 GB saat analisis). Analisis per-class tidak
dilakukan; kesimpulan diambil dari nama package/class, simbol JNI, string,
dan format file.

## 2. Inventory artifact

| Item | Nilai | Sumber |
|---|---|---|
| Package | `com.lemon.lvoverseas` | `aapt dump badging` |
| Versi | 19.8.0 / 19801600 | idem |
| minSdk / target | 23 / 36 | idem |
| Launcher | `com.vega.main.MainActivity` | idem |
| Split | base, arm64_v8a, xhdpi, in, id | isi APKS |
| Dex | 35 file, ~298 MB | `ls base_x/classes*.dex` |
| Class descriptor | 275.246 unik | `/tmp/classes2.txt` |
| Native | 162 `.so`, ~218 MB | `so_all/` |
| Asset | 1.075 file | `/tmp/assets.txt` |
| Nama operasi/tipe native | 1.390 | `/tmp/modules.txt` |
| `*Client` facade | 95 | `com/vega/middlebridge/client/*Client` |
| Effect package | 50 zip | `assets/zip/*.zip` |
| Font | 49 file | `assets/fonts/` |

Build metadata:

- `assets/ve.version.lock` → VESDK `21.2.0`.
- `assets/git_info` → RetouchMiddleware SHA/branch.
- `assets/developer_info` → metadata developer (tidak disalin; berisi PII).
- `libcccreator.so` → `VESDK`, `VESDKVersion`, `bef_effect_get_sdk_version`.

## 3. Package index

Angka = jumlah class descriptor (termasuk inner class), bukan LOC.

| Package | Jumlah | Dipakai di |
|---|---:|---|
| `com.vega.edit.editpage` | 2.325 | 01, 04, 08, 09, 14 |
| `com.vega.edit.aiclipper` | 1.764 | 22 |
| `com.vega.edit.aigenerator` | 1.667 | 24 |
| `com.vega.edit.digitalhuman` | 1.114 | 25 |
| `com.vega.edit.videoeffect` | 735 | 07, 09 |
| `com.vega.edit.palette` | 710 | 08 |
| `com.vega.edit.matting` | 699 | 11 |
| `com.vega.edit.materialgeneration` | 481 | 24 |
| `com.vega.edit.inpainting` | 411 | 24 |
| `com.vega.edit.outpainting` | 357 | 24 |
| `com.vega.edit.videoagent` | 392 | 23 |
| `com.vega.edit.gameplay` | 226 | 30 |
| `com.vega.audio.tone` | 2.135 | 16, 17 |
| `com.vega.audio.aimusic` | 1.077 | 18 |
| `com.vega.audio.library` | 726 | 16 |
| `com.vega.audio.aiaudio` | 586 | 18 |
| `com.vega.audio.dubbing` | 448 | 15, 16 |
| `com.vega.audio.lipsync` | 366 | 25 |
| `com.vega.transcriptedit` | 838 | 15 |
| `com.vega.libsticker` | 5.472 | 13, 14, 15 |
| `com.vega.image` | 5.988 | 26 |
| `com.xt.retouch` | 21.351 | 12, 26 |
| `com.xt.edit` | 4.650 | 12, 26 |
| `com.vega.cutsameedit` | 5.930 | 21 |
| `com.vega.libcutsame` | 4.392 | 21 |
| `com.vega.edward` | 1.370 | 21 |
| `com.vega.ailabagent` | 3.941 | 23 |
| `com.vega.cloud` | 2.729 | 20 |
| `com.lemon.clipmonetize` | 2.816 (commerce page) | 28 |
| `com.vega.effectplatform` | 740 | 09, 29 |
| `com.vega.property` | 2.222 | 29 |
| `com.vega.recorder` | 2.205 | 26 |
| `com.vega.adeditor` | 4.799 | 31 |
| `com.vega.middlebridge.swig` | 14.167 | 01, 03, 04, 05, 06, 19 |

## 4. Native library index

Top library (ukuran):

| Library | Ukuran | Peran | Doc |
|---|---:|---|---|
| `libmiddle-bridge.so` | 62 MB | Lyra NLE, 49.020 simbol JNI | 01, 03, 04, 19 |
| `libcccreator.so` | 40 MB | VESDK 21.2 + EffectSDK | 03, 08, 09, 19 |
| `libmiddleware.so` | 25 MB | middleware media/algorithm | 02, 11, 12 |
| `libres_pool.so` | 9,9 MB | resource pool | 29 |
| `libsscronet.so` | 6,0 MB | networking | 20, 29 |
| `libttffmpeg.so` | 5,8 MB | FFmpeg mux/decode/encode | 02, 19 |
| `libaudioeffect.so` | 5,1 MB | audio DSP | 16 |
| `libclipflow.so` | 4,9 MB | DAG workflow | 22, 24 |
| `liblynx.so` | 4,1 MB | Lynx UI | 28, 29 |
| `liblens.so` | 3,6 MB | AR/lens | 09, 12 |
| `libttmplayer.so` | 3,6 MB | player | 03 |
| `libbytenn.so` | 3,1 MB | neural inference | 11, 12, 15 |
| `libretouch_sdk.so` | 2,1 MB | XT Retouch | 12, 26 |
| `libquick.so` | 1,9 MB | QuickJS (Lua/JS effect?) | 09 |

Kodek/media: `libByteVC1_dec`, `libbyteVC2dec`, `libbytevc1enc`,
`libttdemux`, `libyuv`, `libttheif_dec`, `libstatic-webp`, `libgifimage`,
`libfdk-aac`, `libopus`, `libnativeaudio`, `libvcn`, `libvcnverify`.

Speech/AI: `libspeechsdk`, `libspeechengine`, `libspeechepg`, `libwhisper`,
`libfastcv`, `libaudio_fingerprint_sdk`.

Infra: `libkeva` (KV), `libwcdb` (DB), `libgeckox_bspatch` (patch),
`libmetasec_ov`, `libpns_crypto`, `libttboringssl`, `libttcrypto`,
`libshadowhook`, `libbytehook`, `libtt_c2pa_sdk` (content credentials).

## 5. Bukti SWIG / modul editor

Contoh simbol JNI (dari `nm -D`):

- Draft: `DraftBeginTransaction`, `DraftCommit`, `DraftAmend`,
  `DraftCanUndo`, `DraftCanRedo`, `DraftDeepCopy`.
- Track/segment: `AddTrack`, `RemoveEmptyTrack`, `MoveSegment`,
  `SplitSegment`, `SetFixClashInSameTrack`.
- Export: `AdapterParamModuleJNI_ExportConfig_1...` dengan field
  `codec`, `bitrate`, `fps`, `enable_hdr`, `compile_with_alpha`,
  `is_audio_only`, `is_video_only`, `enable_parallel_export`, dll.
- Keyframe: `TimeKeyframe` field `play_head`, `position`, `rotation`, `scale`.
- Canvas background: `CanvasBackgroundBlurParam`, `CanvasBackgroundColorParam`,
  `CanvasBackgroundImageParam`, `CanvasUpdateTransform`.
- Mask: `MaskParam` field `feather`, `invert`, `center_x/y`, `width`, `height`,
  `rotation`, `round_corner`, `mask_effect_type`.
- Beauty: `BeautyAdjustParam`, `BodyReshapeInfo`, `HeadReshapeInfo`,
  `EnableMultFacesEffect`.
- Split screen: `InitSplitScreen`, `SplitScreenPrepareVideo`,
  `SplitScreenReplaceApplyTemplate`, `SplitScreenGetCenterCropParam`,
  `SplitScreenExportJson`.
- Gameplay: `GameplayVideo`, `GameplayAdjustableInfo`, `GameplayBachInfo`,
  `GameplayV2VInfo`, `SetGameplayConfig`, `SetGameplayPath`.
- Live photo: `GetLivePhotoInfo`, `SplitLivePhoto`,
  `LivePhotoAddCombineImages`, `LivePhotoAlignCombineImagesTimeRange`,
  `LivePhotoUpdateCanvasSize`.
- Digital human: `AddDigitalHuman`, `AddDigitalHumanTemplate`,
  `AddDigitalHumanTemplateSubtitles`, `AddDigitalHumanModelDressing`.

Jumlah simbol `Java_com_vega_middlebridge*`: 49.020. Struct/tipe SWIG unik:
5.975. Nama operasi/tipe yang diekstrak: 1.390.

## 6. Bukti asset

| Asset | Isi | Doc |
|---|---|---|
| `assets/splitscreen/split_screen_{2..9}.zip` | `template.json` + `config.json` | 21, 30 |
| `assets/zip/*.zip` (50) | effect package: `config.json` + `amazingfeature/*` | 08, 09 |
| `assets/fonts/*` (49) | font latin/cyrillic/greek/maths | 14 |
| `assets/offline/image_lynx_*` | halaman Lynx subscription/growth | 28, 29 |
| `assets/smart_crop/*` | OFX plugin + `algorithmConfig.json` | 11 |
| `assets/textStyle*.txt` | preset style teks | 14 |
| `assets/simkit_rate_settings/other.json` | rate/bitrate | 19 |
| `assets/ve.version.lock` | versi VESDK | 09, 19 |
| `assets/git_info`, `developer_info` | metadata build (PII dihapus) | arsitektur |
| `assets/ailab_tab_v2/data.json` | konfigurasi tab AI (Lottie) | 23 |
| `assets/ai_writer_template/*.json` | Lottie AI writer | 23 |
| `assets/zip/aigc_text_template.zip` | template teks AIGC | 21, 24 |
| `assets/Brush2D_*.zip` | brush/mask | 10, 12 |
| `assets/zip/FaceReshape_V{2,3}.zip` | warp wajah | 12 |
| `assets/zip/Chroma_V*.zip` | chroma key | 10 |
| `assets/zip/MattingBlend*.zip` | matting blend | 11 |

Struktur effect package:

```
config.json                  → name/version/effect.Link
amazingfeature/content.json  → requirement (faceDetect, matting, handDetect)
amazingfeature/sticker.config
amazingfeature/main.scene
amazingfeature/Transform.lua
amazingfeature/xshader/*.frag|vert
amazingfeature/texture/*
amazingfeature/material/*
```

## 7. Bukti skema draft/template

Dua sumber:

1. String mini-draft di `classes2.dex` (offset sekitar 3.029.845):

```json
{"header":{"version":"1","type":"minidraft","modify_timestamp":123456},
 "draft":{"id":"...","new_version":"...142.0.0","name":"display name",
 "duration":1000,"fps":30,"canvas_config":{...},
 "tracks":[{"id":"","attribute":0,"segments":[{"id":""}]}],
 "config":{"video_mute":false,"maintrack_adsorb":true,
 "export_range":{"start":0,"duration":0}}},
 "segments":[...]}
```

2. `assets/splitscreen/split_screen_2.zip → template.json` (draft nyata):

- Top-level: `canvas_config`, `color_space`, `config`, `create_time`,
  `duration`, `extra_info`, `fps`, `id`, `keyframes`, `last_modified_platform`,
  `materials`, `name`, `new_version`, `platform`, `render_index_track_mode_on`,
  `source`, `static_cover_image_path`, `tracks`, `update_time`, `version`.
- `materials.videos[]`: `path`, `duration`, `width`, `height`, `crop`,
  `has_audio`, `type`, `material_id`, dll.
- `tracks[]`: `attribute`, `flag`, `id`, `segments`, `type`.
- `segments[]`: `cartoon`, `clip`, `enable_adjust`, `enable_color_curves`,
  `enable_color_wheels`, `enable_lut`, `group_id`, `hdr_settings`, `id`,
  `intensifies_audio`, `is_tone_modify`, `last_nonzero_volume`,
  `material_id`, `render_index`, `reverse`, `source_timerange`, `speed`,
  `target_timerange`, `track_attribute`, `track_render_index`, `visible`,
  `volume`.

Tipe segmen: `segment_video`, `segment_audio`, `segment_text`,
`segment_sticker`, `segment_image_sticker`, `segment_text_template`,
`segment_video_effect`, `segment_composition`, `segment_tail_leader`,
`segment_handwrite`, `segment_filter`, `segment_shape`,
`segment_picture_adjust`, `segment_adcube`.

## 8. Bukti engine AI & pipeline

- `libclipflow.so` + `com.vega.clipflow.ClipflowServer` (JNI:
  `clipflowServerJniInit`, `invoke`, `invokeAsync`, `invokeSync`).
- 440 class berakhiran `Node`; contoh: `ASRNode`, `VideoASRNode`,
  `AEDNode`, `AIRecognitionHighlightNode`, `VideoContentUnderstandNode`,
  `AITemplateRemoteTaskParamNode`, `AITemplateRemoteTaskParseNode`,
  `AIApplyDraftNode`, `AiAudioBuildRequestNode`,
  `AIEffectRemoteTaskParamBuildNode`.
- Agent: `com.vega.ailabagent.unifyagent.{chat,conversation,ability,preview,
  history,sug,msglist}`.
- AI generation: node `AIEffectClipflowPrepareReferenceNode`,
  `AIEffectClipflowReferenceUploadNode`, `AIEffectClipflowDownloadNode`,
  `AIEffectClipflowResultParseNode`, `AIGCEffectStartVETaskMixNode`.

## 9. Bukti endpoint & SDK

Domain (dari string dex, dihitung kemunculan):

| Domain | Fungsi |
|---|---|
| `feed-api.capcutapi.com` | feed/home |
| `editor-api.capcutapi.com` | editor/cloud |
| `aivideo-normal.capcutapi.com` | AI video |
| `mon-v2-boot.capcutapi.com` | config/boot |
| `mon.byteoversea.com`, `mon.isnssdk.com` | telemetry |
| `sf16-sg.tiktokcdn.com`, `lf16-beecdn.ibytedtos.com` | CDN |
| `www.capcut.com`, `www.capcut.net` | web/legal |
| `api-retouchpics-intranet.byteintl.net` | retouch (internal) |

SDK pihak ketiga yang terlihat:

- Billing: Google Play Billing, GlobalPay, PayPal/Paylution.
- Ads: AppLovin MAX, Pangle, MBridge, Vungle, Facebook Audience.
- Analytics: AppLog, APM6, Slardar, AppsFlyer.
- TTS: ElevenLabs, Microsoft Azure, Qwen, internal V3.
- UI growth: Lynx.

## 10. Manifest & permission

Permission utama (dari `aapt dump xmltree`):

`CAMERA`, `RECORD_AUDIO`, `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`,
`READ_MEDIA_AUDIO`, `READ_MEDIA_VISUAL_USER_SELECTED`,
`ACCESS_MEDIA_LOCATION`, `READ/WRITE_EXTERNAL_STORAGE`,
`POST_NOTIFICATIONS`, `FOREGROUND_SERVICE*`, `SYSTEM_ALERT_WINDOW`,
`com.android.vending.BILLING`, `AD_ID`, `ACCESS_ADSERVICES_*`.

Activity penting: `com.vega.main.MainActivity`,
`com.vega.cutsameedit.biz.template.compose.loading.LoadingActivity`,
`SmartVlogLoadingActivity`, `TextStartLoadingActivity`,
`DigitalHumanTemplateLoadingActivity`, `BusinessVideoLoadingActivity`,
`AiLabAgentUniversalPreviewActivity`, `MinimalEditorActivity`,
`VoiceoverEditActivity`, `ToneDebugActivity`, `CloneToneActivity`,
`AdComponentEditActivity`, `AdPartPromptActivity`, `SmartAdDebugActivity`.

## 11. Bukti per fitur

| Doc | Bukti kunci |
|---|---|
| 01 | `Draft*`, `AddTrack`, `SplitSegment`, mini-draft schema |
| 02 | `com.vega.gallery`, `MediaDataTransEntity`, `libttheif_dec` |
| 03 | `PlayerClient`, `libttmplayer`, `SetPreviewTimeRange` |
| 04 | `MoveSegment`, `SplitSegment`, `SetFixClashInSameTrack` |
| 05 | `ChangeSpeed`, `StartConvertSlowMotion`, `ConvertReverseVideo` |
| 06 | `AddCommonKeyframe`, `TimeKeyframe` |
| 07 | `AddVideoTransition`, `AITransition*` |
| 08 | `GlobalFilterClient`, `assets/zip/{curves,hsl_v1,...}` |
| 09 | `AddSingleVideoEffect`, EffectSDK, `amazingfeature/*` |
| 10 | `MaskParam`, `Chroma_V320`, `GreenScreenClient` |
| 11 | `AddMatting`, `MattingBlendV2`, `smart_cut.ofx` |
| 12 | `BeautyAdjustParam`, `FaceReshape_V3`, `libretouch_sdk` |
| 13 | `AddImageSticker`, `StartVideoTracking` |
| 14 | `AddText`, `AddTextTemplate`, `assets/fonts`, `textStyle*.txt` |
| 15 | `GenerateTranscriptInfo`, ASRNode, `transcriptedit` |
| 16 | `AddAudio`, `AudioFadeIn/Out`, `AudioSeparateSound` |
| 17 | `TextToSpeechTaskManager`, `engine/{elevenlabs,microsoft,...}` |
| 18 | `aimusic/*`, `AiAudio*Node`, `AddAiMusicMvTemplate*` |
| 19 | `ExportConfig`, `ExportStartReqStruct`, VESDK encoder |
| 20 | `DraftClient`, `CloudDraftClient`, `cloud/*` |
| 21 | `TemplateClient`, `cutsameedit/*`, `split_screen_2.zip` |
| 22 | `AiClipperTask`, ASR/AED/highlight node |
| 23 | `ailabagent/unifyagent/*`, `videoagent`, `functionassistant` |
| 24 | `aigenerator`, `inpainting`, `outpainting`, `AIGCEffect*Node` |
| 25 | `digitalhuman/*`, `lipsync/*`, `AddDigitalHuman*` |
| 26 | `com.vega.recorder`, `com.vega.image`, `com.xt.retouch` |
| 27 | `Share*`, `publish/template`, `resultpage/share` |
| 28 | `clipmonetize`, billing, `assets/offline/image_lynx_subscription` |
| 29 | `effectplatform`, `mon-v2-boot`, AppLog/APM6/Slardar |
| 30 | `splitscreen`, `Gameplay*`, `LivePhoto*` |
| 31 | `adeditor/scriptvideo`, `smartad`, `com.vega.business.ad` |

## 12. Batasan & cara reproduksi

Batasan:

- Tidak ada observasi runtime; perilaku aktual bisa berbeda.
- Nama class diobfuscate; hanya modul JNI/SWIG yang terbaca jelas.
- Angka class termasuk inner/anonymous class.
- Parameter default (resolusi, bitrate, durasi, bahasa) tidak terbaca.
- Tidak ada kredensial/token/secret yang disertakan.

Reproduksi ringan (tanpa JADX):

```bash
cd work/extracted
aapt dump badging base.apk
unzip -l base.apk | grep '^.*assets/'
unzip -o -j split_config.arm64_v8a.apk 'lib/arm64-v8a/*.so' -d so_all
nm -D --defined-only so_all/libmiddle-bridge.so | grep Java_com_vega_middlebridge
for f in base_x/classes*.dex; do strings "$f"; done | grep -F '142.0.0'
unzip -p base.apk assets/splitscreen/split_screen_2.zip > /tmp/ss.zip
unzip -o /tmp/ss.zip -d /tmp/ss
```

Untuk analisis lebih dalam tanpa RAM besar: decompile **satu class** dengan
`jadx --single-class` dan `-Xmx1g`, bukan seluruh APK. Jangan jalankan
`dexdump` penuh pada 35 dex sekaligus.
