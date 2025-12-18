# ImgCraft

**ImgCraft** is an Android image editing library built with **Kotlin + NDK**.
It provides **real-time photo adjustments**, smooth preview rendering, and high-quality final exports.
The library also supports a **16 KB memory page size**.

---

## Dependency Setup

### Add JitPack Repository

(Project `settings.gradle`)

```gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

---

### Add Dependency

```gradle
dependencies {
    implementation("com.github.muhammadnoman11:ImgCraft:1.0-beta")
}
```

---

## Usage

### 1️⃣ Initialize ImageCraft

Create an `ImageCraft` instance by providing a **Context**, **Image URI**, and a **preview callback**.

#### Jetpack Compose Example

```kotlin
private val previewBitmap = mutableStateOf<Bitmap?>(null)

imgCraft = ImageCraft(context, uri) { bitmap ->
    previewBitmap.value = bitmap
}
```

Display the preview:

```kotlin
previewBitmap.value?.let {
    Image(
        bitmap = it.asImageBitmap(),
        contentDescription = null,
        modifier = Modifier.aspectRatio(1f),
        contentScale = ContentScale.Fit
    )
}
```

---

## Tools Default Slider/Seekbar Values

| Tool Name  | Default UI Value |
| ---------- | ---------------- |
| Brightness | 50               |
| Contrast   | 50               |
| Exposure   | 50               |
| Hue        | 50               |
| Saturation | 50               |
| Highlight  | 50               |
| Shadows    | 50               |
| Grain      | 0                |
| Sharpness  | 0                |
| Vignette   | 0                |

---

## Tools Default Values Explanation

Each adjustment tool uses a **slider (SeekBar)** to control the effect. The **Default UI Value** represents the slider’s starting position when the editor loads.

* **`50` (CENTER)** → Used for tools like **Brightness, Contrast, Exposure, Hue, Saturation, Highlight, Shadows**.

    * This is the middle of the slider, meaning **no effect is applied initially**.
    * Moving the slider left decreases the effect, moving it right increases the effect.

* **`0` (ZERO)** → Used for tools like **Grain, Sharpness, Vignette**.

    * These tools only **add an effect**, so they start at 0 by default.
    * Moving the slider right increases the effect.

### Example: Tool Model

Tools are represented using the `AdjustmentTool` model:

```kotlin
data class AdjustmentTool(
    val name: String,  // Tool name
    var progressValue: Int,  // Current slider value (0–100)
    val defaultValue: Int,  // Starting slider value
    var isSelected: Boolean = false  // Currently active tool in the UI
)
```

You can create a list of tools like this:

```kotlin

val tools = mutableListOf(
    AdjustmentTool("Brightness", progressValue = ToolDefaults.CENTER, defaultValue = ToolDefaults.CENTER),
    AdjustmentTool("Contrast", progressValue = ToolDefaults.CENTER, defaultValue = ToolDefaults.CENTER),
    AdjustmentTool("Vignette", progressValue = ToolDefaults.ZERO, defaultValue = ToolDefaults.ZERO)
)
```

---

## Applying Adjustments

### Slider/Seekbar → ImgCraft Mapping

| Tool       | Code                                            |
| ---------- | ----------------------------------------------- |
| Brightness | `imgCraft.setBrightness((progress - 50) / 50f)` |
| Contrast   | `imgCraft.setContrast((progress - 50) / 50f)`   |
| Exposure   | `imgCraft.setExposure((progress - 50) / 50f)`   |
| Hue        | `imgCraft.setHue((progress - 50) / 50f)`        |
| Saturation | `imgCraft.setSaturation((progress - 50) / 50f)` |
| Highlight  | `imgCraft.setHighlight((progress - 50) / 50f)`  |
| Shadows    | `imgCraft.setShadows((progress - 50) / 50f)`    |
| Grain      | `imgCraft.setGrain(progress / 100f)`            |
| Sharpness  | `imgCraft.setSharpness(progress / 100f)`        |
| Vignette   | `imgCraft.setVignette(progress / 100f)`         |

---

## Before / After Comparison

### Press & Hold Comparison

Show the original image while pressing an icon, and show the edited image when released.

#### Compose Example

```kotlin
Icon(
    painter = painterResource(R.drawable.ic_compare),
    contentDescription = "Compare",
    modifier = Modifier
        .size(28.dp)
        .pointerInteropFilter { event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> imgCraft.showBefore()
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> imgCraft.showAfter()
            }
            true
        }
)
```

---

## Reset Adjustments

Reset all applied effects:

```kotlin
imgCraft.reset()
```

⚠️ Make sure to also reset your UI slider values when calling `reset()`.

---

## Save Final Image (High Quality)

Render the **full-resolution output**:

```kotlin
val bitmap = imgCraft.renderFinal()
```

---

## Cleanup (Important)

Always release ImgCraft when the screen is destroyed:

```kotlin
override fun onDestroy() {
    super.onDestroy()
    imgCraft.release()
}
```

---

## Demo

<img src="demo/demo.gif" width="300" />


