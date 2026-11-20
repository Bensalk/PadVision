# CLAUDE.md — PadVision

Android TV-style launcher and live TV player for tablets by Bensal. Kotlin/Jetpack Compose, single-file source. Public repo. Write docs and changelog for users, not about development history.

## Repo layout

```
/
├── app/src/main/java/com/bensal/tvlauncher/
│   ├── MainActivity.kt    ← entire app: UI, player, EPG, settings, recording, app launcher
│   ├── PlayService.kt     ← background playback / notification
│   └── AlarmReceiver.kt   ← channel reminders / scheduled play
├── app/src/main/res/      ← resources (icons, etc.)
├── app/build.gradle.kts   ← version + dependencies
├── .github/workflows/build.yml  ← CI build + release
├── CHANGELOG.md, LICENSE
└── README.md
```

## Build and release

- Every push to `main` builds an APK on GitHub Actions and publishes a GitHub Release.
- Signing: secrets `KEYSTORE_BASE64` (base64 of the keystore) and `KEYSTORE_PASSWORD`. Key alias `padvision`. **Never create or replace the signing key**: updates would no longer install over the existing app.
- **On every user-visible change bump `versionCode` and `versionName`** in `app/build.gradle.kts` and add an entry at the top of `CHANGELOG.md`.
- Other branches only upload a workflow artifact, not a release. Test risky changes on a branch first.
- Commit identity: `343577983+Bensalk@users.noreply.github.com`. Never a personal email.
- `main` is protected (no force push, no deletion). Private vulnerability reporting is on.

## App facts

- Package / applicationId: `com.bensal.tvlauncher`
- Current version: 2.6
- Features: live TV (M3U playlists), app launcher (with game detection), EPG/XMLTV program guide, recording (direct streams + HLS), multiple profiles with PIN lock, channel reminders, PiP, custom themes (10 built-in + custom accent), weather widget, backup/restore via clipboard, channel hiding, renaming, custom lists, quality filter

## Source files

| File | Role |
|---|---|
| `MainActivity.kt` | Entire app — M3U parsing, channel list, ExoPlayer, EPG, settings, recording, app launcher, profiles, all Compose UI |
| `PlayService.kt` | Background playback and notification. |
| `AlarmReceiver.kt` | Fires when a scheduled channel reminder is due. |

## Working style

- Bensal installs the APK from the GitHub Release on his device. He doesn't use Android Studio.
- Suggest changes and wait for his approval before implementing.
- Keep explanations short and non-technical.
- Start every reply with a `Model:` line (Sonnet for routine/support, Opus for hard coding or design decisions) and a one-line reason; Bensal can't judge difficulty himself.
