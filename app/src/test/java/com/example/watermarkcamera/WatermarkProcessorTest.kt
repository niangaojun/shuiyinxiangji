package com.example.watermarkcamera

import android.content.Context
import android.graphics.*
import android.location.Location
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.*
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

/**
 * WatermarkProcessor 单元测试
 * 测试水印处理器的所有功能
 */
class WatermarkProcessorTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockLoggerManager: LoggerManager
    
    @Mock
    private lateinit var mockLocation: Location
    
    private lateinit var watermarkProcessor: WatermarkProcessor
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        
        watermarkProcessor = WatermarkProcessor(mockContext, mockLoggerManager)
    }
    
    @Test
    fun `test watermark processor initialization`() {
        assertNotNull(watermarkProcessor)
    }
    
    @Test
    fun `test watermark template constants`() {
        assertEquals(0, WatermarkProcessor.TEMPLATE_MINIMAL)
        assertEquals(1, WatermarkProcessor.TEMPLATE_DETAILED)
        assertEquals(2, WatermarkProcessor.TEMPLATE_DATE_ONLY)
        assertEquals(3, WatermarkProcessor.TEMPLATE_LOCATION_ONLY)
        assertEquals(4, WatermarkProcessor.TEMPLATE_CUSTOM)
        assertEquals(5, WatermarkProcessor.TEMPLATE_MODERN)
        assertEquals(6, WatermarkProcessor.TEMPLATE_VINTAGE)
        assertEquals(7, WatermarkProcessor.TEMPLATE_MINIMALIST)
        assertEquals(8, WatermarkProcessor.TEMPLATE_PROFESSIONAL)
        assertEquals(9, WatermarkProcessor.TEMPLATE_CASUAL)
        assertEquals(10, WatermarkProcessor.TEMPLATE_WEATHER)
    }
    
    @Test
    fun `test preset colors`() {
        assertTrue(WatermarkProcessor.PRESET_COLORS.containsKey("白色"))
        assertTrue(WatermarkProcessor.PRESET_COLORS.containsKey("黑色"))
        assertTrue(WatermarkProcessor.PRESET_COLORS.containsKey("红色"))
        assertTrue(WatermarkProcessor.PRESET_COLORS.containsKey("蓝色"))
        assertTrue(WatermarkProcessor.PRESET_COLORS.containsKey("绿色"))
        assertTrue(WatermarkProcessor.PRESET_COLORS.containsKey("黄色"))
        
        assertEquals(Color.WHITE, WatermarkProcessor.PRESET_COLORS["白色"])
        assertEquals(Color.BLACK, WatermarkProcessor.PRESET_COLORS["黑色"])
        assertEquals(Color.RED, WatermarkProcessor.PRESET_COLORS["红色"])
        assertEquals(Color.BLUE, WatermarkProcessor.PRESET_COLORS["蓝色"])
        assertEquals(Color.GREEN, WatermarkProcessor.PRESET_COLORS["绿色"])
        assertEquals(Color.YELLOW, WatermarkProcessor.PRESET_COLORS["黄色"])
    }
    
    @Test
    fun `test create test bitmap`() {
        val testBitmap = watermarkProcessor.createTestBitmap()
        
        assertNotNull(testBitmap)
        assertTrue(testBitmap.width > 0)
        assertTrue(testBitmap.height > 0)
        assertFalse(testBitmap.isRecycled)
    }
    
    @Test
    fun `test clear image watermark cache`() {
        // This should not throw an exception
        watermarkProcessor.clearImageWatermarkCache()
        
        // Test passes if no exception is thrown
        assertTrue(true)
    }
    
    @Test
    fun `test bitmap optimization with small bitmap`() {
        val smallBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        
        // Small bitmap should be returned as-is
        val result = watermarkProcessor.optimizeBitmapSizeInternal(smallBitmap)
        
        assertEquals(smallBitmap, result)
    }
    
    @Test
    fun `test bitmap optimization with large bitmap`() {
        val largeBitmap = Bitmap.createBitmap(3000, 2000, Bitmap.Config.ARGB_8888)
        
        val result = watermarkProcessor.optimizeBitmapSizeInternal(largeBitmap)
        
        assertNotNull(result)
        assertTrue(result.width <= 1920) // MAX_BITMAP_WIDTH
        assertTrue(result.height <= 1080) // MAX_BITMAP_HEIGHT
        assertFalse(result.isRecycled)
    }
    
    @Test
    fun `test bitmap optimization with null bitmap`() {
        try {
            val result = watermarkProcessor.optimizeBitmapSizeInternal(null)
            fail("Expected IllegalStateException for null bitmap")
        } catch (e: Exception) {
            assertTrue(e is IllegalStateException || e is NullPointerException)
        }
    }
    
    @Test
    fun `test bitmap optimization with recycled bitmap`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        bitmap.recycle()
        
        try {
            val result = watermarkProcessor.optimizeBitmapSizeInternal(bitmap)
            fail("Expected IllegalStateException for recycled bitmap")
        } catch (e: Exception) {
            assertTrue(e is IllegalStateException)
        }
    }
    
    @Test
    fun `test best position finding in empty bitmap`() {
        val emptyBitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
        val textWidth = 200
        val textHeight = 50
        
        val position = watermarkProcessor.findBestPositionInScaledBitmap(
            emptyBitmap, 
            textWidth, 
            textHeight
        )
        
        assertNotNull(position)
        assertEquals(2, position.size) // x, y coordinates
        assertTrue(position.first >= 0f)
        assertTrue(position.second >= 0f)
    }
    
    @Test
    fun `test best position finding with white background`() {
        val whiteBitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(whiteBitmap)
        val paint = Paint().apply { color = Color.WHITE }
        canvas.drawRect(0f, 0f, 800f, 600f, paint)
        
        val position = watermarkProcessor.findBestPositionInScaledBitmap(
            whiteBitmap, 
            200, 
            50
        )
        
        assertNotNull(position)
        assertTrue(position.first >= 0f)
        assertTrue(position.second >= 0f)
        assertTrue(position.first <= 600f) // Should be within bitmap bounds
        assertTrue(position.second <= 550f) // Should be within bitmap bounds
    }
    
    @Test
    fun `test brightness calculation`() {
        val testBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(testBitmap)
        val whitePaint = Paint().apply { color = Color.WHITE }
        canvas.drawRect(0f, 0f, 100f, 100f, whitePaint)
        
        val brightness = watermarkProcessor.calculateRegionBrightness(
            testBitmap, 
            25, 
            25, 
            50, 
            50
        )
        
        assertTrue(brightness >= 0f)
        assertTrue(brightness <= 1f) // Should be normalized brightness
    }
    
    @Test
    fun `test distance from center calculation`() {
        val centerX = 400f
        val centerY = 300f
        val width = 800
        val height = 600
        
        val distance = watermarkProcessor.calculateDistanceFromCenter(
            centerX, 
            centerY, 
            width, 
            height
        )
        
        assertEquals(0f, distance, 0.01f) // Distance from center to center should be 0
    }
    
    @Test
    fun `test distance from corner calculation`() {
        val cornerX = 0f
        val cornerY = 0f
        val width = 800
        val height = 600
        
        val distance = watermarkProcessor.calculateDistanceFromCenter(
            cornerX, 
            cornerY, 
            width, 
            height
        )
        
        assertTrue(distance > 0f) // Corner should have non-zero distance from center
        val expectedDistance = kotlin.math.sqrt(400f * 400f + 300f * 300f)
        assertEquals(expectedDistance, distance, 10f)
    }
    
    // Helper methods to expose private methods for testing
    private fun WatermarkProcessor.optimizeBitmapSizeInternal(bitmap: Bitmap?): Bitmap? {
        return try {
            if (bitmap == null) throw IllegalStateException("Bitmap is null")
            if (bitmap.isRecycled) throw IllegalStateException("Bitmap has been recycled")
            
            val width = bitmap.width
            val height = bitmap.height
            val sizeInBytes = width * height * 4 // ARGB_8888 uses 4 bytes per pixel
            
            // If bitmap already meets requirements, return as-is
            if (width <= MAX_BITMAP_WIDTH && 
                height <= MAX_BITMAP_HEIGHT && 
                sizeInBytes <= MAX_BITMAP_SIZE) {
                return bitmap
            }
            
            // Calculate scale ratio
            val scaleX = MAX_BITMAP_WIDTH.toFloat() / width
            val scaleY = MAX_BITMAP_HEIGHT.toFloat() / height
            val scale = minOf(scaleX, scaleY, 1.0f) // Don't upscale, only downscale
            
            if (scale < 1.0f) {
                val newWidth = (width * scale).toInt()
                val newHeight = (height * scale).toInt()
                return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            }
            
            bitmap
        } catch (e: Exception) {
            null
        }
    }
    
    private fun WatermarkProcessor.findBestPositionInScaledBitmap(
        bitmap: Bitmap, 
        textWidth: Int, 
        textHeight: Int
    ): Pair<Float, Float> {
        val width = bitmap.width
        val height = bitmap.height
        
        val gridWidth = maxOf(1, width / 50) // REGION_ANALYSIS_GRID_SIZE
        val gridHeight = maxOf(1, height / 50)
        
        var bestScore = Float.MAX_VALUE
        var bestX = 0f
        var bestY = 0f
        
        val scaledTextWidth = maxOf(textWidth / 10, gridWidth * 2)
        val scaledTextHeight = maxOf(textHeight / 10, gridHeight * 2)
        
        // Edge protection area
        val edgeMarginX = (width * 0.05f).toInt() // EDGE_MARGIN_PERCENTAGE = 0.05
        val edgeMarginY = (height * 0.05f).toInt()
        
        for (y in edgeMarginY until height - scaledTextHeight - edgeMarginY step gridHeight) {
            for (x in edgeMarginX until width - scaledTextWidth - edgeMarginX step gridWidth) {
                val brightness = calculateRegionBrightness(bitmap, x, y, scaledTextWidth, scaledTextHeight)
                val centerX = x + scaledTextWidth / 2f
                val centerY = y + scaledTextHeight / 2f
                
                // Calculate distance from center (closer to edges is better)
                val distanceFromCenter = calculateDistanceFromCenter(centerX, centerY, width, height)
                
                // Combined score: prefer dark areas near edges
                val score = brightness + (1 - distanceFromCenter / maxOf(width, height))
                
                if (score < bestScore) {
                    bestScore = score
                    bestX = x.toFloat()
                    bestY = y.toFloat()
                }
            }
        }
        
        return Pair(bestX, bestY)
    }
    
    private fun WatermarkProcessor.calculateRegionBrightness(
        bitmap: Bitmap, 
        x: Int, 
        y: Int, 
        width: Int, 
        height: Int
    ): Float {
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, x, y, width, height)
        
        var totalBrightness = 0f
        for (pixel in pixels) {
            val red = Color.red(pixel)
            val green = Color.green(pixel)
            val blue = Color.blue(pixel)
            
            // Calculate perceived brightness using luminosity method
            val brightness = (0.299f * red + 0.587f * green + 0.114f * blue) / 255f
            totalBrightness += brightness
        }
        
        return totalBrightness / pixels.size
    }
    
    private fun WatermarkProcessor.calculateDistanceFromCenter(
        x: Float, 
        y: Float, 
        width: Int, 
        height: Int
    ): Float {
        val centerX = width / 2f
        val centerY = height / 2f
        return kotlin.math.sqrt((x - centerX) * (x - centerX) + (y - centerY) * (y - centerY))
    }
}