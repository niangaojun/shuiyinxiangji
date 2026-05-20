package com.example.watermarkcamera

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class WatermarkPreviewActivity : AppCompatActivity() {
    
    private lateinit var previewImageView: ImageView
    private lateinit var originalImageView: ImageView
    private lateinit var toggleView: Button
    private lateinit var templateSelector: Spinner
    private lateinit var textSizeSeekBar: SeekBar
    private lateinit var opacitySeekBar: SeekBar
    private lateinit var textSizeValue: TextView
    private lateinit var opacityValue: TextView
    private lateinit var layout: ConstraintLayout
    
    private lateinit var configManager: ConfigManager
    private lateinit var watermarkProcessor: WatermarkProcessor
    private lateinit var loggerManager: LoggerManager
    
    private var previewBitmap: Bitmap? = null
    private var isOriginalMode = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_watermark_preview)
        
        setupToolbar()
        initViews()
        initManagers()
        setupListeners()
        loadPreviewImage()
    }
    
    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "水印预览"
    }
    
    private fun initViews() {
        previewImageView = findViewById(R.id.preview_image)
        originalImageView = findViewById(R.id.original_image)
        toggleView = findViewById(R.id.toggle_view)
        templateSelector = findViewById(R.id.template_selector)
        textSizeSeekBar = findViewById(R.id.text_size_seekbar)
        opacitySeekBar = findViewById(R.id.opacity_seekbar)
        textSizeValue = findViewById(R.id.text_size_value)
        opacityValue = findViewById(R.id.opacity_value)
        layout = findViewById(R.id.preview_layout)
    }
    
    private fun initManagers() {
        configManager = ConfigManager(this)
        loggerManager = LoggerManager(this)
        watermarkProcessor = WatermarkProcessor(this)
    }
    
    private fun setupListeners() {
        // 切换原图/水印图视图
        toggleView.setOnClickListener {
            isOriginalMode = !isOriginalMode
            updateViewMode()
        }
        
        // 模板选择
        templateSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                configManager.watermarkPosition = position
                updatePreview()
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        // 文字大小调节
        textSizeSeekBar.progress = (configManager.watermarkTextSize * 2).toInt()
        textSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val textSize = progress / 2f
                    textSizeValue.text = "${textSize}sp"
                    configManager.watermarkTextSize = textSize
                    updatePreview()
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 透明度调节
        opacitySeekBar.progress = (configManager.watermarkBackgroundAlpha * 100 / 255).toInt()
        opacitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val alpha = (progress * 255 / 100).toInt()
                    opacityValue.text = "${progress}%"
                    configManager.watermarkBackgroundAlpha = alpha
                    updatePreview()
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
    
    private fun loadPreviewImage() {
        // 使用默认的预览图片或生成测试图片
        lifecycleScope.launch {
            try {
                // 生成一个彩色渐变作为预览图片
                previewBitmap = generateTestBitmap()
                originalImageView.setImageBitmap(previewBitmap)
                previewImageView.setImageBitmap(previewBitmap)
                updatePreview()
            } catch (e: Exception) {
                loggerManager.e(TAG, "加载预览图片失败: ${e.message}", e)
            }
        }
    }
    
    private fun generateTestBitmap(): Bitmap {
        val width = 800
        val height = 600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        // 创建彩色渐变背景
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint()
        
        // 背景渐变
        val gradient = android.graphics.LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(
                Color.rgb(135, 206, 235),  // 天蓝色
                Color.rgb(255, 182, 193),  // 浅粉色
                Color.rgb(152, 251, 152),  // 淡绿色
                Color.rgb(255, 218, 185),  // 桃色
                Color.rgb(221, 160, 221)   // 梅色
            ),
            null as FloatArray?,
            android.graphics.Shader.TileMode.CLAMP
        )
        
        paint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        
        // 添加一些测试内容
        val contentPaint = android.graphics.Paint().apply {
            color = Color.argb(100, 255, 255, 255)
            textSize = 48f
            isAntiAlias = true
        }
        
        canvas.drawText("测试照片", 50f, 100f, contentPaint)
        canvas.drawText("用于水印预览", 50f, 160f, contentPaint)
        canvas.drawText("2024-01-01", 50f, 220f, contentPaint)
        
        return bitmap
    }
    
    private fun updateViewMode() {
        if (isOriginalMode) {
            originalImageView.visibility = View.VISIBLE
            previewImageView.visibility = View.GONE
            toggleView.text = "查看水印"
        } else {
            originalImageView.visibility = View.GONE
            previewImageView.visibility = View.VISIBLE
            toggleView.text = "查看原图"
        }
    }
    
    private fun updatePreview() {
        previewBitmap?.let { bitmap ->
            lifecycleScope.launch {
                try {
                    val watermarkedBitmap = watermarkProcessor.addWatermarkToImage(bitmap, null)
                    previewImageView.setImageBitmap(watermarkedBitmap)
                } catch (e: Exception) {
                    loggerManager.e(TAG, "更新预览失败: ${e.message}", e)
                }
            }
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.watermark_preview_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_reset -> {
                configManager.resetToDefaults()
                textSizeSeekBar.progress = (configManager.watermarkTextSize * 2).toInt()
                opacitySeekBar.progress = (configManager.watermarkBackgroundAlpha * 100 / 255).toInt()
                templateSelector.setSelection(0)
                updatePreview()
                true
            }
            R.id.action_save -> {
                saveCurrentSettings()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun saveCurrentSettings() {
        try {
            // 保存当前设置为默认设置
            loggerManager.i(TAG, "水印设置已保存")
            android.widget.Toast.makeText(this, "设置已保存", android.widget.Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            loggerManager.e(TAG, "保存设置失败: ${e.message}", e)
            android.widget.Toast.makeText(this, "保存失败", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 清理资源
    }
    
    companion object {
        private const val TAG = "WatermarkPreview"
    }
}