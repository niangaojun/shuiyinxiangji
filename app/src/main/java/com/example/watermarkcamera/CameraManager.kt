package com.example.watermarkcamera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: androidx.camera.view.PreviewView,
    private val onImageCaptured: (Bitmap) -> Unit
) {

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var cameraInfo: CameraInfo? = null
    private var cameraControl: CameraControl? = null

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val isCapturing = AtomicBoolean(false)

    // 缩放相关变量
    private var currentZoomRatio: Float = 1.0f
    private var maxZoomRatio: Float = 1.0f
    private var minZoomRatio: Float = 1.0f

    // 闪光灯相关变量
    private var currentFlashMode: String = FLASH_MODE_OFF

    // 摄像头相关变量
    private var currentCameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var isUsingFrontCamera: Boolean = false

    companion object {
        private const val TAG = "CameraManager"
        private const val AUTO_FOCUS_DELAY_MS = 1000L
        private const val CAPTURE_TIMEOUT_MS = 5000L

        // 闪光灯模式常量
        const val FLASH_MODE_OFF = "off"
        const val FLASH_MODE_ON = "on"
        const val FLASH_MODE_AUTO = "auto"
    }

    fun initializeCamera() {
        startCamera()
        previewView.postDelayed({
            performInitialAutoFocus()
        }, AUTO_FOCUS_DELAY_MS)
    }

    private fun performInitialAutoFocus() {
        try {
            val meteringPoint = previewView.meteringPointFactory.createPoint(
                previewView.width / 2f,
                previewView.height / 2f
            )
            val autoFocusAction = FocusMeteringAction.Builder(meteringPoint)
                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                .build()

            cameraControl?.startFocusAndMetering(autoFocusAction)
                ?.addListener({
                    Log.d(TAG, "初始自动对焦完成")
                }, ContextCompat.getMainExecutor(context))
        } catch (e: Exception) {
            Log.e(TAG, "设置初始自动对焦失败: ${e.message}")
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()

                // 设置预览 - 使用更高效的预览尺寸
                val preview = Preview.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                // 设置图像捕获 - 使用更高效的捕获模式
                val imageCaptureBuilder = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetResolution(Size(1920, 1080))
                    .setJpegQuality(90)

                val flashMode = when (currentFlashMode) {
                    FLASH_MODE_ON -> ImageCapture.FLASH_MODE_ON
                    FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_AUTO
                    else -> ImageCapture.FLASH_MODE_OFF
                }
                imageCaptureBuilder.setFlashMode(flashMode)

                imageCapture = imageCaptureBuilder.build()

                // 图像分析 - 降低分辨率以提高性能
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setTargetResolution(Size(640, 480))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor) { imageProxy ->
                            imageProxy.close()
                        }
                    }

                cameraProvider?.unbindAll()

                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner, currentCameraSelector, preview, imageCapture, imageAnalyzer
                )

                camera?.let {
                    cameraInfo = it.cameraInfo
                    cameraControl = it.cameraControl

                    val zoomState = cameraInfo?.zoomState?.value
                    zoomState?.let { state ->
                        currentZoomRatio = state.zoomRatio
                        maxZoomRatio = state.maxZoomRatio
                        minZoomRatio = state.minZoomRatio
                        Log.d(TAG, "缩放范围: $minZoomRatio - $maxZoomRatio, 当前: $currentZoomRatio")
                    }
                }

                Log.d(TAG, "相机初始化成功")
            } catch (exc: Exception) {
                Log.e(TAG, "相机初始化失败: ${exc.message}")
            }

        }, ContextCompat.getMainExecutor(context))
    }

    fun capturePhotoInMemory(onSuccess: (Bitmap) -> Unit) {
        // 防止重复点击
        if (!isCapturing.compareAndSet(false, true)) {
            Log.w(TAG, "正在拍照中，忽略重复请求")
            return
        }

        imageCapture?.let { capture ->
            capture.takePicture(
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        try {
                            val bitmap = imageProxyToBitmap(image)
                            bitmap?.let { 
                                onSuccess(it) 
                            } ?: run {
                                Log.e(TAG, "Bitmap转换失败")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "处理图片失败: ${e.message}")
                        } finally {
                            image.close()
                            isCapturing.set(false)
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e(TAG, "拍照失败: ${exception.message}")
                        isCapturing.set(false)
                    }
                }
            )
        } ?: run {
            Log.e(TAG, "图像捕获器未初始化")
            isCapturing.set(false)
        }
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        return try {
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            
            // 使用BitmapFactory.Options优化内存
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inDither = false
            }
            
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "内存不足，无法转换图片")
            null
        } catch (e: Exception) {
            Log.e(TAG, "转换图片失败: ${e.message}")
            null
        } finally {
            // 强制垃圾回收以释放内存
            System.gc()
        }
    }

    fun release() {
        try {
            isCapturing.set(false)
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
            if (!cameraExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                cameraExecutor.shutdownNow()
            }
        } catch (e: Exception) {
            Log.e(TAG, "关闭相机执行器失败: ${e.message}")
        }
    }

    fun setZoomRatio(zoomRatio: Float) {
        val clampedZoomRatio = zoomRatio.coerceIn(minZoomRatio, maxZoomRatio)
        if (clampedZoomRatio != currentZoomRatio) {
            currentZoomRatio = clampedZoomRatio
            cameraControl?.setZoomRatio(currentZoomRatio)
            Log.d(TAG, "设置缩放比例: $currentZoomRatio")
        }
    }

    fun getCurrentZoomRatio(): Float = currentZoomRatio
    fun getMaxZoomRatio(): Float = maxZoomRatio
    fun getMinZoomRatio(): Float = minZoomRatio

    fun focusAtPoint(x: Float, y: Float) {
        try {
            val meteringPoint = previewView.meteringPointFactory.createPoint(x, y)
            val autoFocusAction = FocusMeteringAction.Builder(meteringPoint)
                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                .build()

            cameraControl?.startFocusAndMetering(autoFocusAction)
                ?.addListener({
                    Log.d(TAG, "对焦成功: ($x, $y)")
                }, ContextCompat.getMainExecutor(context))
        } catch (e: Exception) {
            Log.e(TAG, "对焦失败: ${e.message}")
        }
    }

    fun getCurrentFlashMode(): String = currentFlashMode

    fun setFlashMode(flashMode: String) {
        if (currentFlashMode != flashMode) {
            currentFlashMode = flashMode
            Log.d(TAG, "设置闪光灯模式: $flashMode")
            // 只更新flash mode，不重新初始化相机
            imageCapture?.flashMode = when (flashMode) {
                FLASH_MODE_ON -> ImageCapture.FLASH_MODE_ON
                FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_AUTO
                else -> ImageCapture.FLASH_MODE_OFF
            }
        }
    }

    fun switchCamera() {
        currentCameraSelector = if (isUsingFrontCamera) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }
        isUsingFrontCamera = !isUsingFrontCamera
        Log.d(TAG, "切换摄像头，当前使用${if (isUsingFrontCamera) "前置" else "后置"}摄像头")
        startCamera()
    }

    fun isUsingFrontCamera(): Boolean = isUsingFrontCamera
}
