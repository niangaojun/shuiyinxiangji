package com.example.watermarkcamera

import android.content.Context
import android.graphics.*
import android.location.Location
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class WatermarkOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val configManager = ConfigManager(context)
    private val addressResolver = AddressResolver(context)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    private var currentLocation: Location? = null
    private var cachedAddress: String? = null
    private var watermarkText: String = ""

    // 手动水印样式（由 WatermarkStyleBottomSheet 设置）
    private var manualWatermarkText: String? = null
    private var manualWatermarkType: WatermarkStyleBottomSheet.WatermarkStyleType? = null

    private val textPaint = TextPaint().apply {
        isAntiAlias = true
    }

    private val timeLargePaint = TextPaint().apply {
        isAntiAlias = true
        textSize = TIME_TEXT_SIZE_LARGE
        typeface = Typeface.DEFAULT_BOLD
        color = Color.parseColor("#FFE66D")
    }

    private val timeSmallPaint = TextPaint().apply {
        isAntiAlias = true
        textSize = TIME_TEXT_SIZE_SMALL
        color = Color.WHITE
    }

    private val backgroundPaint = Paint().apply {
        isAntiAlias = true
    }

    private var updateJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        private const val WATERMARK_PADDING = 50
        private const val BACKGROUND_PADDING = 20
        private const val CORNER_RADIUS = 20f
        private const val LINE_SPACING_MULTIPLIER = 1.2f
        private const val UPDATE_INTERVAL = 1000L
        private const val TIME_TEXT_SIZE_LARGE = 96f
        private const val TIME_TEXT_SIZE_SMALL = 32f
    }

    init {
        startPeriodicUpdate()
    }

    fun updateLocation(location: Location?) {
        if (currentLocation != location) {
            currentLocation = location
            cachedAddress = null
            refreshWatermark()
        }
    }

    fun refreshWatermark() {
        updateJob?.cancel()
        updateJob = coroutineScope.launch {
            try {
                watermarkText = buildWatermarkText()
                invalidate()
            } catch (e: Exception) {
            }
        }
    }

    /**
     * 设置手动水印样式（由 WatermarkStyleBottomSheet 调用）
     * 对于 TIME 类型，文字格式为 "TIME:HH:mm\n日期 地址"
     */
    fun setStyledWatermark(style: WatermarkStyleBottomSheet.WatermarkStyle) {
        manualWatermarkText = style.text
        manualWatermarkType = style.type
        watermarkText = style.text
        invalidate()
    }

    fun clearManualWatermark() {
        manualWatermarkText = null
        manualWatermarkType = null
        refreshWatermark()
    }

    private fun startPeriodicUpdate() {
        coroutineScope.launch {
            while (isActive) {
                if (configManager.showDateTime) {
                    // 如果是时间水印，实时刷新时间部分
                    if (manualWatermarkType == WatermarkStyleBottomSheet.WatermarkStyleType.TIME) {
                        val current = manualWatermarkText ?: ""
                        if (current.startsWith("TIME:")) {
                            val timeFormat = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())
                            val newTime = timeFormat.format(java.util.Date())
                            val rest = current.substringAfter("\n", "")
                            manualWatermarkText = "TIME:$newTime\n$rest"
                            watermarkText = manualWatermarkText ?: ""
                            invalidate()
                        }
                    } else if (manualWatermarkText == null) {
                        refreshWatermark()
                    }
                }
                delay(UPDATE_INTERVAL)
            }
        }
    }

    private suspend fun buildWatermarkText(): String = withContext(Dispatchers.Default) {
        if (manualWatermarkText != null) return@withContext manualWatermarkText!!

        val parts = buildList {
            configManager.customWatermarkText?.takeIf { it.isNotEmpty() }?.let { add(it) }
            if (configManager.showDateTime) add(dateFormat.format(Date()))
            if (configManager.showLocation && currentLocation != null) {
                add(buildLocationText(currentLocation!!))
            }
        }
        parts.joinToString("\n")
    }

    private suspend fun buildLocationText(location: Location): String {
        val parts = mutableListOf<String>()
        parts.add("经度: %.6f°".format(location.longitude))
        parts.add("纬度: %.6f°".format(location.latitude))
        if (configManager.fetchElevation && location.hasAltitude()) {
            parts.add("海拔: %.1f米".format(location.altitude))
        }
        if (configManager.showAddress) {
            if (cachedAddress == null) {
                cachedAddress = try {
                    withContext(Dispatchers.IO) {
                        addressResolver.getAddressFromLocation(location.latitude, location.longitude)
                    } ?: "获取失败"
                } catch (e: Exception) {
                    "获取失败"
                }
            }
            parts.add("地址: $cachedAddress")
        }
        return parts.joinToString("\n")
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (watermarkText.isEmpty()) return

        if (manualWatermarkType == WatermarkStyleBottomSheet.WatermarkStyleType.TIME
            && watermarkText.startsWith("TIME:")
        ) {
            drawTimeWatermark(canvas)
        } else {
            drawNormalWatermark(canvas)
        }
    }

    private fun drawTimeWatermark(canvas: Canvas) {
        val raw = watermarkText.removePrefix("TIME:")
        val lines = raw.split("\n", limit = 2)
        val bigTime = lines.getOrElse(0) { "" }.trim()
        val subtitle = lines.getOrElse(1) { "" }.trim()

        val bgAlpha = configManager.watermarkBackgroundAlpha
        backgroundPaint.color = Color.argb(bgAlpha, 0, 0, 0)

        val bigW = timeLargePaint.measureText(bigTime)
        val subW = if (subtitle.isNotEmpty()) timeSmallPaint.measureText(subtitle) else 0f
        val blockW = maxOf(bigW, subW) + BACKGROUND_PADDING * 2
        val bigH = timeLargePaint.textSize
        val subH = if (subtitle.isNotEmpty()) timeSmallPaint.textSize + 8f else 0f
        val blockH = bigH + subH + BACKGROUND_PADDING * 2

        val left = WATERMARK_PADDING.toFloat()
        val top = (height - blockH - BACKGROUND_PADDING).toFloat()
        val rect = RectF(left, top, left + blockW, top + blockH)
        canvas.drawRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, backgroundPaint)

        val textX = left + BACKGROUND_PADDING
        val textY = top + BACKGROUND_PADDING + bigH - timeLargePaint.descent()
        canvas.drawText(bigTime, textX, textY, timeLargePaint)

        if (subtitle.isNotEmpty()) {
            val subY = textY + timeLargePaint.descent() + 8f + timeSmallPaint.textSize - timeSmallPaint.descent()
            canvas.drawText(subtitle, textX, subY, timeSmallPaint)
        }
    }

    private fun drawNormalWatermark(canvas: Canvas) {
        val textSize = configManager.watermarkTextSize
        val textColor = configManager.watermarkColor
        val backgroundAlpha = configManager.watermarkBackgroundAlpha
        val position = configManager.watermarkPosition

        textPaint.apply {
            color = textColor
            this.textSize = textSize
        }
        backgroundPaint.color = Color.argb(backgroundAlpha, 0, 0, 0)

        val textWidth = width - WATERMARK_PADDING * 2
        val textLayout = StaticLayout.Builder.obtain(
            watermarkText, 0, watermarkText.length, textPaint, textWidth
        )
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, LINE_SPACING_MULTIPLIER)
            .setIncludePad(false)
            .build()

        val textHeight = textLayout.height
        val backgroundRect = calculateBackgroundRect(position, textWidth, textHeight)
        canvas.drawRoundRect(backgroundRect, CORNER_RADIUS, CORNER_RADIUS, backgroundPaint)

        val (textX, textY) = calculateTextPosition(position, textWidth, textHeight)
        canvas.save()
        canvas.translate(textX, textY)
        textLayout.draw(canvas)
        canvas.restore()
    }

    private fun calculateBackgroundRect(position: Int, textWidth: Int, textHeight: Int): RectF {
        val totalWidth = textWidth + BACKGROUND_PADDING * 2
        val totalHeight = textHeight + BACKGROUND_PADDING * 2
        return when (position) {
            0 -> RectF(
                WATERMARK_PADDING.toFloat(),
                (height - totalHeight - BACKGROUND_PADDING).toFloat(),
                (WATERMARK_PADDING + totalWidth).toFloat(),
                (height - BACKGROUND_PADDING).toFloat()
            )
            1 -> RectF(
                WATERMARK_PADDING.toFloat(),
                BACKGROUND_PADDING.toFloat(),
                (WATERMARK_PADDING + totalWidth).toFloat(),
                (totalHeight + BACKGROUND_PADDING).toFloat()
            )
            2 -> RectF(
                WATERMARK_PADDING.toFloat(),
                WATERMARK_PADDING.toFloat(),
                (WATERMARK_PADDING + totalWidth).toFloat(),
                (WATERMARK_PADDING + totalHeight).toFloat()
            )
            else -> RectF(
                (width - WATERMARK_PADDING - totalWidth).toFloat(),
                WATERMARK_PADDING.toFloat(),
                (width - WATERMARK_PADDING).toFloat(),
                (WATERMARK_PADDING + totalHeight).toFloat()
            )
        }
    }

    private fun calculateTextPosition(position: Int, textWidth: Int, textHeight: Int): Pair<Float, Float> {
        val textX = when (position) {
            2 -> WATERMARK_PADDING + BACKGROUND_PADDING.toFloat()
            3 -> width - WATERMARK_PADDING - textWidth - BACKGROUND_PADDING.toFloat()
            else -> WATERMARK_PADDING + BACKGROUND_PADDING.toFloat()
        }
        val textY = when (position) {
            1 -> BACKGROUND_PADDING.toFloat()
            2, 3 -> WATERMARK_PADDING + BACKGROUND_PADDING.toFloat()
            else -> height - textHeight - BACKGROUND_PADDING.toFloat()
        }
        return Pair(textX, textY)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        updateJob?.cancel()
        coroutineScope.cancel()
    }
}
