package com.example.imgcraft.sample.adapter

/**
 * @author: Muhammad Noman
 * @github: https://github.com/muhammadnoman11
 * @date: 12/15/2025
 */
data class AdjustmentTool(
    val name: String,
    var progressValue: Int,
    val defaultValue: Int,
    var isSelected: Boolean = false
)
