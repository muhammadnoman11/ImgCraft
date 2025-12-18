package com.example.imgcraft.sample

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.imgcraft.sample.adapter.AdjustmentAdapter
import com.example.imgcraft.sample.adapter.AdjustmentTool
import com.example.imgcraft.sample.util.tools
import com.example.imgcraft.sample.databinding.ActivityMainBinding
import com.example.imgcraft.sample.util.ImageSaver
import com.github.muhammadnoman11.imgcraft.core.ImageCraft
import com.github.muhammadnoman11.imgcraft.util.ToolDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {

    val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@registerForActivityResult

        imgCraft = ImageCraft(this@MainActivity, uri) { preview ->
            binding.imageView.setImageBitmap(preview)
        }

        binding.pickImageBtn.visibility = View.GONE
        binding.editorArea.visibility = View.VISIBLE
    }

    private lateinit var binding: ActivityMainBinding

    private var selectedTool: AdjustmentTool? = null
    private lateinit var imgCraft: ImageCraft
    private var progressDialog: Dialog? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.editorArea.visibility = View.GONE
        binding.pickImageBtn.visibility = View.VISIBLE


        tools[0].isSelected = true
        selectedTool = tools[0]

        val adapter = AdjustmentAdapter(tools) { tool ->
            tools.forEach { it.isSelected = false }
            tool.isSelected = true

            selectedTool = tool

            binding.seekBar.max = 100
            binding.seekBar.progress = tool.progressValue
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL, false
        )
        binding.recyclerView.adapter = adapter

        setupSeekBars()

        binding.pickImageBtn.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.saveImageBtn.setOnClickListener {
            showProgressDialog("Saving image...")

            lifecycleScope.launch {
                val bitmap = withContext(Dispatchers.Default) {
                    imgCraft.renderFinal()
                }
                onSaveClicked(bitmap)
            }
        }



        binding.resetBtn.setOnClickListener {
            imgCraft.reset()
            // Reset tools to their individual default values
            tools.forEachIndexed { index, tool ->
                tool.isSelected = index == 0           // select first tool
                tool.progressValue = tool.defaultValue         // reset to tool’s own default
            }

            // Update selected tool reference
            selectedTool = tools[0]
            binding.recyclerView.scrollToPosition(0)

            // Reset SeekBar to match the selected tool
            binding.seekBar.progress = selectedTool?.progressValue ?: ToolDefaults.CENTER

            // Notify RecyclerView to refresh selection and values
            binding.recyclerView.adapter?.notifyDataSetChanged()

        }

        binding.compareBtn.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    imgCraft.showBefore()
                    true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    imgCraft.showAfter()
                    true
                }

                else -> false
            }
        }

    }

    private fun setupSeekBars() {

        binding.seekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return

                selectedTool?.let { tool ->
                    tool.progressValue = progress
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
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun onSaveClicked(bitmap: Bitmap) {
        lifecycleScope.launch {
            val result = ImageSaver.saveBitmap(
                context = applicationContext,
                bitmap = bitmap,
                displayName = "craft_${System.currentTimeMillis()}"
            )

            result.onSuccess { uri ->
                hideProgressDialog()
                Toast.makeText(this@MainActivity, "Saved to: $uri", Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                hideProgressDialog()
                error.printStackTrace()
                Toast.makeText(this@MainActivity, "Error: ${error.message}", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }


    private fun showProgressDialog(message: String) {
        if (progressDialog?.isShowing == true) return

        progressDialog = Dialog(this).apply {
            setCancelable(false)
            setContentView(R.layout.dialog_progress)
            findViewById<TextView>(R.id.tvProgressText).text = message
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            show()
        }
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
    }


    override fun onDestroy() {
        super.onDestroy()
        imgCraft.release()
    }
}