//
// Created by Muhammad Noman on 12/15/2025.
//

#include "helper.h"
#include <cmath>


// ==================== COLOR CONVERSION ====================

void rgbToHsv(float r, float g, float b, float &h, float &s, float &v) {
    r /= 255.0f; g /= 255.0f; b /= 255.0f;

    float max = fmax(r, fmax(g, b));
    float min = fmin(r, fmin(g, b));
    float d = max - min;

    v = max;
    s = (max == 0.0f) ? 0.0f : (d / max);

    if (d == 0.0f) {
        h = 0.0f;
    } else {
        if (max == r)
            h = fmod((g - b) / d, 6.0f);
        else if (max == g)
            h = ((b - r) / d) + 2.0f;
        else
            h = ((r - g) / d) + 4.0f;

        h *= 60.0f;
        if (h < 0.0f) h += 360.0f;
    }
}

void hsvToRgb(float h, float s, float v, float &r, float &g, float &b) {
    float c = v * s;
    float x = c * (1.0f - fabs(fmod(h / 60.0f, 2.0f) - 1.0f));
    float m = v - c;

    float rr, gg, bb;

    if (h < 60.0f)       { rr = c; gg = x; bb = 0; }
    else if (h < 120.0f) { rr = x; gg = c; bb = 0; }
    else if (h < 180.0f) { rr = 0; gg = c; bb = x; }
    else if (h < 240.0f) { rr = 0; gg = x; bb = c; }
    else if (h < 300.0f) { rr = x; gg = 0; bb = c; }
    else                 { rr = c; gg = 0; bb = x; }

    r = (rr + m) * 255.0f;
    g = (gg + m) * 255.0f;
    b = (bb + m) * 255.0f;
}


// ==================== IMAGE PROCESSING ====================

int clamp(int v) {
    return v < 0 ? 0 : (v > 255 ? 255 : v);
}

float clampf(float v) {
    return v < 0.0f ? 0.0f : (v > 255.0f ? 255.0f : v);
}

float luminance(float r, float g, float b) {
    return 0.299f * r + 0.587f * g + 0.114f * b;
}

float contrastCurve(float x, float contrast) {
    // x in 0..1
    // contrast in -1..1

    // Map contrast to curve strength
    float k = (contrast >= 0.0f)
            ? (1.0f + contrast * 0.5f)
            : (1.0f + contrast * 0.2f);

    // S-curve using smoothstep
    x = x - 0.5f;
    x = x * k;
    x = x + 0.5f;

    return fminf(fmaxf(x, 0.0f), 1.0f);
}

// Box blur with proper edge handling
void boxBlur(uint32_t* src, uint32_t* dst, int w, int h) {
    for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
            int r = 0, g = 0, b = 0;
            int count = 0;

            for (int ky = -1; ky <= 1; ky++) {
                for (int kx = -1; kx <= 1; kx++) {
                    int ny = y + ky;
                    int nx = x + kx;

                    if (ny >= 0 && ny < h && nx >= 0 && nx < w) {
                        uint32_t c = src[ny * w + nx];
                        r += (c >> 16) & 0xFF;
                        g += (c >> 8) & 0xFF;
                        b += c & 0xFF;
                        count++;
                    }
                }
            }

            if (count > 0) {
                dst[y * w + x] = (0xFF << 24) | ((r / count) << 16) | ((g / count) << 8) | (b / count);
            } else {
                dst[y * w + x] = src[y * w + x];
            }
        }
    }
}

// Unsharp mask for sharpening
void unsharpMask(uint32_t* src, uint32_t* dst, int w, int h, float amount) {
    uint32_t* blur = new uint32_t[w * h];
    boxBlur(src, blur, w, h);

    for (int i = 0; i < w * h; i++) {
        uint32_t orig = src[i];
        uint32_t blurred = blur[i];

        int origR = (orig >> 16) & 0xFF;
        int origG = (orig >> 8) & 0xFF;
        int origB = orig & 0xFF;

        int blurR = (blurred >> 16) & 0xFF;
        int blurG = (blurred >> 8) & 0xFF;
        int blurB = blurred & 0xFF;

        int sharpR = origR + (int)((origR - blurR) * amount);
        int sharpG = origG + (int)((origG - blurG) * amount);
        int sharpB = origB + (int)((origB - blurB) * amount);

        dst[i] = (0xFF << 24) | (clamp(sharpR) << 16) | (clamp(sharpG) << 8) | clamp(sharpB);
    }

    delete[] blur;
}