# ArCanvas

**A simple, distraction-free Android drawing canvas made with children in mind. No accounts, no permissions, no ads.**[span_1](start_span)[span_1](end_span)[span_2](start_span)[span_2](end_span)

ArCanvas is a lightweight, free Android app made for quick sketching and doodling[span_3](start_span)[span_3](end_span). Pick a brush, grab a stamp, turn on Focus mode so kids (or you) don't accidentally exit while drawing, and save your work straight to your gallery[span_4](start_span)[span_4](end_span)[span_5](start_span)[span_5](end_span).

> *Try it in your browser first:* Check out the web version at [ardalorn.github.io/ArTools/ArCanvas](https://ardalorn.github.io/ArTools/ArCanvas/)[span_6](start_span)[span_6](end_span)[span_7](start_span)[span_7](end_span).  
> *Note: This is a hobbyist project. Expect staggered updates and occasional patches when functionality needs fixing[span_8](start_span)[span_8](end_span).*

---

## Features

* **Simple, Kid-Friendly Tools:** Choose from low-stimulation colour swatches, adjust brush sizes, fix mistakes with the eraser, or drop in four different stamp shapes for quick marks[span_9](start_span)[span_9](end_span)[span_10](start_span)[span_10](end_span).
* **Focus Mode:** Prevents accidental exits by blocking the Back button and gestures while drawing[span_11](start_span)[span_11](end_span). Pressing Back wakes a subtle on-screen pill—tap it when you're ready to exit[span_12](start_span)[span_12](end_span).
* **Distraction-Free Canvas:** Runs fullscreen and edge-to-edge (swipe from the screen edge to peek at system bars)[span_13](start_span)[span_13](end_span). Artwork and Focus mode stay intact even if the screen rotates[span_14](start_span)[span_14](end_span).
* **Adapts to Your Phone:** Uses Material You dynamic colours on Android 12+ for the interface, and a high-contrast accessible palette on Android 10 and 11[span_15](start_span)[span_15](end_span). The paper and ink colours always stay consistent[span_16](start_span)[span_16](end_span).
* **Private & Accessible:** Large 44 dp touch targets, no analytics, no internet connection required, and zero device permissions requested[span_17](start_span)[span_17](end_span).

---

## How to Use

1. **Download & Install** the APK from the [Releases](https://github.com/Ardalorn/ArCanvas-Android/releases) page *(requires Android 10 or newer)*[span_18](start_span)[span_18](end_span).
2. **Open & Draw:** The canvas fills your screen immediately—just pick a tool and start sketching[span_19](start_span)[span_19](end_span).
3. **Save:** Tap **Save** to export your picture as a PNG directly to `Pictures/ArCanvas` on your device[span_20](start_span)[span_20](end_span).

---

## Good to Know

* **Light theme only:** The app ignores your phone's dark mode setting so the canvas always looks like paper[span_21](start_span)[span_21](end_span).
* **Your data stays yours:** Everything stays locally on your device[span_22](start_span)[span_22](end_span). Delete saved PNGs anytime through your phone's normal gallery or files app[span_23](start_span)[span_23](end_span).
* **Security:** Found a vulnerability? Please report it privately via the **Security > Report a vulnerability** tab rather than opening a public issue[span_24](start_span)[span_24](end_span).

---

<details>
<summary><strong>For Developers (Building, Architecture & CI)</strong></summary>

<br>

ArCanvas is a native Kotlin and Jetpack Compose rebuild of the [single-file ArCanvas web app](https://ardalorn.github.io/ArTools/ArCanvas/) (no WebView)[span_25](start_span)[span_25](end_span)[span_26](start_span)[span_26](end_span).

### Architecture
```text
app/src/main/java/<package>/
  MainActivity.kt      # Fullscreen window setup
  CanvasViewModel.kt   # Bitmap, tools, Focus mode, and save state
  ArCanvasApp.kt       # Main UI, drawing surface, Focus island, Back handling
  Components.kt        # Custom 44 dp Pressable buttons & path icons
  Stamps.kt            # Vector stamp shapes
  Theme.kt             # Material 3 & dynamic colour theming
  Tokens.kt            # Fallback palette, swatches, brush sizes, timings
