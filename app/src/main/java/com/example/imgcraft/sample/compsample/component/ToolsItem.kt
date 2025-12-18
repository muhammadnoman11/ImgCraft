package com.example.imgcraft.sample.compsample.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.magnifier
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.imgcraft.sample.adapter.AdjustmentTool

/**
 * @author: Muhammad Noman
 * @github: https://github.com/muhammadnoman11
 * @date: 12/16/2025
 */

@Composable
fun ToolsItem(
    tool: AdjustmentTool,
    isSelected: Boolean,
    onToolSelected: (AdjustmentTool) -> Unit
) {
    Text(
        modifier = Modifier
            .padding(10.dp)
            .clickable { onToolSelected(tool) },
        text = tool.name,
        fontSize = 18.sp,
        color = if (isSelected) Color.Blue else Color.Black,
        fontFamily = FontFamily.Serif
    )
}
