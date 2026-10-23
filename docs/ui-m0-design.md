# Desain UI M0 — Fondasi Compose

Spesifikasi desain untuk milestone **M0** (shell aplikasi + theme + Home + Projects).
Basis visual: mockup Stage 2 yang sudah di-ACC (`mockup/assets/style.css`,
`mockup/assets/core.js`, `mockup/assets/screens-core.js`).

Zcut dark-first, gaya "studio". Semua token dideklarasikan ulang sebagai nilai
Compose di `ui/theme/` — tidak ada aset, font, atau ikon milik CapCut.

## 1. Token warna

Dipetakan dari `mockup/assets/style.css` `:root`.

| Mockup | Hex | Token Compose |
|---|---|---|
| `--bg` | `#08090D` | `ZcutBackground` |
| `--panel` | `#11141C` | `ZcutSurface` |
| `--panel-2` | `#171B25` | `ZcutSurface2` |
| `--panel-3` | `#1E2330` | `ZcutSurface3` |
| `--text` | `#F5F7F8` | `ZcutText` |
| `--muted` | `#8D95A8` | `ZcutMuted` |
| `--muted-2` | `#5F6678` | `ZcutMuted2` |
| `--lime` | `#C9FF4A` | `ZcutLime` (aksen utama) |
| `--coral` | `#FF6B4A` | `ZcutCoral` |
| `--cyan` | `#59D9FF` | `ZcutCyan` |
| `--violet` | `#B69CFF` | `ZcutViolet` |
| `--amber` | `#FFC857` | `ZcutAmber` |
| `--green` | `#55E6A5` | `ZcutGreen` |
| `--red` | `#FF5470` | `ZcutRed` |
| `--line-strong` | `#2A2F3C` | `ZcutOutline` |

Skema Material 3:

- **Dark (default).** `background=ZcutBackground`, `surface=ZcutSurface`,
  `surfaceVariant=ZcutSurface2`, `surfaceContainer=ZcutSurface3`,
  `onBackground/onSurface=ZcutText`, `onSurfaceVariant=ZcutMuted`,
  `primary=ZcutLime`, `onPrimary=#0B0D07`, `secondary=ZcutCyan`,
  `tertiary=ZcutCoral`, `error=ZcutRed`, `outline=ZcutOutline`.
- **Light.** `background=#F6F7F9`, `surface=#FFFFFF`, `surfaceVariant=#EDF0F4`,
  `onSurface=#101218`, `onSurfaceVariant=#55606F`, `primary=#4F7A00`
  (lime digelapkan agar kontras teks lolos), `secondary=#0E7490`,
  `tertiary=#C2410C`, `outline=#C9D0DA`.

Aksen lime tetap `#C9FF4A` sebagai warna brand (badge, highlight), bukan
`primary` di light mode.

## 2. Tipografi

Font sistem (`FontFamily.SansSerif`) — tanpa font eksternal. Skala:

| Style | Size | Weight | Line height | Letter spacing |
|---|---|---|---|---|
| `displaySmall` | 34sp | Black | 38sp | -1.0sp |
| `headlineMedium` | 26sp | Black | 30sp | -0.6sp |
| `headlineSmall` | 22sp | Bold | 27sp | -0.5sp |
| `titleLarge` | 18sp | Bold | 24sp | -0.3sp |
| `titleMedium` | 15sp | SemiBold | 20sp | -0.2sp |
| `titleSmall` | 13sp | SemiBold | 18sp | 0sp |
| `bodyLarge` | 15sp | Normal | 22sp | 0sp |
| `bodyMedium` | 13sp | Normal | 19sp | 0sp |
| `bodySmall` | 12sp | Normal | 17sp | 0sp |
| `labelLarge` | 13sp | Bold | 18sp | 0.2sp |
| `labelMedium` | 11sp | SemiBold | 15sp | 0.4sp |
| `labelSmall` | 10sp | SemiBold | 13sp | 0.6sp |

Judul brand (`zcut.`) memakai `displaySmall` dengan `zcut` putih dan titik lime.

## 3. Bentuk & jarak

- **Radius:** `extraSmall=6dp`, `small=10dp`, `medium=14dp`, `large=18dp`,
  `extraLarge=28dp`. Pill/chip = `CircleShape` (mockup `--radius:18px` → `large`).
- **Spacing:** `xs=4dp`, `sm=8dp`, `md=12dp`, `lg=16dp`, `xl=20dp`, `xxl=24dp`,
  `xxxl=32dp`.
- **Padding layar:** horizontal `18dp`, gap antar section `20dp`.
- **Touch target:** minimal 48dp untuk semua elemen interaktif.
- **Kontras:** teks muted `#8D95A8` di atas `#08090D` ≈ 7.6:1 (lolos AA).

## 4. Komponen bersama

Semua di `ui/components/`.

