# Zcut Stage 2 Mockup

Interactive HTML mockup for all 31 feature areas (42 screens).

## Run

```bash
python3 -m http.server 8080
```

Open:

- http://127.0.0.1:8080/ — interactive prototype
- http://127.0.0.1:8080/?view=map — all screens
- http://127.0.0.1:8080/?screen=editor — editor
- http://127.0.0.1:8080/?screen=export — export flow

## Notes

- Pure HTML/CSS/JS, no build step and no external assets.
- All thumbnail art is CSS-generated.
- Backend-dependent tools are marked with a visible badge instead of a paywall.
- Approval must come via Telegram before Android UI work starts.

## Screen groups

| Group | Screens |
|---|---|
| Start | Splash, onboarding, home, projects |
| Library | Templates, template detail, discover, drafts |
| AI | AI Studio, AI Clipper, Zcut Agent, Generate, Digital Human, Ad Maker |
| Create | Editor, media picker, trim, speed, keyframes, transitions, filters, adjust, effects, mask, cutout, beauty, stickers, text, captions, audio, TTS, AI music, split screen, gameplay, live photo |
| Capture | Camera, photo editor |
| Share | Export, export done, share |
| System | Profile, settings |
