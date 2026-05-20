package com.example.watermarkcamera

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.GestureDetector
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.watermarkcamera.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import java.io.File
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraManager: CameraManager
    private lateinit var locationManager: com.example.watermarkcamera.LocationManager
    private lateinit var watermarkProcessor: WatermarkProcessor
    private lateinit var permissionManager: PermissionManager
    private lateinit var elevationManager: ElevationManager
    private lateinit var configManager: ConfigManager
    private lateinit var gestureDetector: GestureDetectorCompat

    private var currentLocation: android.location.Location? = null
    private var lastCapturedImage: File? = null
    private var isProcessingImage = false

    // 缩放手势相关变量
    private var initialDistance = 0f
    private var currentZoom = 1.0f

    companion object {
        private const val TAG = "MainActivity"
        private const val MIN_DISTANCE = 10f
        private const val ANIMATION_DURATION = 300L
        private const val LOCATION_WAIT_TIMEOUT = 5000L
    }

    // 使用 Activity Result API 替代已弃用的 onActivityResult
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                processGalleryImage(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeComponents()
        setupClickListeners()
        setupGestureDetector()
        setupAnimations()

        // 检查权限
        if (permissionManager.hasAllRequiredPermissions()) {
            startCamera()
        } else {
            permissionManager.requestAllPermissions(this)
        }
    }

    private fun initializeComponents() {
        permissionManager = PermissionManager(this)
        configManager = ConfigManager(this)
        watermarkProcessor = WatermarkProcessor(this)
        elevationManager = ElevationManager(this)

        cameraManager = CameraManager(
            context = this,
            lifecycleOwner = this,
            previewView = binding.previewView,
            onImageCaptured = { bitmap ->
                processCapturedImage(bitmap)
            }
        )

        locationManager = com.example.watermarkcamera.LocationManager(this) { location ->
            // 只更新当前位置如果新位置更精确
            val currentAccuracy = currentLocation?.accuracy ?: Float.MAX_VALUE
            val newAccuracy = location.accuracy
            
            if (newAccuracy <= currentAccuracy) {
                currentLocation = location
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "位置更新: ${location.latitude}, ${location.longitude}, 精度: ${newAccuracy}米")
                }
            }
        }

        lifecycle.addObserver(locationManager)
    }

    private fun setupClickListeners() {
        // 拍照按钮 - 带缩放动画
        binding.captureButton.setOnClickListener { view ->
            animateButtonPress(view) {
                capturePhoto()
            }
        }

        // 设置按钮
        binding.settingsButton.setOnClickListener { view ->
            animateButtonPress(view) {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        }

        // 最后拍摄照片预览
        binding.imageView.setOnClickListener { view ->
            animateButtonPress(view) {
                lastCapturedImage?.let { file ->
                    openFullscreenImage(file)
                }
            }
        }

        // 图库按钮
        binding.galleryButton.setOnClickListener { view ->
            animateButtonPress(view) {
                openGallery()
            }
        }

        // 闪光灯按钮
        binding.flashButton.setOnClickListener { view ->
            animateButtonPress(view) {
                toggleFlash()
            }
        }
    }

    private fun setupAnimations() {
        // 入场动画
        binding.topToolbar.apply {
            alpha = 0f
            translationY = -100f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(ANIMATION_DURATION)
                .setInterpolator(OvershootInterpolator())
                .start()
        }

        binding.bottomControls.apply {
            alpha = 0f
            translationY = 100f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(ANIMATION_DURATION)
                .setStartDelay(100)
                .setInterpolator(OvershootInterpolator())
                .start()
        }

        // 拍照按钮脉冲动画
        startCaptureButtonPulse()
    }

    private fun startCaptureButtonPulse() {
        val pulseAnimator = ObjectAnimator.ofFloat(binding.captureButton, "scaleX", 1f, 1.05f, 1f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }

        val pulseAnimatorY = ObjectAnimator.ofFloat(binding.captureButton, "scaleY", 1f, 1.05f, 1f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }

        AnimatorSet().apply {
            playTogether(pulseAnimator, pulseAnimatorY)
            start()
        }
    }

    private fun animateButtonPress(view: View, onComplete: () -> Unit) {
        view.animate()
            .scaleX(0.85f)
            .scaleY(0.85f)
            .setDuration(100)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .setInterpolator(AnticipateOvershootInterpolator())
                    .withEndAction {
                        onComplete()
                    }
                    .start()
            }
            .start()
    }

    private fun animateCaptureSuccess() {
        // 拍照成功动画
        val flash = ObjectAnimator.ofFloat(binding.previewView, "alpha", 1f, 0.5f, 1f).apply {
            duration = 200
        }

        val scaleX = ObjectAnimator.ofFloat(binding.imageView, "scaleX", 0.8f, 1.1f, 1f).apply {
            duration = 400
            interpolator = OvershootInterpolator()
        }

        val scaleY = ObjectAnimator.ofFloat(binding.imageView, "scaleY", 0.8f, 1.1f, 1f).apply {
            duration = 400
            interpolator = OvershootInterpolator()
        }

        AnimatorSet().apply {
            playSequentially(flash, AnimatorSet().apply {
                playTogether(scaleX, scaleY)
            })
            start()
        }
    }

    private fun setupGestureDetector() {
        gestureDetector = GestureDetectorCompat(this, MyGestureListener())

        binding.previewView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            handleZoomGesture(event)
            true
        }
    }

    private fun handleZoomGesture(event: MotionEvent) {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    initialDistance = getDistance(event)
                    if (initialDistance > MIN_DISTANCE) {
                        currentZoom = cameraManager.getCurrentZoomRatio()
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 2) {
                    val newDistance = getDistance(event)
                    if (initialDistance > MIN_DISTANCE && newDistance > MIN_DISTANCE) {
                        val scale = newDistance / initialDistance
                        val newZoom = currentZoom * scale
                        cameraManager.setZoomRatio(newZoom)
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_gallery -> {
                openGallery()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        permissionManager.onRequestPermissionsResult(
            requestCode, permissions, grantResults,
            onPermissionGranted = {
                startCamera()
            },
            onPermissionDenied = {
                Toast.makeText(this, "权限未授予，应用无法正常运行", Toast.LENGTH_SHORT).show()
                finish()
            }
        )
    }

    private fun startCamera() {
        cameraManager.initializeCamera()
        updateFlashIcon(cameraManager.getCurrentFlashMode())
        
        // 启动时就开始获取精确位置
        locationManager.waitForAccurateLocation(8000) { location ->
            location?.let {
                currentLocation = it
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "初始位置获取完成，精度: ${it.accuracy}米")
                }
            }
        }
    }

    private fun capturePhoto() {
        Log.d(TAG, "拍照按钮被点击")
        
        // 检查位置精度，如果不够精确则等待
        if (!locationManager.isLocationAccurateEnough()) {
            Toast.makeText(this, "正在获取精确位置...", Toast.LENGTH_SHORT).show()
            
            locationManager.waitForAccurateLocation(LOCATION_WAIT_TIMEOUT) { location ->
                runOnUiThread {
                    currentLocation = location
                    val accuracy = location?.accuracy ?: Float.MAX_VALUE
                    if (accuracy > 50f) {
                        Toast.makeText(this, "位置精度较低(${accuracy.toInt()}米)，建议到开阔地带", Toast.LENGTH_LONG).show()
                    }
                    // 执行拍照
                    performCapture()
                }
            }
        } else {
            performCapture()
        }
    }
    
    private fun performCapture() {
        cameraManager.capturePhotoInMemory { bitmap ->
            processCapturedImage(bitmap)
        }
    }

    private fun processCapturedImage(bitmap: android.graphics.Bitmap) {
        // 防止重复处理
        if (isProcessingImage) {
            Log.w(TAG, "正在处理图片，忽略新请求")
            return
        }
        isProcessingImage = true
        
        val locationSnapshot = currentLocation

        // 显示缩略图（立即显示，不等待处理完成）
        binding.imageView.setImageBitmap(bitmap)
        
        // 播放拍照成功动画
        animateCaptureSuccess()

        // 使用 SupervisorJob 防止子协程异常影响其他协程
        val supervisorJob = SupervisorJob()
        val scope = CoroutineScope(Dispatchers.Main + supervisorJob)
        
        scope.launch {
            try {
                // 在后台线程处理图片
                val result = withContext(Dispatchers.IO) {
                    processImageInternal(bitmap, locationSnapshot)
                }

                // 在主线程更新UI
                result?.let { (watermarkedBitmap, savedFile, savedToGallery) ->
                    savedFile?.let { lastCapturedImage = it }
                    binding.imageView.setImageBitmap(watermarkedBitmap)

                    val message = if (savedToGallery) "照片已保存到相册" else "照片已保存"
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: CancellationException) {
                // 协程被取消，正常情况
                Log.d(TAG, "图片处理被取消")
            } catch (e: Exception) {
                Log.e(TAG, "处理图片时出错: ${e.message}")
                Toast.makeText(this@MainActivity, "处理图片失败: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                // 释放原始位图内存
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
                isProcessingImage = false
                supervisorJob.cancel()
            }
        }
    }

    private suspend fun processImageInternal(
        bitmap: android.graphics.Bitmap,
        location: android.location.Location?
    ): Triple<android.graphics.Bitmap, File?, Boolean>? {
        // 获取海拔信息（如果启用）
        val finalLocation = if (location != null && !location.hasAltitude() && configManager.fetchElevation) {
            try {
                elevationManager.getElevationForLocation(location)
            } catch (e: Exception) {
                Log.e(TAG, "获取海拔失败，使用原始位置: ${e.message}")
                location
            }
        } else {
            location
        }

        // 添加水印
        val watermarkedBitmap = watermarkProcessor.addWatermarkToImage(bitmap, finalLocation)
        val filename = "watermarked_photo_${System.currentTimeMillis()}.jpg"

        // 使用 lifecycleScope.async 并行保存
        val saveToPrivateDir = lifecycleScope.async(Dispatchers.IO) {
            watermarkProcessor.saveBitmapToFile(watermarkedBitmap, filename)
        }

        val saveToGallery = lifecycleScope.async(Dispatchers.IO) {
            watermarkProcessor.saveBitmapToGallery(watermarkedBitmap, filename)
        }

        val savedFile = saveToPrivateDir.await()
        val savedToGallery = saveToGallery.await()

        return Triple(watermarkedBitmap, savedFile, savedToGallery)
    }

    private fun processGalleryImage(uri: Uri) {
        lifecycleScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(contentResolver, uri)
                }
                processCapturedImage(bitmap)
            } catch (e: Exception) {
                Log.e(TAG, "加载图库图片失败: ${e.message}")
                Toast.makeText(this@MainActivity, "加载图片失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openFullscreenImage(imageFile: File) {
        val intent = Intent(this, FullscreenImageActivity::class.java).apply {
            putExtra("IMAGE_URI", Uri.fromFile(imageFile))
        }
        startActivity(intent)
    }

    private fun toggleFlash() {
        val currentFlashMode = cameraManager.getCurrentFlashMode()
        val newFlashMode = when (currentFlashMode) {
            CameraManager.FLASH_MODE_OFF -> CameraManager.FLASH_MODE_ON
            CameraManager.FLASH_MODE_ON -> CameraManager.FLASH_MODE_AUTO
            else -> CameraManager.FLASH_MODE_OFF
        }
        cameraManager.setFlashMode(newFlashMode)
        Log.d(TAG, "闪光灯模式切换为: $newFlashMode")
        updateFlashIcon(newFlashMode)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        galleryLauncher.launch(intent)
    }

    private fun updateFlashIcon(flashMode: String) {
        val flashIcon = when (flashMode) {
            CameraManager.FLASH_MODE_OFF -> R.drawable.icons8_flash_off_100
            CameraManager.FLASH_MODE_ON -> R.drawable.icons8_flash_100
            CameraManager.FLASH_MODE_AUTO -> R.drawable.icons8_flash_auto_100
            else -> R.drawable.icons8_flash_auto_100
        }
        binding.flashButton.setImageResource(flashIcon)
    }

    private fun getDistance(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return sqrt((x * x + y * y).toDouble()).toFloat()
    }

    inner class MyGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            val x = e.x
            val y = e.y
            binding.focusRingView.showAt(x, y)
            cameraManager.focusAtPoint(x, y)
            return true
        }

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager.release()
        _binding = null
    }
}
