package com.example.watermarkcamera

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.*
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

/**
 * ConfigManager 单元测试
 * 测试配置管理器的所有功能
 */
class ConfigManagerTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockPreferences: SharedPreferences
    
    private lateinit var configManager: ConfigManager
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        
        // Mock SharedPreferences
        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPreferences)
        
        configManager = ConfigManager(mockContext)
    }
    
    @Test
    fun `test default watermark text size`() {
        `when`(mockPreferences.getInt(anyString(), anyInt())).thenReturn(72)
        
        assertEquals(72f, configManager.watermarkTextSize, 0.01f)
    }
    
    @Test
    fun `test watermark text size get and set`() {
        val editor = mock(SharedPreferences.Editor::class.java)
        `when`(mockPreferences.edit()).thenReturn(editor)
        `when`(editor.putInt(anyString(), anyInt())).thenReturn(editor)
        `when`(editor.apply()).then { }
        
        configManager.watermarkTextSize = 100f
        
        verify(editor).putInt(ConfigManager.Companion.WATERMARK_TEXT_SIZE, 100)
        verify(editor).apply()
    }
    
    @Test
    fun `test default watermark color`() {
        `when`(mockPreferences.getInt(anyString(), anyInt())).thenReturn(Color.WHITE)
        
        assertEquals(Color.WHITE, configManager.watermarkColor)
    }
    
    @Test
    fun `test watermark color get and set`() {
        val editor = mock(SharedPreferences.Editor::class.java)
        `when`(mockPreferences.edit()).thenReturn(editor)
        `when`(editor.putInt(anyString(), anyInt())).thenReturn(editor)
        `when`(editor.apply()).then { }
        
        configManager.watermarkColor = Color.RED
        
        verify(editor).putInt(ConfigManager.Companion.WATERMARK_COLOR, Color.RED)
        verify(editor).apply()
    }
    
    @Test
    fun `test default watermark position`() {
        `when`(mockPreferences.getInt(anyString(), anyInt())).thenReturn(1)
        
        assertEquals(1, configManager.watermarkPosition)
    }
    
    @Test
    fun `test watermark position get and set`() {
        val editor = mock(SharedPreferences.Editor::class.java)
        `when`(mockPreferences.edit()).thenReturn(editor)
        `when`(editor.putInt(anyString(), anyInt())).thenReturn(editor)
        `when`(editor.apply()).then { }
        
        configManager.watermarkPosition = 3
        
        verify(editor).putInt(ConfigManager.Companion.WATERMARK_POSITION, 3)
        verify(editor).apply()
    }
    
    @Test
    fun `test default show location setting`() {
        `when`(mockPreferences.getBoolean(anyString(), anyBoolean())).thenReturn(true)
        
        assertTrue(configManager.showLocation)
    }
    
    @Test
    fun `test show location get and set`() {
        val editor = mock(SharedPreferences.Editor::class.java)
        `when`(mockPreferences.edit()).thenReturn(editor)
        `when`(editor.putBoolean(anyString(), anyBoolean())).thenReturn(editor)
        `when`(editor.apply()).then { }
        
        configManager.showLocation = false
        
        verify(editor).putBoolean(ConfigManager.Companion.SHOW_LOCATION, false)
        verify(editor).apply()
    }
    
    @Test
    fun `test default show date time setting`() {
        `when`(mockPreferences.getBoolean(anyString(), anyBoolean())).thenReturn(true)
        
        assertTrue(configManager.showDateTime)
    }
    
    @Test
    fun `test show date time get and set`() {
        val editor = mock(SharedPreferences.Editor::class.java)
        `when`(mockPreferences.edit()).thenReturn(editor)
        `when`(editor.putBoolean(anyString(), anyBoolean())).thenReturn(editor)
        `when`(editor.apply()).then { }
        
        configManager.showDateTime = false
        
        verify(editor).putBoolean(ConfigManager.Companion.SHOW_DATE_TIME, false)
        verify(editor).apply()
    }
    
    @Test
    fun `test custom watermark text default`() {
        `when`(mockPreferences.getString(anyString(), any())).thenReturn("")
        
        assertEquals("", configManager.customWatermarkText)
    }
    
    @Test
    fun `test custom watermark text get and set`() {
        val editor = mock(SharedPreferences.Editor::class.java)
        `when`(mockPreferences.edit()).thenReturn(editor)
        `when`(editor.putString(anyString(), anyString())).thenReturn(editor)
        `when`(editor.apply()).then { }
        
        configManager.customWatermarkText = "Test Watermark"
        
        verify(editor).putString(ConfigManager.Companion.CUSTOM_WATERMARK_TEXT, "Test Watermark")
        verify(editor).apply()
    }
    
    @Test
    fun `test default image quality`() {
        `when`(mockPreferences.getInt(anyString(), anyInt())).thenReturn(80)
        
        assertEquals(80, configManager.imageQuality)
    }
    
    @Test
    fun `test image quality get and set`() {
        val editor = mock(SharedPreferences.Editor::class.java)
        `when`(mockPreferences.edit()).thenReturn(editor)
        `when`(editor.putInt(anyString(), anyInt())).thenReturn(editor)
        `when`(editor.apply()).then { }
        
        configManager.imageQuality = 95
        
        verify(editor).putInt(ConfigManager.Companion.IMAGE_QUALITY, 95)
        verify(editor).apply()
    }
    
    @Test
    fun `test default font style`() {
        `when`(mockPreferences.getInt(anyString(), anyInt())).thenReturn(0)
        
        assertEquals(0, configManager.fontStyle)
    }
    
    @Test
    fun `test font style get and set`() {
        val editor = mock(SharedPreferences.Editor::class.java)
        `when`(mockPreferences.edit()).thenReturn(editor)
        `when`(editor.putInt(anyString(), anyInt())).thenReturn(editor)
        `when`(editor.apply()).then { }
        
        configManager.fontStyle = 1
        
        verify(editor).putInt(ConfigManager.Companion.FONT_STYLE, 1)
        verify(editor).apply()
    }
    
    @Test
    fun `test default font bold setting`() {
        `when`(mockPreferences.getBoolean(anyString(), anyBoolean())).thenReturn(false)
        
        assertFalse(configManager.fontBold)
    }
    
    @Test
    fun `test font bold get and set`() {
        val editor = mock(SharedPreferences.Editor::class.java)
        `when`(mockPreferences.edit()).thenReturn(editor)
        `when`(editor.putBoolean(anyString(), anyBoolean())).thenReturn(editor)
        `when`(editor.apply()).then { }
        
        configManager.fontBold = true
        
        verify(editor).putBoolean(ConfigManager.Companion.FONT_BOLD, true)
        verify(editor).apply()
    }
    
    @Test
    fun `test default font italic setting`() {
        `when`(mockPreferences.getBoolean(anyString(), anyBoolean())).thenReturn(false)
        
        assertFalse(configManager.fontItalic)
    }
    
    @Test
    fun `test font italic get and set`() {
        val editor = mock(SharedPreferences.Editor::class.java)
        `when`(mockPreferences.edit()).thenReturn(editor)
        `when`(editor.putBoolean(anyString(), anyBoolean())).thenReturn(editor)
        `when`(editor.apply()).then { }
        
        configManager.fontItalic = true
        
        verify(editor).putBoolean(ConfigManager.Companion.FONT_ITALIC, true)
        verify(editor).apply()
    }
    
    @Test
    fun `test getFontTypeface with default settings`() {
        `when`(mockPreferences.getInt(eq(ConfigManager.Companion.FONT_STYLE), anyInt())).thenReturn(0)
        `when`(mockPreferences.getBoolean(eq(ConfigManager.Companion.FONT_BOLD), anyBoolean())).thenReturn(false)
        `when`(mockPreferences.getBoolean(eq(ConfigManager.Companion.FONT_ITALIC), anyBoolean())).thenReturn(false)
        
        val typeface = configManager.getFontTypeface()
        
        assertNotNull(typeface)
        assertEquals(Typeface.DEFAULT, typeface)
    }
    
    @Test
    fun `test getFontTypeface with bold style`() {
        `when`(mockPreferences.getInt(eq(ConfigManager.Companion.FONT_STYLE), anyInt())).thenReturn(0)
        `when`(mockPreferences.getBoolean(eq(ConfigManager.Companion.FONT_BOLD), anyBoolean())).thenReturn(true)
        `when`(mockPreferences.getBoolean(eq(ConfigManager.Companion.FONT_ITALIC), anyBoolean())).thenReturn(false)
        
        val typeface = configManager.getFontTypeface()
        
        assertNotNull(typeface)
    }
    
    @Test
    fun `test getFontTypeface with italic style`() {
        `when`(mockPreferences.getInt(eq(ConfigManager.Companion.FONT_STYLE), anyInt())).thenReturn(0)
        `when`(mockPreferences.getBoolean(eq(ConfigManager.Companion.FONT_BOLD), anyBoolean())).thenReturn(false)
        `when`(mockPreferences.getBoolean(eq(ConfigManager.Companion.FONT_ITALIC), anyBoolean())).thenReturn(true)
        
        val typeface = configManager.getFontTypeface()
        
        assertNotNull(typeface)
    }
    
    @Test
    fun `test getFontTypeface with bold italic style`() {
        `when`(mockPreferences.getInt(eq(ConfigManager.Companion.FONT_STYLE), anyInt())).thenReturn(0)
        `when`(mockPreferences.getBoolean(eq(ConfigManager.Companion.FONT_BOLD), anyBoolean())).thenReturn(true)
        `when`(mockPreferences.getBoolean(eq(ConfigManager.Companion.FONT_ITALIC), anyBoolean())).thenReturn(true)
        
        val typeface = configManager.getFontTypeface()
        
        assertNotNull(typeface)
    }
    
    @Test
    fun `test getFontTypeface with serif style`() {
        `when`(mockPreferences.getInt(eq(ConfigManager.Companion.FONT_STYLE), anyInt())).thenReturn(1)
        `when`(mockPreferences.getBoolean(eq(ConfigManager.Companion.FONT_BOLD), anyBoolean())).thenReturn(false)
        `when`(mockPreferences.getBoolean(eq(ConfigManager.Companion.FONT_ITALIC), anyBoolean())).thenReturn(false)
        
        val typeface = configManager.getFontTypeface()
        
        assertNotNull(typeface)
    }
    
    @Test
    fun `test getFontTypeface with sans serif style`() {
        `when`(mockPreferences.getInt(eq(ConfigManager.Companion.FONT_STYLE), anyInt())).thenReturn(2)
        `when`(mockPreferences.getBoolean(eq(ConfigManager.Companion.FONT_BOLD), anyBoolean())).thenReturn(false)
        `when`(mockPreferences.getBoolean(eq(ConfigManager.Companion.FONT_ITALIC), anyBoolean())).thenReturn(false)
        
        val typeface = configManager.getFontTypeface()
        
        assertNotNull(typeface)
    }
    
    @Test
    fun `test reset to defaults`() {
        val editor = mock(SharedPreferences.Editor::class.java)
        `when`(mockPreferences.edit()).thenReturn(editor)
        `when`(editor.clear()).thenReturn(editor)
        `when`(editor.apply()).then { }
        
        configManager.resetToDefaults()
        
        verify(editor).clear()
        verify(editor).apply()
    }
}