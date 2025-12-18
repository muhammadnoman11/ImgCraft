package com.example.imgcraft.sample.util


import com.example.imgcraft.sample.adapter.AdjustmentTool
import com.github.muhammadnoman11.imgcraft.util.ToolDefaults


/**
 * @author: Muhammad Noman
 * @github: https://github.com/muhammadnoman11
 * @date: 12/16/2025
 */


val tools = mutableListOf(
    AdjustmentTool("Brightness", progressValue = ToolDefaults.CENTER, defaultValue = ToolDefaults.CENTER),
    AdjustmentTool("Contrast", progressValue = ToolDefaults.CENTER, defaultValue = ToolDefaults.CENTER),
    AdjustmentTool("Exposure", progressValue = ToolDefaults.CENTER, defaultValue = ToolDefaults.CENTER),
    AdjustmentTool("Hue", progressValue = ToolDefaults.CENTER, defaultValue = ToolDefaults.CENTER),
    AdjustmentTool("Saturation", progressValue = ToolDefaults.CENTER, defaultValue = ToolDefaults.CENTER),
    AdjustmentTool("Highlight", progressValue = ToolDefaults.CENTER, defaultValue = ToolDefaults.CENTER),
    AdjustmentTool("Shadows", progressValue = ToolDefaults.CENTER, defaultValue = ToolDefaults.CENTER),
    AdjustmentTool("Grain", progressValue = ToolDefaults.ZERO, defaultValue = ToolDefaults.ZERO),
    AdjustmentTool("Sharpness", progressValue = ToolDefaults.ZERO, defaultValue = ToolDefaults.ZERO),
    AdjustmentTool("Vignette", progressValue = ToolDefaults.ZERO, defaultValue = ToolDefaults.ZERO)
)
