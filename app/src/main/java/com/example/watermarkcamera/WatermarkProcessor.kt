package com.example.watermarkcamera

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.location.Location
import android.os.Build
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class WatermarkProcessor(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val configManager = ConfigManager(context)
    private val addressResolver = AddressResolver(context)

    // 使用 ConcurrentHashMap 替代 LinkedHashMap，线程安全
    private val addressCache = ConcurrentHashMap<String, Pair<String, Long>>()

    private val cacheExpiryTime = 30 * 60 * 1000L // 30分钟过期
    private val maxCacheSize = 50

    companion object {
        private const val TAG = "WatermarkProcessor"
        private const val WATERMARK_PADDING = 50
        private const val BACKGROUND_PADDING = 20
        private const val CORNER_RADIUS = 20f
        private const val LINE_SPACING_MULTIPLIER = 1.2f
    }

    suspend fun addWatermarkToImage(
        originalBitmap: Bitmap,
        location: Location?,
        watermarkText: String? = null
    ): Bitmap = withContext(Dispatchers.Default) {
        // 如果不需要水印，直接返回原图
        if (!configManager.showDateTime && !configManager.showLocation &&
            configManager.customWatermarkText.isNullOrEmpty() && watermarkText.isNullOrEmpty()) {
            return@withContext originalBitmap
        }

        // 准备水印文本
        val fullWatermarkText = watermarkText ?: buildWatermarkText(location)

        if (fullWatermarkText.isEmpty()) {
            return@withContext originalBitmap
        }

        // 创建可变的位图副本
        val mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
            ?: throw IllegalStateException("无法创建位图副本")
        
        try {
            val canvas = Canvas(mutableBitmap)
            drawWatermark(canvas, fullWatermarkText)
            mutableBitmap
        } catch (e: Exception) {
            // 如果绘制失败，释放创建的位图并返回原图
            if (mutableBitmap != originalBitmap) {
                mutableBitmap.recycle()
            }
            Log.e(TAG, "绘制水印失败: ${e.message}")
            originalBitmap
        }
    }

    private suspend fun buildWatermarkText(location: Location?): String {
        val parts = buildList {
            configManager.customWatermarkText?.takeIf { it.isNotEmpty() }?.let { add(it) }

            if (configManager.showDateTime) {
                add(dateFormat.format(Date()))
            }

            if (configManager.showLocation && location != null) {
                add(buildLocationText(location))
            }
        }

        return parts.joinToString("\n")
    }

    private suspend fun buildLocationText(location: Location): String {
        val parts = mutableListOf<String>()
        
        // 经纬度始终显示
        parts.add("经度: %.6f°".format(location.longitude))
        parts.add("纬度: %.6f°".format(location.latitude))
        
        // 海拔（如果启用且可用）
        if (configManager.fetchElevation && location.hasAltitude()) {
            parts.add("海拔: %.1f米".format(location.altitude))
        }
        
        // 地址（如果启用）
        if (configManager.showAddress) {
            parts.add(getCachedAddress(location))
        }
        
        return parts.joinToString("\n")
    }

    private suspend fun getCachedAddress(location: Location): String {
        val cacheKey = String.format(Locale.ROOT, "%.4f,%.4f", location.latitude, location.longitude)
        
        // 检查缓存
        addressCache[cacheKey]?.let { (address, timestamp) ->
            if (System.currentTimeMillis() - timestamp < cacheExpiryTime) {
                return "地址: $address"
            }
            // 过期移除
            addressCache.remove(cacheKey)
        }

        // 清理过期缓存
        cleanupCache()

        return try {
            val addressText = addressResolver.getAddressFromLocation(location.latitude, location.longitude)
            if (addressText != null) {
                addressCache[cacheKey] = Pair(addressText, System.currentTimeMillis())
                "地址: $addressText"
            } else {
                "地址: 获取失败"
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取地址异常: ${e.message}")
            "地址: 获取失败"
        }
    }

    private fun cleanupCache() {
        if (addressCache.size > maxCacheSize) {
            val currentTime = System.currentTimeMillis()
            val iterator = addressCache.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (currentTime - entry.value.second > cacheExpiryTime) {
                    iterator.remove()
                }
            }
        }
    }

    private fun drawWatermark(canvas: Canvas, text: String) {
        val textSize = configManager.watermarkTextSize
        val textColor = configManager.watermarkColor
        val backgroundAlpha = configManager.watermarkBackgroundAlpha
        val position = configManager.watermarkPosition

        val textPaint = TextPaint().apply {
            color = textColor
            isAntiAlias = true
            this.textSize = textSize
        }

        val backgroundPaint = Paint().apply {
            color = Color.argb(backgroundAlpha, 0, 0, 0)
            isAntiAlias = true
        }

        val textWidth = canvas.width - (WATERMARK_PADDING * 2)
        val textLayout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, textWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, LINE_SPACING_MULTIPLIER)
            .setIncludePad(false)
            .build()

        val textHeight = textLayout.height

        val backgroundRect = calculateBackgroundRect(
            canvas, position, textWidth, textHeight
        )

        canvas.drawRoundRect(backgroundRect, CORNER_RADIUS, CORNER_RADIUS, backgroundPaint)

        val (textX, textY) = calculateTextPosition(
            canvas, position, textWidth, textHeight
        )

        canvas.save()
        canvas.translate(textX.toFloat(), textY.toFloat())
        textLayout.draw(canvas)
        canvas.restore()
    }

    private fun calculateBackgroundRect(
        canvas: Canvas, position: Int,
        textWidth: Int, textHeight: Int
    ): RectF {
        val totalWidth = textWidth + BACKGROUND_PADDING * 2
        val totalHeight = textHeight + BACKGROUND_PADDING * 2

        return when (position) {
            0 -> RectF( // 底部
                WATERMARK_PADDING.toFloat(),
                (canvas.height - totalHeight - BACKGROUND_PADDING).toFloat(),
                (WATERMARK_PADDING + totalWidth).toFloat(),
                (canvas.height - BACKGROUND_PADDING).toFloat()
            )
            1 -> RectF( // 顶部
                WATERMARK_PADDING.toFloat(),
                BACKGROUND_PADDING.toFloat(),
                (WATERMARK_PADDING + totalWidth).toFloat(),
                (totalHeight + BACKGROUND_PADDING).toFloat()
            )
            2 -> RectF( // 左侧
                WATERMARK_PADDING.toFloat(),
                WATERMARK_PADDING.toFloat(),
                (WATERMARK_PADDING + totalWidth).toFloat(),
                (WATERMARK_PADDING + totalHeight).toFloat()
            )
            else -> RectF( // 右侧
                (canvas.width - WATERMARK_PADDING - totalWidth).toFloat(),
                WATERMARK_PADDING.toFloat(),
                (canvas.width - WATERMARK_PADDING).toFloat(),
                (WATERMARK_PADDING + totalHeight).toFloat()
            )
        }
    }

    private fun calculateTextPosition(
        canvas: Canvas, position: Int,
        textWidth: Int, textHeight: Int
    ): Pair<Float, Float> {
        val textX = when (position) {
            2 -> WATERMARK_PADDING + BACKGROUND_PADDING.toFloat()
            3 -> canvas.width - WATERMARK_PADDING - textWidth - BACKGROUND_PADDING.toFloat()
            else -> WATERMARK_PADDING + BACKGROUND_PADDING.toFloat()
        }

        val textY = when (position) {
            1 -> BACKGROUND_PADDING.toFloat()
            2, 3 -> WATERMARK_PADDING + BACKGROUND_PADDING.toFloat()
            else -> canvas.height - textHeight - BACKGROUND_PADDING.toFloat()
        }

        return Pair(textX, textY)
    }

    fun saveBitmapToFile(bitmap: Bitmap, filename: String): File? {
        var outputStream: FileOutputStream? = null
        return try {
            val directory = File(context.getExternalFilesDir(null), "watermarked_photos").apply {
                if (!exists()) mkdirs()
            }

            val file = File(directory, filename)
            outputStream = FileOutputStream(file)
            
            val quality = configManager.imageQuality.coerceIn(70, 95)
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            outputStream.flush()
            
            Log.d(TAG, "图片已保存: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "保存图片失败: ${e.message}")
            null
        } finally {
            try {
                outputStream?.close()
            } catch (e: Exception) {
                Log.w(TAG, "关闭文件流失败: ${e.message}")
            }
        }
    }

    fun saveBitmapToGallery(bitmap: Bitmap, unusedFileName: String): Boolean {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "WATERMARK_$timestamp.jpg"

            val resolver = context.contentResolver
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, imageFileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/WatermarkCamera")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val imageUri = resolver.insert(collection, contentValues)
            imageUri?.let { uri ->
                resolver.openOutputStream(uri)?.use { stream ->
                    val quality = configManager.imageQuality.coerceIn(70, 95)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }

                Log.d(TAG, "图片已保存到相册: $uri")
                true
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "保存图片到相册失败: ${e.message}")
            false
        }
    }
}