| Komponen | Signature inti | Catatan |
|---|---|---|
| `ZcutScaffold` | `(topBar, bottomBar, floatingActionButton, content)` | `Scaffold` + background `ZcutBackground`; inset ditangani `Scaffold` |
| `ZcutTopBar` | `(title, subtitle?, navigationIcon?, actions)` | tinggi 54dp, blur/border tipis, judul ellipsis 1 baris |
| `ZcutBottomNav` | `(currentRoute, onNavigate)` | 5 item: Home, Template, Drafts, AI Studio, Profil; item aktif lime + garis atas |
| `ZcutCard` | `(onClick?, content)` | `surface` + border `ZcutOutline`, radius `large` |
| `ZcutChip` | `(label, selected, onClick, leading?)` | pill; aktif = lime + teks gelap |
| `ZcutSlider` | `(label, value, onValueChange, valueRange, valueLabel?)` | baris label + nilai di kanan |
| `SectionHeader` | `(title, actionLabel?, onAction?)` | judul `titleSmall`, aksi teks lime |
| `EmptyState` | `(title, message, actionLabel?, onAction?, icon?)` | ikon lingkaran panel-2, teks muted, tombol utama |
| `BackendBadge` | `(featureId, modifier)` | baca `FeatureRegistry`; tampil hanya jika `requirement != NONE`; teks "BUTUH BACKEND" / "BACKEND OPSIONAL"; **bukan paywall** |
| `ZcutPrimaryButton` / `ZcutSecondaryButton` / `ZcutGhostButton` | `(text, onClick, enabled, modifier)` | tinggi 44dp, radius `small`, konten minimal 48dp |

## 5. Layar M0

### Splash (`feature/splash/SplashScreen.kt`)
Mark `zcut.` di tengah (brand), latar `ZcutBackground`. Sistem splash lewat
`core-splashscreen` (`Theme.Zcut.Splash`). Setelah ±700ms pindah ke Onboarding
(sekali per proses; M1 mempersistensinya) atau Home.

### Onboarding (`feature/onboarding/OnboardingScreen.kt`)
3 slide: (1) semua fitur terbuka tanpa langganan/watermark, (2) proses
on-device tanpa server, (3) hasil siap share. Indikator titik; tombol
"Lewati" (ghost) dan "Mulai" (primary). Tanpa aset gambar — art dibuat dari
bentuk/gradasi Compose.

### Home (`feature/home/HomeScreen.kt`)
- Hero: gradient lime→cyan→coral tipis, judul "Studio video lokal", subteks,
  tombol primary "Project baru", chip "TANPA WATERMARK".
- `SectionHeader("Draft terakhir")` + baris horizontal kartu project (thumb
  9:16 placeholder bergradasi, badge durasi, nama ellipsis, waktu edit).
- `SectionHeader("Tool AI")` + grid 3 kolom kartu tool (AI Studio, AI Clipper,
  Agent) dengan `BackendBadge`.
- Empty state bila belum ada draft.

### Projects / Drafts (`feature/projects/ProjectsScreen.kt`)
- `OutlinedTextField` pencarian nama (filter langsung di ViewModel; tanpa
  debounce — daftar lokal dan kecil).
- Chip filter aspek: Semua / 9:16 / 1:1 / 16:9 / 4:5.
- Daftar kartu: thumb 48dp, nama, "diedit X · durasi · aspek", trailing `MoreVert`.
- Menekan kartu → Editor; `MoreVert` → menu (Duplikat, Hapus) via `DropdownMenu`.
- FAB "Project baru".
- Empty state + loading (`LinearProgressIndicator`).

## 6. Aksesibilitas & perilaku

- Semua ikon punya `contentDescription`; tombol ikon minimal 48dp.
- Nama project panjang: `maxLines=1`, `overflow=Ellipsis`.
- Layar kecil: grid tool memakai `weight(1f)` dan tidak overflow; hero
  memakai `maxWidth` relatif.
- Edge-to-edge: `enableEdgeToEdge()` di `MainActivity`; konten memakai padding
  inset dari `Scaffold`.
- Loading: skeleton sederhana (`ZcutCard` abu) saat `isLoading`.
- Error: banner teks di atas daftar. Tombol "Coba lagi" menyusul di M1
  (butuh jalur retry di ViewModel).

## 7. Preview

`@Preview` untuk komponen bersama (`ZcutCard`, `ZcutChip` aktif/nonaktif,
`EmptyState`, `BackendBadge`, `ProjectRailCard`/`ProjectRowCard`) dan layar
(`HomeScreen` isi & kosong, `ProjectsScreen` daftar/kosong/pencarian kosong)
**belum ditambahkan di M0** — masuk backlog M1 bersama layar Profil/Pengaturan.
M0 fokus ke jalur render nyata dan unit test logika murni.
