# ArCanvas

**A simple, distraction-free Android drawing canvas made with children in mind. No accounts, no permissions, no ads.**

ArCanvas is a lightweight, free Android app made for quick sketching and doodling. Pick a brush, grab a stamp, turn on Focus mode so kids (or you) don't accidentally exit while drawing, and save your work straight to your gallery.

> *Try it in your browser first:* Check out the web version at [ardalorn.github.io/ArTools/ArCanvas](https://ardalorn.github.io/ArTools/ArCanvas/).  
> *Note: This is a hobbyist project. Expect staggered updates and occasional patches when functionality needs fixing.*

---

## Features

* **Simple, Kid-Friendly Tools:** Choose from low-stimulation colour swatches, adjust brush sizes, fix mistakes with the eraser, or drop in four different stamp shapes for quick marks.
* **Focus Mode:** Prevents accidental exits by blocking the Back button and gestures while drawing. Pressing Back wakes a subtle on-screen pill—tap it when you're ready to exit.
* **Distraction-Free Canvas:** Runs fullscreen and edge-to-edge (swipe from the screen edge to peek at system bars). Artwork and Focus mode stay intact even if the screen rotates.
* **Adapts to Your Phone:** Uses Material You dynamic colours on Android 12+ for the interface, and a high-contrast accessible palette on Android 10 and 11. The paper and ink colours always stay consistent.
* **Private & Accessible:** Large 44 dp touch targets, no analytics, no internet connection required, and zero device permissions requested.

---

## How to Use

1. **Download & Install** the APK from the [Releases](https://github.com/Ardalorn/ArCanvas-Android/releases) page *(requires Android 10 or newer)*.
2. **Open & Draw:** The canvas fills your screen immediately—just pick a tool and start sketching.
3. **Save:** Tap **Save** to export your picture as a PNG directly to `Pictures/ArCanvas` on your device.

---

## Good to Know

* **Light theme only:** The app ignores your phone's dark mode setting so the canvas always looks like paper.
* **Your data stays yours:** Everything stays locally on your device. Delete saved PNGs anytime through your phone's normal gallery or files app.
* **Security:** Found a vulnerability? Please report it privately via the **Security > Report a vulnerability** tab rather than opening a public issue.

---

<details>
<summary><strong>For Developers (Building, Architecture & CI)</strong></summary>

<br>

ArCanvas is a native Kotlin and Jetpack Compose rebuild of the [single-file ArCanvas web app](https://ardalorn.github.io/ArTools/ArCanvas/) (no WebView).

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
