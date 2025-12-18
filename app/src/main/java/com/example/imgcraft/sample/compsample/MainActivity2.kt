package com.example.imgcraft.sample.compsample

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.imgcraft.sample.adapter.AdjustmentTool
import com.example.imgcraft.sample.compsample.screen.ImageEditorScreen
import com.example.imgcraft.sample.compsample.ui.theme.ImgCraftTheme
import com.example.imgcraft.sample.util.ImageSaver
import com.example.imgcraft.sample.util.tools
import com.github.muhammadnoman11.imgcraft.core.ImageCraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity2 : ComponentActivity() {

    private lateinit var imgCraft: ImageCraft

    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri == null) return@registerForActivityResult

            imgCraft = ImageCraft(this, uri) { preview ->
                _previewBitmap.value = preview
            }

            _editorVisible.value = true
        }

    private val _previewBitmap = mutableStateOf<Bitmap?>(null)
    private val _editorVisible = mutableStateOf(false)
    private val _isSaving = mutableStateOf(false)
    private val _selectedTool = mutableStateOf(tools.first())
    private val _sliderValue = mutableStateOf(tools.first().progressValue.toFloat())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ImgCraftTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ImageEditorScreen(
                        modifier = Modifier.padding(innerPadding),
                        previewBitmap = _previewBitmap.value,
                        editorVisible = _editorVisible.value,
                        isSaving = _isSaving.value,
                        selectedTool = _selectedTool.value,
                        sliderValue = _sliderValue.value,
                        onPickImage = {
                            pickMedia.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onSave = { saveImage() },
                        onCompareDown = { imgCraft.showBefore() },
                        onCompareUp = { imgCraft.showAfter() },
                        onReset = { resetTools() },
                        onToolSelected = { tool ->
                            _selectedTool.value = tool
                            _sliderValue.value = tool.progressValue.toFloat()
                        },
                        onSliderChange = { value ->
                            _sliderValue.value = value
                            _selectedTool.value.progressValue = value.toInt()
                            applyAdjustment(_selectedTool.value, value.toInt())
                        }
                    )
                }
            }
        }
    }

    private fun saveImage() {
        lifecycleScope.launch {
            _isSaving.value = true
            try {
                val bitmap = withContext(Dispatchers.Default) {
                    imgCraft.renderFinal()
                }

                val result = ImageSaver.saveBitmap(
                    applicationContext,
                    bitmap,
                    "craft_${System.currentTimeMillis()}"
                )

                result.onSuccess { uri ->
                    Toast.makeText(
                        applicationContext,
                        "Saved to: $uri",
                        Toast.LENGTH_SHORT
                    ).show()
                }.onFailure {
                    Toast.makeText(
                        applicationContext,
                        "Save failed",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } finally {
                _isSaving.value = false
            }
        }
    }

    private fun resetTools() {
        imgCraft.reset()
        tools.forEachIndexed { index, tool ->
            tool.isSelected = index == 0
            tool.progressValue = tool.defaultValue
        }
        _selectedTool.value = tools.first()
        _sliderValue.value = tools.first().progressValue.toFloat()
    }

    private fun applyAdjustment(tool: AdjustmentTool, progress: Int) {
        when (tool.name) {
            "Brightness" -> imgCraft.setBrightness((progress - 50) / 50f)
            "Contrast" -> imgCraft.setContrast((progress - 50) / 50f)
            "Exposure" -> imgCraft.setExposure((progress - 50) / 50f)
            "Hue" -> imgCraft.setHue((progress - 50) / 50f)
            "Saturation" -> imgCraft.setSaturation((progress - 50) / 50f)
            "Highlight" -> imgCraft.setHighlight((progress - 50) / 50f)
            "Shadows" -> imgCraft.setShadows((progress - 50) / 50f)
            "Grain" -> imgCraft.setGrain(progress / 100f)
            "Sharpness" -> imgCraft.setSharpness(progress / 100f)
            "Vignette" -> imgCraft.setVignette(progress / 100f)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::imgCraft.isInitialized) imgCraft.release()
    }
}
