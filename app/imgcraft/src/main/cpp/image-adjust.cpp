#include <jni.h>
#include <android/bitmap.h>
#include <cmath>
#include <cstring>
#include <android/log.h>
#include "helper.h"

#define LOG_TAG "ImgCraft"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)


// ==================== JNI ENTRY POINT ====================


extern "C"
JNIEXPORT void JNICALL
Java_com_github_muhammadnoman11_imgcraft_nativebridge_NativeImageProcessor_processImage(
        JNIEnv *env,
        jobject,
        jobject original,
        jobject output,
        jfloat brightness,
        jfloat exposure,
        jfloat contrast,
        jfloat hue,
        jfloat saturation,
        jfloat highlight,
        jfloat shadows,
        jfloat grain,
        jfloat sharpness,
        jfloat vignette
) {
    AndroidBitmapInfo info;
    void *srcPixels = nullptr;
    void *dstPixels = nullptr;

    // Get bitmap info and validate
    if (AndroidBitmap_getInfo(env, original, &info) != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("Failed to get bitmap info");
        return;
    }

    // Lock pixels
    if (AndroidBitmap_lockPixels(env, original, &srcPixels) != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("Failed to lock source pixels");
        return;
    }

    if (AndroidBitmap_lockPixels(env, output, &dstPixels) != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("Failed to lock destination pixels");
        AndroidBitmap_unlockPixels(env, original);
        return;
    }

    // Copy original to output
    size_t bufferSize = info.height * info.stride;
    memcpy(dstPixels, srcPixels, bufferSize);

    uint32_t *pixels = (uint32_t *) dstPixels;
    int width = info.width;
    int height = info.height;
    int size = width * height;

    // Pre-calculate values
    float expVal = powf(2.0f, exposure);
    float contrastFactor = (259.0f * (contrast * 255.0f + 255.0f)) / (255.0f * (259.0f - contrast * 255.0f));
    float cx = width / 2.0f;
    float cy = height / 2.0f;
    float maxDist = sqrtf(cx * cx + cy * cy);

    // Apply sharpness FIRST (if needed)
    if (fabs(sharpness) > 0.001f) {
        uint32_t* temp = new uint32_t[size];
        if (temp) {
            unsharpMask(pixels, temp, width, height, sharpness * 3.0f);
            memcpy(pixels, temp, size * sizeof(uint32_t));
            delete[] temp;
        }
    }

    // Main pixel processing loop
    for (int i = 0; i < size; i++) {
        uint32_t c = pixels[i];
        int a = (c >> 24) & 0xFF;
        float r = (float)((c >> 16) & 0xFF);
        float g = (float)((c >> 8) & 0xFF);
        float b = (float)(c & 0xFF);

        // BRIGHTNESS
        if (fabs(brightness) > 0.001f) {
            r += brightness * 100.0f;
            g += brightness * 100.0f;
            b += brightness * 100.0f;
        }

        // EXPOSURE
        if (fabs(exposure) > 0.001f) {
            r *= expVal;
            g *= expVal;
            b *= expVal;
        }


        // CONTRAST
        if (fabs(contrast) > 0.001f) {

            float rn = r / 255.0f;
            float gn = g / 255.0f;
            float bn = b / 255.0f;

            rn = contrastCurve(rn, contrast);
            gn = contrastCurve(gn, contrast);
            bn = contrastCurve(bn, contrast);

            r = rn * 255.0f;
            g = gn * 255.0f;
            b = bn * 255.0f;
        }

        //  HUE ROTATION
        if (fabs(hue) > 0.001f) {
            float h, s, v;
            rgbToHsv(r, g, b, h, s, v);
            h += hue * 60.0f; // Hue rotation
            if (h > 360) h -= 360;
            if (h < 0) h += 360;
            hsvToRgb(h, s, v, r, g, b);
        }

        // SATURATION
        if (fabs(saturation) > 0.001f) {
            float gray = luminance(r, g, b);
            r = gray + (r - gray) * (1.0f + saturation);
            g = gray + (g - gray) * (1.0f + saturation);
            b = gray + (b - gray) * (1.0f + saturation);
        }


        // HIGHLIGHTS
        if (fabs(highlight) > 0.001f) {

            float rn = r / 255.0f;
            float gn = g / 255.0f;
            float bn = b / 255.0f;

            float lum = 0.299f * rn + 0.587f * gn + 0.114f * bn;

            // Highlight mask (only bright areas)
            float mask = fminf(fmaxf((lum - 0.5f) / 0.5f, 0.0f), 1.0f);
            mask = mask * mask; // smooth

            if (highlight < 0.0f) {
                // LEFT: gently darken highlights (no recovery, no gray crush)
                float k = -highlight * 0.35f;   // VERY IMPORTANT: small value

                rn -= k * mask * lum;
                gn -= k * mask * lum;
                bn -= k * mask * lum;

                // Slight desaturation (natural)
                float gray = lum;
                float sat = 1.0f - k * 0.4f;
                rn = gray + (rn - gray) * sat;
                gn = gray + (gn - gray) * sat;
                bn = gray + (bn - gray) * sat;

            } else {
                // RIGHT: gently brighten highlights
                float k = highlight * 0.8f;

                rn += k * mask * (1.0f - rn);
                gn += k * mask * (1.0f - gn);
                bn += k * mask * (1.0f - bn);
            }

            r = fminf(fmaxf(rn, 0.0f), 1.0f) * 255.0f;
            g = fminf(fmaxf(gn, 0.0f), 1.0f) * 255.0f;
            b = fminf(fmaxf(bn, 0.0f), 1.0f) * 255.0f;
        }





        // SHADOWS - smoother professional style
        if (fabs(shadows) > 0.001f) {
            float lum = luminance(r, g, b) / 255.0f; // 0..1
            if (lum < 0.5f) {
                float t = lum / 0.6f; // 0..1 in shadows
                float factor = 1.0f + shadows * (1.0f - t)*(1.0f - t); // smooth curve
                r *= factor;
                g *= factor;
                b *= factor;
            }
        }


        // GRAIN
        if (fabs(grain) > 0.001f) {
            float noise = ((rand() % 200) - 100) / 100.0f;
            float lumNorm = luminance(r, g, b) / 255.0f;
            float strength = grain * (1.0f - lumNorm);
            r += noise * strength * 60.0f;
            g += noise * strength * 30.0f;
            b += noise * strength * 30.0f;
        }

        // VIGNETTE
        if (vignette != 0.0f) {
            int x = i % width;
            int y = i / width;
            float dx = x - cx;
            float dy = y - cy;
            float dist = sqrtf(dx*dx + dy*dy);

            // Smooth vignette curve
            float vig = 1.0f - vignette * powf(dist / maxDist, 1.5f);
            vig = vig < 0.0f ? 0.0f : vig;

            r *= vig;
            g *= vig;
            b *= vig;
        }

        // Clamp and write back
        pixels[i] = (a << 24) | (clamp((int)r) << 16) | (clamp((int)g) << 8) | clamp((int)b);
    }

    // Unlock pixels
    AndroidBitmap_unlockPixels(env, original);
    AndroidBitmap_unlockPixels(env, output);
}