package com.example.watermarkcamera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

class ImageCompressor(private val context: Context) {

    companion object {
        private const val TAG = "ImageCompressor"
        private const val DEFAULT_QUALITY = 85
        private const val MIN_QUALITY = 50
        private const val MAX_QUALITY = 95
    }

    enum class CompressionLevel {
        LOW, MEDIUM, HIGH
    }

    data class CompressionOptions(
        val quality: Int = DEFAULT_QUALITY,
        val maxWidth: Int = 1920,
        val maxHeight: Int = 1080,
        val format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
    )

    suspend fun compressBitmap(
        bitmap: Bitmap,
        options: CompressionOptions = CompressionOptions()
    ): Bitmap = withContext(Dispatchers.Default) {
        val scaledBitmap = scaleBitmap(bitmap, options.maxWidth, options.maxHeight)

        // 如果质量已经是最大值且尺寸没有变化，直接返回
        if (options.quality >= MAX_QUALITY && scaledBitmap == bitmap) {
            return@withContext bitmap
        }

        // 进行质量压缩
        ByteArrayOutputStream().use { outputStream ->
            scaledBitmap.compress(options.format, options.quality, outputStream)
            val byteArray = outputStream.toByteArray()
            BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size) ?: scaledBitmap
        }
    }

    suspend fun compressToFile(
        bitmap: Bitmap,
        outputFile: File,
        options: CompressionOptions = CompressionOptions()
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val compressedBitmap = compressBitmap(bitmap, options)

            FileOutputStream(outputFile).use { outputStream ->
                compressedBitmap.compress(options.format, options.quality, outputStream)
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun compressToTargetSize(
        bitmap: Bitmap,
        targetSizeKB: Int,
        maxWidth: Int = 1920,
        maxHeight: Int = 1080
    ): Bitmap = withContext(Dispatchers.Default) {
        val targetBytes = targetSizeKB * 1024
        var low = MIN_QUALITY
        var high = MAX_QUALITY
        var bestQuality = MIN_QUALITY

        val scaledBitmap = scaleBitmap(bitmap, maxWidth, maxHeight)

        while (low <= high) {
            val mid = (low + high) / 2
            val size = calculateCompressedSize(scaledBitmap, mid)

            if (size <= targetBytes) {
                bestQuality = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }

        ByteArrayOutputStream().use { outputStream ->
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, bestQuality, outputStream)
            val byteArray = outputStream.toByteArray()
            BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size) ?: scaledBitmap
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        // 如果原图已经小于目标尺寸，直接返回
        if (originalWidth <= maxWidth && originalHeight <= maxHeight) {
            return bitmap
        }

        val scaleX = maxWidth.toFloat() / originalWidth
        val scaleY = maxHeight.toFloat() / originalHeight
        val scale = min(scaleX, scaleY)

        val newWidth = (originalWidth * scale).toInt()
        val newHeight = (originalHeight * scale).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun calculateCompressedSize(bitmap: Bitmap, quality: Int): Int {
        return ByteArrayOutputStream().use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            outputStream.size()
        }
    }

    fun getOptimalSizeForDevice(): Size {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val availableProcessors = runtime.availableProcessors()

        return when {
            maxMemory > 128 * 1024 * 1024 && availableProcessors >= 6 -> Size(2560, 1440)
            maxMemory > 64 * 1024 * 1024 && availableProcessors >= 4 -> Size(1920, 1080)
            else -> Size(1280, 720)
        }
    }
}
