package com.example.imgcraft.sample.compsample.screen

import android.graphics.Bitmap
import android.view.MotionEvent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.imgcraft.sample.R
import com.example.imgcraft.sample.adapter.AdjustmentTool
import com.example.imgcraft.sample.compsample.component.SavingDialog
import com.example.imgcraft.sample.compsample.component.ToolsItem
import com.example.imgcraft.sample.util.tools

/**
 * @author: Muhammad Noman
 * @github: https://github.com/muhammadnoman11
 * @date: 12/16/2025
 */

@Composable
fun ImageEditorScreen(
    modifier: Modifier = Modifier,
    previewBitmap: Bitmap?,
    editorVisible: Boolean,
    isSaving: Boolean,
    selectedTool: AdjustmentTool,
    sliderValue: Float,
    onPickImage: () -> Unit,
    onSave: () -> Unit,
    onCompareDown: () -> Unit,
    onCompareUp: () -> Unit,
    onReset: () -> Unit,
    onToolSelected: (AdjustmentTool) -> Unit,
    onSliderChange: (Float) -> Unit
) {

    val listState = rememberLazyListState()

    LaunchedEffect(selectedTool) {
        if (selectedTool == tools.first()) {
            listState.animateScrollToItem(0)
        }
    }


    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {

        if (!editorVisible) {
            Button(
                onClick = onPickImage,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text("Pick Image")
            }
            return
        }

        Column(modifier = Modifier.fillMaxSize()) {

            // ---------- HEADER ----------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {


                Text(
                    text = "Sample",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )

                Spacer(Modifier.weight(1f))



                Icon(
                    painter = painterResource(R.drawable.restore),
                    contentDescription = null,
                    tint = Color.DarkGray,

                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onReset() }
                )

                Spacer(Modifier.width(16.dp))

                Icon(
                    painter = painterResource(R.drawable.compare),
                    contentDescription = null,
                    tint = Color.DarkGray,
                    modifier = Modifier
                        .size(28.dp)
                        .pointerInteropFilter {
                            when (it.action) {
                                MotionEvent.ACTION_DOWN -> onCompareDown()
                                MotionEvent.ACTION_UP,
                                MotionEvent.ACTION_CANCEL -> onCompareUp()
                            }
                            true
                        }
                )




                Spacer(Modifier.width(16.dp))

                Button(onClick = onSave) {
                    Text("Save")
                }
            }

            // ---------- IMAGE ----------
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                previewBitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.aspectRatio(1f),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Text(
                text = "Value: ${sliderValue.toInt()}",
                color = Color.Black
            )

            // ---------- SLIDER ----------
            Slider(
                value = sliderValue,
                onValueChange = { onSliderChange(it) },
                valueRange = 0f..100f,
            )

            Spacer(Modifier.height(12.dp))

            // ---------- TOOLS ----------
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(tools, key = { it.name }) { tool ->
                    ToolsItem(
                        tool = tool,
                        isSelected = tool == selectedTool,
                        onToolSelected = { onToolSelected(tool) }
                    )
                }
            }
        }

        if (isSaving) {
            SavingDialog(message = "Saving image...")
        }
    }
}
