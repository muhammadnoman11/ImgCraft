//
// Created by Muhammad Noman on 12/15/2025.
//

#ifndef EASYIMAGE_HELPER_H
#define EASYIMAGE_HELPER_H

#include <stdint.h>

// RGB ↔ HSV
void rgbToHsv(float r, float g, float b,
        float &h, float &s, float &v);

void hsvToRgb(float h, float s, float v,
        float &r, float &g, float &b);

int clamp(int v);

float luminance(float r, float g, float b);

float clampf(float v);

float contrastCurve(float x, float contrast);


void boxBlur(uint32_t* src, uint32_t* dst,
        int w, int h);

void unsharpMask(uint32_t* src, uint32_t* dst,
        int w, int h, float amount);

#endif //EASYIMAGE_HELPER_H
