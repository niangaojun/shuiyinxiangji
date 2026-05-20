package com.example.watermarkcamera

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var configManager: ConfigManager
    private lateinit var textSizeSeekBar: SeekBar
    private lateinit var textSizeValue: TextView
    private lateinit var colorPreview: View
    private lateinit var backgroundAlphaSeekBar: SeekBar
    private lateinit var backgroundAlphaValue: TextView
    private lateinit var watermarkPositionSpinner: Spinner
    private lateinit var showLocationSwitch: SwitchCompat
    private lateinit var showDateTimeSwitch: SwitchCompat
    private lateinit var fetchElevationSwitch: SwitchCompat
    private lateinit var showAddressSwitch: SwitchCompat
    private lateinit var customWatermarkEditText: EditText
    private lateinit var imageQualitySeekBar: SeekBar
    private lateinit var imageQualityValue: TextView
    private lateinit var resetButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        configManager = ConfigManager(this)
        
        initViews()
        setupListeners()
        loadSettings()
    }
    
    private fun initViews() {
        textSizeSeekBar = findViewById(R.id.textSizeSeekBar)
        textSizeValue = findViewById(R.id.textSizeValue)
        colorPreview = findViewById(R.id.colorPreview)
        backgroundAlphaSeekBar = findViewById(R.id.backgroundAlphaSeekBar)
        backgroundAlphaValue = findViewById(R.id.backgroundAlphaValue)
        watermarkPositionSpinner = findViewById(R.id.watermarkPositionSpinner)
        showLocationSwitch = findViewById(R.id.showLocationSwitch)
        showDateTimeSwitch = findViewById(R.id.showDateTimeSwitch)
        fetchElevationSwitch = findViewById(R.id.fetchElevationSwitch)
        showAddressSwitch = findViewById(R.id.showAddressSwitch)
        customWatermarkEditText = findViewById(R.id.customWatermarkEditText)
        imageQualitySeekBar = findViewById(R.id.imageQualitySeekBar)
        imageQualityValue = findViewById(R.id.imageQualityValue)
        resetButton = findViewById(R.id.resetButton)
        
        // 设置水印位置下拉框
        val positions = arrayOf("底部", "顶部", "左侧", "右侧")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, positions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        watermarkPositionSpinner.adapter = adapter
    }
    
    private fun setupListeners() {
        // 文本大小变化
        textSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                textSizeValue.text = "${progress + 20}sp"
                if (fromUser) {
                    configManager.watermarkTextSize = (progress + 20).toFloat()
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 背景透明度变化
        backgroundAlphaSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                backgroundAlphaValue.text = "$progress%"
                if (fromUser) {
                    configManager.watermarkBackgroundAlpha = progress
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 图片质量变化
        imageQualitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                imageQualityValue.text = "$progress%"
                if (fromUser) {
                    configManager.imageQuality = progress
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 颜色选择
        colorPreview.setOnClickListener {
            showColorPickerDialog()
        }
        
        // 水印位置变化
        watermarkPositionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                configManager.watermarkPosition = position
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        // 显示位置开关
        showLocationSwitch.setOnCheckedChangeListener { _, isChecked ->
            configManager.showLocation = isChecked
        }
        
        // 显示日期时间开关
        showDateTimeSwitch.setOnCheckedChangeListener { _, isChecked ->
            configManager.showDateTime = isChecked
        }
        
        // 获取海拔信息开关
        fetchElevationSwitch.setOnCheckedChangeListener { _, isChecked ->
            configManager.fetchElevation = isChecked
        }
        
        // 显示地址信息开关
        showAddressSwitch.setOnCheckedChangeListener { _, isChecked ->
            configManager.showAddress = isChecked
        }
        
        // 自定义水印文本变化
        customWatermarkEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                configManager.customWatermarkText = customWatermarkEditText.text.toString()
            }
        }
        
        // 重置按钮
        resetButton.setOnClickListener {
            showResetConfirmationDialog()
        }
    }
    
    private fun loadSettings() {
        // 加载文本大小
        val textSize = configManager.watermarkTextSize.toInt() - 20
        textSizeSeekBar.progress = textSize
        textSizeValue.text = "${textSize + 20}sp"
        
        // 加载颜色
        colorPreview.setBackgroundColor(configManager.watermarkColor)
        
        // 加载背景透明度
        backgroundAlphaSeekBar.progress = configManager.watermarkBackgroundAlpha
        backgroundAlphaValue.text = "${configManager.watermarkBackgroundAlpha}%"
        
        // 加载水印位置
        watermarkPositionSpinner.setSelection(configManager.watermarkPosition)
        
        // 加载显示位置
        showLocationSwitch.isChecked = configManager.showLocation
        
        // 加载显示日期时间
        showDateTimeSwitch.isChecked = configManager.showDateTime
        
        // 加载获取海拔信息
        fetchElevationSwitch.isChecked = configManager.fetchElevation
        
        // 加载显示地址信息
        showAddressSwitch.isChecked = configManager.showAddress
        
        // 加载自定义水印文本
        customWatermarkEditText.setText(configManager.customWatermarkText)
        
        // 加载图片质量
        imageQualitySeekBar.progress = configManager.imageQuality
        imageQualityValue.text = "${configManager.imageQuality}%"
    }
    
    private fun showColorPickerDialog() {
        val colors = arrayOf(
            "白色", "黑色", "红色", "绿色", "蓝色", "黄色", "青色", "品红"
        )
        
        val colorValues = arrayOf(
            Color.WHITE, Color.BLACK, Color.RED, Color.GREEN, 
            Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA
        )
        
        AlertDialog.Builder(this)
            .setTitle("选择水印颜色")
            .setItems(colors) { _, which ->
                configManager.watermarkColor = colorValues[which]
                colorPreview.setBackgroundColor(colorValues[which])
            }
            .show()
    }
    
    private fun showResetConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("重置设置")
            .setMessage("确定要重置所有设置为默认值吗？")
            .setPositiveButton("确定") { _, _ ->
                configManager.resetToDefaults()
                loadSettings()
                Toast.makeText(this, "设置已重置", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
