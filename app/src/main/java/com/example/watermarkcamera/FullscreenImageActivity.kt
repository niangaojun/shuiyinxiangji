package com.example.watermarkcamera

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.watermarkcamera.R

class FullscreenImageActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_image)
        
        // 设置状态栏为透明
        window.statusBarColor = ContextCompat.getColor(this, android.R.color.transparent)
        
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        
        val fullscreenImageView = findViewById<ImageView>(R.id.fullscreenImageView)
        
        // 获取从MainActivity传递过来的图片URI
        val imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("IMAGE_URI", Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("IMAGE_URI") as? Uri
        }
        
        imageUri?.let {
            Log.d("FullscreenImageActivity", "加载图片: $it")
            fullscreenImageView.setImageURI(it)
        } ?: run {
            Log.e("FullscreenImageActivity", "无法获取图片URI")
        }
        
        // 点击全屏图片可以返回
        fullscreenImageView.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        
        // 设置返回按钮的动画效果
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
                // 使用淡入淡出的动画效果
                @Suppress("DEPRECATION")
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        })
    }
}