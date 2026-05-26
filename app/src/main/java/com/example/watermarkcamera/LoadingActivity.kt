package com.example.watermarkcamera

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class LoadingActivity : AppCompatActivity() {

    private lateinit var ivLoading: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvProgress: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var currentProgress = 0

    companion object {
        private const val TOTAL_DURATION = 2000L
        private const val UPDATE_INTERVAL = 20L
        private const val PROGRESS_INCREMENT = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        setupBackPressHandler()
        initViews()
        startLoading()
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 禁用返回键，防止用户中断加载
            }
        })
    }

    private fun initViews() {
        ivLoading = findViewById(R.id.ivLoading)
        progressBar = findViewById(R.id.progressBar)
        tvProgress = findViewById(R.id.tvProgress)

        progressBar.max = 100
        progressBar.progress = 0
        tvProgress.text = "0%"
    }

    private fun startLoading() {
        val totalSteps = (TOTAL_DURATION / UPDATE_INTERVAL).toInt()
        val progressPerStep = 100.0 / totalSteps

        val updateRunnable = object : Runnable {
            override fun run() {
                if (currentProgress < 100) {
                    currentProgress = (currentProgress + progressPerStep).toInt().coerceAtMost(100)
                    
                    progressBar.progress = currentProgress
                    tvProgress.text = "$currentProgress%"

                    if (currentProgress < 100) {
                        handler.postDelayed(this, UPDATE_INTERVAL)
                    } else {
                        navigateToMain()
                    }
                }
            }
        }

        handler.postDelayed(updateRunnable, UPDATE_INTERVAL)
    }

    private fun navigateToMain() {
        handler.postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, 300)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
