package com.github.muhammadnoman11.imgcraft.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import com.github.muhammadnoman11.imgcraft.model.AdjustmentConfig
import com.github.muhammadnoman11.imgcraft.nativebridge.NativeImageProcessor
import com.github.muhammadnoman11.imgcraft.util.BitmapUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min

/**
 * @author: Muhammad Noman
 * @github: https://github.com/muhammadnoman11
 * @date: 12/15/2025
 */

/**
 * ImageCraft
 *
 * Core image processing engine responsible for:
 * - Loading image from URI
 * - Creating preview & full-resolution pipelines
 * - Applying real-time adjustments using native (NDK) processing
 * - Managing background rendering and UI-safe callbacks
 *
 */
class ImageCraft(
    context: Context,
    uri: Uri,
    private val onPreviewReady: (Bitmap) -> Unit
) {

    /** Native image processor (JNI bridge) */
    private val processor = NativeImageProcessor()

    /** Current adjustment configuration */
    private val config = AdjustmentConfig()

    /* ---------------------------------------------------------
     * Threading & Concurrency
     * --------------------------------------------------------- */

    /** Dedicated background thread for rendering */
    private val renderThread = HandlerThread("ImageCraft-Render").apply { start() }

    /** Handler bound to render thread */
    private val renderHandler = Handler(renderThread.looper)

    /** Main thread handler for UI callbacks */
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Coroutine scope for background decoding work */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /* ---------------------------------------------------------
     * Bitmaps
     * --------------------------------------------------------- */

    /** Full-resolution original image (used for export) */
    private lateinit var fullOriginal: Bitmap

    /** Downscaled preview original image */
    private lateinit var previewOriginal: Bitmap

    /** Bitmap displayed to UI */
    private lateinit var displayBitmap: Bitmap

    /** Work buffers for triple buffering */
    private lateinit var workBitmap1: Bitmap
    private lateinit var workBitmap2: Bitmap

    /** Canvas for drawing final output */
    private lateinit var displayCanvas: Canvas

    /** Front and back buffers for processing */
    private lateinit var frontWorkBitmap: Bitmap
    private lateinit var backWorkBitmap: Bitmap

    /* ---------------------------------------------------------
    * State Management
    * --------------------------------------------------------- */

    /** Prevents concurrent render execution */
    private val isProcessing = AtomicBoolean(false)

    /** Counts pending render requests (slider fast movement) */
    private val pendingRenderCount = AtomicInteger(0)

    /** Indicates whether engine is released */
    private var isReleased = false

    /* ---------------------------------------------------------
     * Initialization
     * --------------------------------------------------------- */

    init {
        // Decode image off main thread
        scope.launch {
            val fullBitmap = BitmapUtils.decodeUri(context, uri) ?: return@launch

            fullOriginal = fullBitmap

            // Create a smaller preview bitmap for real-time editing
            val previewBitmap = BitmapUtils.createPreviewBitmap(fullBitmap, maxSize = 1080)

            // Switch to main thread for bitmap creation & callback
            withContext(Dispatchers.Main) {
                previewOriginal = previewBitmap

                // Triple buffer setup
                displayBitmap = Bitmap.createBitmap(
                    previewOriginal.width,
                    previewOriginal.height,
                    Bitmap.Config.ARGB_8888
                )
                workBitmap1 = Bitmap.createBitmap(
                    previewOriginal.width,
                    previewOriginal.height,
                    Bitmap.Config.ARGB_8888
                )
                workBitmap2 = Bitmap.createBitmap(
                    previewOriginal.width,
                    previewOriginal.height,
                    Bitmap.Config.ARGB_8888
                )
                displayCanvas = Canvas(displayBitmap)
                frontWorkBitmap = workBitmap1
                backWorkBitmap = workBitmap2

                // Draw original image initially
                displayCanvas.drawBitmap(previewOriginal, 0f, 0f, null)

                // Notify UI with initial preview
                onPreviewReady(displayBitmap)
            }
        }
    }

    /* ---------------------------------------------------------
    * Adjustment (Public)
    * --------------------------------------------------------- */
    fun setBrightness(v: Float) = update { config.brightness = v.coerceIn(-1f, 1f) }
    fun setContrast(v: Float) = update { config.contrast = v.coerceIn(-1f, 1f) }
    fun setExposure(v: Float) = update { config.exposure = v.coerceIn(-2f, 2f) }
    fun setHue(v: Float) = update { config.hue = v.coerceIn(-1f, 1f) }
    fun setSaturation(v: Float) = update { config.saturation = v.coerceIn(-1f, 1f) }
    fun setHighlight(v: Float) = update { config.highlight = v.coerceIn(-1f, 1f) }
    fun setShadows(v: Float) = update { config.shadows = v.coerceIn(-1f, 1f) }
    fun setGrain(v: Float) = update { config.grain = v.coerceIn(0f, 1f) }
    fun setSharpness(v: Float) = update { config.sharpness = v.coerceIn(0f, 1f) }
    fun setVignette(v: Float) = update { config.vignette = v.coerceIn(0f, 1f) }

    /**
     * Applies adjustment update and schedules re-render.
     */
    private fun update(block: () -> Unit) {
        block()
        scheduleRender()
    }

    /* ---------------------------------------------------------
    * Preview Controls
    * --------------------------------------------------------- */

    /** Shows original image without adjustments */
    fun showBefore() {
        if (isReleased) return
        onPreviewReady(previewOriginal)
    }

    /** Shows processed image */
    fun showAfter() {
        if (isReleased) return
        onPreviewReady(displayBitmap)
    }

    /**
     * Reset all adjustments to default values.
     */
    fun reset() {
        config.apply {
            brightness = 0f
            contrast = 0f
            exposure = 0f
            hue = 0f
            saturation = 0f
            highlight = 0f
            shadows = 0f
            grain = 0f
            sharpness = 0f
            vignette = 0f
        }

        scheduleRender()
    }

     /* ---------------------------------------------------------
      * Rendering Pipeline
      * --------------------------------------------------------- */

    /**
     * Schedules rendering with debounce (16ms).
     * Prevents excessive native calls during fast slider changes.
     */

    private fun scheduleRender() {
        if (isReleased) return

        pendingRenderCount.incrementAndGet()
        renderHandler.removeCallbacksAndMessages(null)

        renderHandler.postDelayed({ render() }, 16)
    }


    /**
     * Executes image processing on background thread.
     */
    private fun render() {
        if (isReleased) return
        if (!isProcessing.compareAndSet(false, true)) return

        try {
            pendingRenderCount.set(0)

            processor.process(
                previewOriginal,
                frontWorkBitmap,
                config.brightness,
                config.exposure,
                config.contrast,
                config.hue,
                config.saturation,
                config.highlight,
                config.shadows,
                config.grain,
                config.sharpness,
                config.vignette
            )

            displayCanvas.drawBitmap(frontWorkBitmap, 0f, 0f, null)

            // Swap buffers
            val tmp = frontWorkBitmap
            frontWorkBitmap = backWorkBitmap
            backWorkBitmap = tmp

            // Deliver result to UI
            mainHandler.post {
                if (!isReleased) onPreviewReady(displayBitmap)
            }

        } catch (t: Throwable) {
            Log.e("ImageCraft", "Render failed", t)
        } finally {
            isProcessing.set(false)
            if (pendingRenderCount.get() > 0) scheduleRender()
        }
    }


    /* ---------------------------------------------------------
    * Final Render (Export)
    * --------------------------------------------------------- */

    /**
     * Renders full-resolution output for saving/exporting.
     */
    fun renderFinal(): Bitmap {
        val output = Bitmap.createBitmap(
            fullOriginal.width,
            fullOriginal.height,
            Bitmap.Config.ARGB_8888
        )
        processor.process(
            fullOriginal,
            output,
            config.brightness, config.exposure, config.contrast,
            config.hue, config.saturation, config.highlight,
            config.shadows, config.grain, config.sharpness, config.vignette
        )

        return output
    }

    /* ---------------------------------------------------------
    * Cleanup
    * --------------------------------------------------------- */

    /**
     * Releases all resources and stops background threads.
     * Must be called when editor is destroyed.
     */
    fun release() {
        isReleased = true
        scope.cancel()
        renderHandler.removeCallbacksAndMessages(null)
        renderThread.quitSafely()

        if (!displayBitmap.isRecycled) displayBitmap.recycle()
        if (!workBitmap1.isRecycled) workBitmap1.recycle()
        if (!workBitmap2.isRecycled) workBitmap2.recycle()
    }
}