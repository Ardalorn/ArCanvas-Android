# ArCanvas

Hobbyist project, expect staggered updates and patches only at degradation of functionality.

**A simple drawing canvas that just works. No account, no permissions, no ads.**

ArCanvas is a free Android app for quick drawing. Pick a brush, the eraser or a stamp, draw on the canvas, and save your picture as a PNG. Switch on Focus mode to stop the app closing by accident while you draw.

---

## Features

### Draw

- **Brush.** Choose from a set of low stimulation colour swatches and brush sizes.
- **Eraser.** Rub out mistakes.
- **Stamps.** Four stamp shapes for quick marks.
- **Save as PNG.** Pictures are written to Pictures/ArCanvas. No permission is needed.

### Focus mode

- **Cannot be closed by accident.** In Focus mode, Back (button or gesture) is blocked.
- **A clear way out.** Each blocked Back press wakes the Focus-mode island, which is the way out.
- **Normal Back otherwise.** Outside Focus mode, Back closes the app as usual.

### A canvas that feels good

- Fullscreen and edge to edge. System bars are hidden. Swipe from an edge to peek at them, and they hide again.
- Your drawing and Focus mode survive rotation.
- Material You colours on Android 12 and newer, applied to the header, toolbar and buttons.

### Accessible by design

- Large 44 dp touch targets.
- Visible focus ring on controls.
- On Android 10 and 11, the fixed palette has text pairs above 7:1 and borders above 4:1.

### Private by default

- No account, no ads, no analytics.
- No permissions are requested.
- Pictures are only saved when you tap Save, to Pictures/ArCanvas on your device.

---

## Getting started

1. **Install** the app (see [Install](#install)).
2. **Open it.** The canvas fills the screen.
3. **Draw.** Pick a brush colour and size, the eraser or a stamp.
4. **Tap Save** to write your picture as a PNG to Pictures/ArCanvas.

> Tip: in Focus mode, press Back to wake the island and find the way out.

### Permissions

| Permission | Why                                                    |
| ---------- | ------------------------------------------------------ |
| None       | Saving a PNG to Pictures/ArCanvas needs no permission. |

---

## Install

**Requirements:** Android 10 (API 29) or newer.

### Build it with GitHub (no command line)

1. Fork this repository, or push your copy of the code to the `main` branch of your own repository. GitHub may ask you to enable Actions first. The `build.yml` workflow then builds the app by itself.
2. When the run finishes, open it in the **Actions** tab and download the debug APK artifact.
3. Unzip it, copy the `.apk` to your phone and open it. Android asks once to allow installs from that source.

This is a debug build made from your own copy of the code, so install only APKs you built yourself or got from someone you trust. For a signed release build, see [Cutting a release](#cutting-a-release).

### Build it yourself

1. Install [Android Studio](https://developer.android.com/studio) and open the project folder.
2. Let Gradle sync, then press **Run**. Android Studio fetches Gradle 8.7 from `gradle/wrapper/gradle-wrapper.properties`.

Or from the command line (JDK 17 and Gradle 8.7 required):

```
gradle assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/`. There is no `gradlew` script or wrapper jar in the repository. Run `gradle wrapper` once if you want them.

---

## Good to know

- **Light theme only.** The device's dark setting is ignored.
- **Older Android versions.** Android 10 and 11 cannot do Material You colours, so they use a fixed high-contrast palette instead.
- **The canvas never changes colour.** The paper, the "Ink" brush colour and the saved PNG's background stay the same whatever the interface colours are.

## Privacy and security

ArCanvas has no server of its own and asks for no permissions.

**What stays on your device**

- **Saved pictures.** PNG files in Pictures/ArCanvas. Delete them like any other picture.
- **Your current drawing.** Kept in the app while it runs.

**How the app protects you**

- No permissions are requested.
- No account, no ads, no analytics, no third-party SDKs.

**Reporting a vulnerability.** Please don't open a public issue. Use the repository's **Security > Report a vulnerability** option, or contact the maintainer privately.

---

## For developers

ArCanvas is written in Kotlin with Jetpack Compose. It is a native rebuild of the single-file ArCanvas web app, with no WebView.

```
app/src/main/java/<package>/
  MainActivity.kt      Fullscreen window setup
  CanvasViewModel.kt   Bitmap, brush, eraser, stamps, tool state, Focus mode, save
  ArCanvasApp.kt       Screen: drawing surface, header, toolbar, Focus-mode island, Back handling
  Components.kt        Pressable button, path icons
  Stamps.kt            The four stamp shapes
  Theme.kt             Material 3 theme: Material You colour, fallback palette
  Tokens.kt            Fallback palette, swatches, brush sizes, timings
```

**Pinned versions:** AGP 8.5.2, Kotlin 1.9.24, Compose compiler 1.5.14, Compose BOM 2024.06.00, compileSdk and targetSdk 34. These are versions that exist and work together, not necessarily the latest. If Android Studio offers an upgrade, take it, but move Kotlin and the Compose compiler together.

### Theme (Material 3 and Material You)

- Material 3 (`androidx.compose.material3`, version from the Compose BOM) themes the whole app through `ArCanvasTheme` in `Theme.kt`.
- Material You dynamic colour is on by default on Android 12+ (API 31). There is no settings screen, so there is no toggle. `DYNAMIC_COLOR` in `Tokens.kt` is a build-time switch only.
- Android 10 and 11 (API 29 and 30) cannot do dynamic colour and use the original boilerplate-AA palette (`Tokens`), mapped onto Material 3 roles. Its text pairs are above 7:1 and its borders above 4:1.
- Light only: the device's dark setting is ignored.
- The canvas paper, the "Ink" brush colour and the saved PNG's background are fixed (`Tokens.Paper` and `Tokens.Ink`) and never change with Material You. Only the interface on top (header, toolbar, buttons, Focus pill) follows the palette.
- Controls are the custom `Pressable` (44 dp targets, press scale, visible focus ring), coloured from Material 3 roles.
- Focus mode state and the drawing are held in a ViewModel, which is why they survive rotation.

### CI and releases

| Workflow      | Trigger                                                        | Does                                                                           |
| ------------- | -------------------------------------------------------------- | ------------------------------------------------------------------------------ |
| `ktlint.yml`  | Push or pull request touching `*.kt`, `*.kts`, `.editorconfig` | ktlint 1.8.0 over all Kotlin files                                             |
| `build.yml`   | Push to `main`, pull requests                                  | `assembleDebug`, uploads the debug APK as an artifact                          |
| `release.yml` | Tag `v*`                                                       | ktlint, `assembleRelease`, zipalign, sign, GitHub release with APK and SHA-256 |

Workflows pin Gradle with `GRADLE_VERSION` (no wrapper in the repo). Update it together with `gradle-wrapper.properties`.

**Run checks locally**

```
ktlint
```

Install [ktlint](https://github.com/pinterest/ktlint) 1.8.0 and run it from the project root.

### Cutting a release

1. Set `versionName` (and bump `versionCode`) in `app/build.gradle.kts`.
2. Tag and push: `git tag v1.0.0 && git push origin v1.0.0`. The tag must equal `v` + `versionName`. A tag containing `-` (for example `v1.1.0-rc1`) is published as a pre-release.

### Signing secrets (Settings, Secrets and variables, Actions)

`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Create a keystore once and keep it:

```
keytool -genkeypair -v -keystore release.jks -alias arcanvas -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 release.jks
```

Losing the keystore means existing installs cannot be updated.
