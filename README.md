# Zcut

A clean-room, local-first video editor for Android, reconstructed feature-by-feature
from the reverse-engineering notes for CapCut 19.8.0.

- **Package:** `com.zaaam.zcut`
- **Stack:** Kotlin, Jetpack Compose, Material 3, Media3, Room, DataStore
- **Target:** Android 8.0+ (minSdk 26), compile/target SDK 35
- **Status:** Stage 1 (specs) complete · Stage 2 (mockup) awaiting ACC
- **Repo:** https://github.com/az925-crypto/Zcut

## Product rules

1. **Every feature is free.** No paywall, subscription, credits, reward ads,
   trial, quota, or watermark.
2. **No CapCut servers.** The app never calls the original service, CDN, or APIs.
3. **Local-first.** Editing, preview, and export run on-device. Features that
   need a generative model or cloud service are marked `BUTUH BACKEND` and keep
   a local fallback where possible.
4. **Clean-room.** No original code, assets, fonts, templates, effects, or
   branding are copied.
5. **No protection bypass.** No license, signature, or anti-tamper bypass is
   implemented.

## Repository map

```
docs/                 feature specs, architecture, evidence index
docs/features/        31 feature specs
PROGRESS.md           1:1 parity checklist, backend flags, build/release log
mockup/               Stage 2 interactive HTML mockup
app/                  Android application (created during Stage 3)
.github/workflows/    CI + signed release build (created during Stage 3)
```

## Stage 2 mockup

The mockup covers 42 screens across all 31 feature areas.

```bash
cd mockup
python3 -m http.server 8080
# open http://127.0.0.1:8080/
```

Useful views:

- `/?screen=home` — interactive prototype
- `/?view=map` — all 42 screens at once
- `/?screen=editor` — editor / timeline
- `/?screen=export` — export flow

Approval is collected via Telegram. Android UI work starts only after explicit
`ACC` is received.

## Documentation

- [Product specs](docs/README.md)
- [Feature index](docs/feature-index.md)
- [Architecture](docs/architecture.md)
- [Parity checklist](PROGRESS.md)

## Build & release

All heavy builds and tests run in GitHub Actions, never locally. The release
workflow builds a signed APK using secrets stored in GitHub and attaches it to a
GitHub Release. Keystore, passwords, and tokens are never committed.

## Legal

CapCut, VESDK, EffectSDK, and their assets belong to ByteDance. This project is
an independent interoperability study and an original implementation. Do not
copy proprietary assets, fonts, effects, templates, or code without a license.
