package com.example.watermarkcamera

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import androidx.core.content.edit

class ConfigManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "watermark_camera_prefs"

        // Keys
        private const val WATERMARK_TEXT_SIZE = "watermark_text_size"
        private const val WATERMARK_COLOR = "watermark_color"
        private const val WATERMARK_BACKGROUND_ALPHA = "watermark_background_alpha"
        private const val WATERMARK_POSITION = "watermark_position"
        private const val SHOW_LOCATION = "show_location"
        private const val SHOW_DATE_TIME = "show_date_time"
        private const val CUSTOM_WATERMARK_TEXT = "custom_watermark_text"
        private const val IMAGE_QUALITY = "image_quality"
        private const val FETCH_ELEVATION = "fetch_elevation"
        private const val SHOW_ADDRESS = "show_address"

        // Defaults
        private const val DEFAULT_TEXT_SIZE = 72f
        private const val DEFAULT_COLOR = Color.WHITE
        private const val DEFAULT_BACKGROUND_ALPHA = 128
        private const val DEFAULT_POSITION = 0 // 0: 底部, 1: 顶部, 2: 左侧, 3: 右侧
        private const val DEFAULT_SHOW_LOCATION = true
        private const val DEFAULT_SHOW_DATE_TIME = true
        private const val DEFAULT_IMAGE_QUALITY = 90
        private const val DEFAULT_FETCH_ELEVATION = false
        private const val DEFAULT_SHOW_ADDRESS = true
    }

    var watermarkTextSize: Float
        get() = prefs.getFloat(WATERMARK_TEXT_SIZE, DEFAULT_TEXT_SIZE)
        set(value) = prefs.edit { putFloat(WATERMARK_TEXT_SIZE, value) }

    var watermarkColor: Int
        get() = prefs.getInt(WATERMARK_COLOR, DEFAULT_COLOR)
        set(value) = prefs.edit { putInt(WATERMARK_COLOR, value) }

    var watermarkBackgroundAlpha: Int
        get() = prefs.getInt(WATERMARK_BACKGROUND_ALPHA, DEFAULT_BACKGROUND_ALPHA)
        set(value) = prefs.edit { putInt(WATERMARK_BACKGROUND_ALPHA, value) }

    var watermarkPosition: Int
        get() = prefs.getInt(WATERMARK_POSITION, DEFAULT_POSITION)
        set(value) = prefs.edit { putInt(WATERMARK_POSITION, value) }

    var showLocation: Boolean
        get() = prefs.getBoolean(SHOW_LOCATION, DEFAULT_SHOW_LOCATION)
        set(value) = prefs.edit { putBoolean(SHOW_LOCATION, value) }

    var showDateTime: Boolean
        get() = prefs.getBoolean(SHOW_DATE_TIME, DEFAULT_SHOW_DATE_TIME)
        set(value) = prefs.edit { putBoolean(SHOW_DATE_TIME, value) }

    var customWatermarkText: String?
        get() = prefs.getString(CUSTOM_WATERMARK_TEXT, null)
        set(value) = prefs.edit { putString(CUSTOM_WATERMARK_TEXT, value) }

    var imageQuality: Int
        get() = prefs.getInt(IMAGE_QUALITY, DEFAULT_IMAGE_QUALITY)
        set(value) = prefs.edit { putInt(IMAGE_QUALITY, value) }

    var fetchElevation: Boolean
        get() = prefs.getBoolean(FETCH_ELEVATION, DEFAULT_FETCH_ELEVATION)
        set(value) = prefs.edit { putBoolean(FETCH_ELEVATION, value) }

    var showAddress: Boolean
        get() = prefs.getBoolean(SHOW_ADDRESS, DEFAULT_SHOW_ADDRESS)
        set(value) = prefs.edit { putBoolean(SHOW_ADDRESS, value) }

    fun resetToDefaults() {
        prefs.edit { clear() }
    }
}
