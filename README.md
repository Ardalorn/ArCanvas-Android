# ArCanvas (Kotlin, Jetpack Compose)

Native Android rebuild of the single-file ArCanvas web app. No WebView.

## Build and run

1. Open this folder in Android Studio and let Gradle sync. Android Studio fetches Gradle 8.7 from
   `gradle/wrapper/gradle-wrapper.properties`; there is no `gradlew` script or wrapper jar in this
   zip. Run `gradle wrapper` once if you want them for command-line builds.
2. Run on a device or emulator (Android 10 / API 29 or newer).

Pinned versions: AGP 8.5.2, Kotlin 1.9.24, Compose compiler 1.5.14, Compose BOM 2024.06.00,
compileSdk/targetSdk 34. These are versions that exist and work together, not necessarily the
latest. If Android Studio offers an upgrade, take it, but move Kotlin and the Compose compiler
together.

## Theme (Material 3 and Material You)

- Material 3 (`androidx.compose.material3`, version from the Compose BOM) themes the whole app through
  `ArCanvasTheme` in `Theme.kt`.
- Material You dynamic colour is on by default on Android 12+ (API 31). There is no settings screen, so
  there is no toggle; `DYNAMIC_COLOR` in `Tokens.kt` is a build-time switch only.
- Android 10 and 11 (API 29 and 30) cannot do dynamic colour and use the original boilerplate-AA palette
  (`Tokens`), mapped onto Material 3 roles. Its text pairs are above 7:1 and its borders above 4:1.
- Still light only: the device's dark setting is ignored, as before.
- The canvas paper, the "Ink" brush colour and the saved PNG's background are fixed (`Tokens.Paper` and
  `Tokens.Ink`) and never change with Material You. Only the interface on top (header, toolbar, buttons,
  Focus pill) follows the palette.
- Controls are still the custom `Pressable` (44 dp targets, press scale, visible focus ring), now coloured
  from Material 3 roles rather than fixed colours.

## Behaviour

- Fullscreen, edge to edge, system bars hidden (swipe from an edge to peek at them; they hide again).
- Focus mode: Back (button or gesture) is swallowed, so the app cannot be closed by accident.
  Each swallowed Back press wakes the island, which is the way out.
  Outside Focus mode Back closes the app as normal.
- The drawing and Focus mode survive rotation (held in a ViewModel).
- Save writes a PNG to Pictures/ArCanvas. No permissions are needed.

## Files

| File | Purpose |
| --- | --- |
| `MainActivity.kt` | Fullscreen window setup |
| `CanvasViewModel.kt` | Bitmap, brush, eraser, stamps, tool state, Focus mode, save |
| `ArCanvasApp.kt` | Screen: drawing surface, header, toolbar, Focus-mode island, Back handling |
| `Components.kt` | Pressable button, path icons |
| `Stamps.kt` | The four stamp shapes |
| `Theme.kt` | Material 3 theme: Material You colour, fallback palette |
| `Tokens.kt` | Fallback palette, swatches, brush sizes, timings |

## CI and releases

| Workflow | Trigger | Does |
| --- | --- | --- |
| `ktlint.yml` | Push or pull request touching `*.kt`, `*.kts`, `.editorconfig` | ktlint 1.8.0 over all Kotlin files |
| `build.yml` | Push to `main`, pull requests | `assembleDebug`, uploads the debug APK as an artifact |
| `release.yml` | Tag `v*` | ktlint, `assembleRelease`, zipalign, sign, GitHub release with APK and SHA-256 |

Workflows pin Gradle with `GRADLE_VERSION` (no wrapper in the repo). Update it together with
`gradle-wrapper.properties`.

### Cutting a release

1. Set `versionName` (and bump `versionCode`) in `app/build.gradle.kts`.
2. Tag and push: `git tag v1.0.0 && git push origin v1.0.0`. The tag must equal `v` + `versionName`.
   A tag containing `-` (for example `v1.1.0-rc1`) is published as a pre-release.

### Signing secrets (Settings, Secrets and variables, Actions)

`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Create a keystore once and keep it:

```
keytool -genkeypair -v -keystore release.jks -alias arcanvas -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 release.jks
```

Losing the keystore means existing installs cannot be updated.
