package com.github.muhammadnoman11.imgcraft.nativebridge

import android.graphics.Bitmap

/**
 * @author: Muhammad Noman
 * @github: https://github.com/muhammadnoman11
 * @date: 12/15/2025
 */


internal class NativeImageProcessor {

    init {
        System.loadLibrary("imgcraft")
    }

    external fun processImage(
        input: Bitmap,
        output: Bitmap,
        brightness: Float,
        exposure: Float,
        contrast: Float,
        hue: Float,
        saturation: Float,
        highlight: Float,
        shadows: Float,
        grain: Float,
        sharpness: Float,
        vignette: Float,
    )

    fun process(
        input: Bitmap,
        output: Bitmap,
        brightness: Float,
        exposure: Float,
        contrast: Float,
        hue: Float,
        saturation: Float,
        highlight: Float,
        shadows: Float,
        grain: Float,
        sharpness: Float,
        vignette: Float,
    ) {
        processImage(
            input,
            output,
            brightness,
            exposure,
            contrast,
            hue,
            saturation,
            highlight,
            shadows,
            grain,
            sharpness,
            vignette
        )
    }
}