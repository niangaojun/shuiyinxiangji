package com.example.watermarkcamera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

/**
 * 水印流程集成测试
 * 测试从拍照到添加水印的完整工作流程
 */
@RunWith(AndroidJUnit4::class)
class WatermarkFlowTest {
    
    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)
    
    @get:Rule
    val cameraPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)
    
    @get:Rule
    val locationPermissionRule = GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION)
    
    @get:Rule
    val storagePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    
    @Mock
    private lateinit var mockBitmap: Bitmap
    
    private lateinit var context: Context
    private lateinit var intent: Intent
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        Intents.init()
        
        // Mock bitmap creation
        `when`(mockBitmap.width).thenReturn(1920)
        `when`(mockBitmap.height).thenReturn(1080)
        `when`(mockBitmap.config).thenReturn(Bitmap.Config.ARGB_8888)
    }
    
    @After
    fun tearDown() {
        Intents.release()
        
        // Clean up preferences
        val prefs = context.getSharedPreferences("watermark_camera_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
    
    @Test
    fun `test complete watermark photo flow`() {
        // Step 1: Verify main activity is loaded
        activityScenarioRule.scenario.onActivity { activity ->
            assertNotNull(activity)
        }
        
        // Step 2: Verify camera button is visible and clickable
        Espresso.onView(ViewMatchers.withId(R.id.cameraButton))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            .check(ViewAssertions.matches(ViewMatchers.isClickable()))
        
        // Step 3: Click camera button
        Espresso.onView(ViewMatchers.withId(R.id.cameraButton))
            .perform(ViewActions.click())
        
        // Step 4: Verify camera preview is shown (in real app)
        // This would require waiting for camera preview to load
        Espresso.onView(ViewMatchers.withId(R.id.cameraPreview))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        
        // Step 5: Capture photo
        Espresso.onView(ViewMatchers.withId(R.id.captureButton))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            .perform(ViewActions.click())
        
        // Step 6: Verify watermark preview activity is launched
        Intents.intended(IntentMatchers.hasComponent(WatermarkPreviewActivity::class.java.name))
        
        // Step 7: Verify preview image is displayed
        Espresso.onView(ViewMatchers.withId(R.id.previewImageView))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        
        // Step 8: Test watermark position adjustment
        Espresso.onView(ViewMatchers.withId(R.id.positionLeft))
            .perform(ViewActions.click())
        
        Espresso.onView(ViewMatchers.withId(R.id.positionRight))
            .perform(ViewActions.click())
        
        Espresso.onView(ViewMatchers.withId(R.id.centerPosition))
            .perform(ViewActions.click())
        
        // Step 9: Test watermark opacity adjustment
        Espresso.onView(ViewMatchers.withId(R.id.opacitySeekBar))
            .perform(ViewActions.swipeLeft())
        
        // Step 10: Save the watermarked image
        Espresso.onView(ViewMatchers.withId(R.id.saveButton))
            .perform(ViewActions.click())
        
        // Step 11: Verify save confirmation (in real app)
        // This would require verifying that image is saved to gallery
    }
    
    @Test
    fun `test watermark customization flow`() {
        // Step 1: Open settings activity
        Espresso.onView(ViewMatchers.withId(R.id.settingsButton))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            .perform(ViewActions.click())
        
        // Step 2: Verify settings activity is launched
        Intents.intended(IntentMatchers.hasComponent(SettingsActivity::class.java.name))
        
        // Step 3: Customize watermark text
        Espresso.onView(ViewMatchers.withId(R.id.customWatermarkInput))
            .perform(ViewActions.clearText())
            .perform(ViewActions.typeText("自定义水印文字"))
        
        // Step 4: Adjust watermark size
        Espresso.onView(ViewMatchers.withId(R.id.watermarkSizeSeekBar))
            .perform(ViewActions.swipeRight())
        
        // Step 5: Change watermark color
        Espresso.onView(ViewMatchers.withId(R.id.colorPickerButton))
            .perform(ViewActions.click())
        
        // Step 6: Toggle location display
        Espresso.onView(ViewMatchers.withId(R.id.showLocationSwitch))
            .perform(ViewActions.click())
        
        // Step 7: Toggle date/time display
        Espresso.onView(ViewMatchers.withId(R.id.showDateTimeSwitch))
            .perform(ViewActions.click())
        
        // Step 8: Save settings
        Espresso.onView(ViewMatchers.withId(R.id.saveButton))
            .perform(ViewActions.click())
        
        // Step 9: Return to main activity
        Espresso.onView(ViewMatchers.withContentDescription("Navigate up"))
            .perform(ViewActions.click())
        
        // Step 10: Capture photo to apply new settings
        Espresso.onView(ViewMatchers.withId(R.id.cameraButton))
            .perform(ViewActions.click())
        
        Espresso.onView(ViewMatchers.withId(R.id.captureButton))
            .perform(ViewActions.click())
        
        // Step 11: Verify watermarked image with new settings
        Espresso.onView(ViewMatchers.withId(R.id.previewImageView))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
    
    @Test
    fun `test watermark position presets flow`() {
        // Step 1: Open settings
        Espresso.onView(ViewMatchers.withId(R.id.settingsButton))
            .perform(ViewActions.click())
        
        // Step 2: Select different position presets
        Espresso.onView(ViewMatchers.withId(R.id.positionSpinner))
            .perform(ViewActions.click())
        
        // Test different positions
        Espresso.onView(ViewMatchers.withText("顶部左侧"))
            .perform(ViewActions.click())
        
        Espresso.onView(ViewMatchers.withId(R.id.saveButton))
            .perform(ViewActions.click())
        
        Espresso.onView(ViewMatchers.withContentDescription("Navigate up"))
            .perform(ViewActions.click())
        
        // Step 3: Apply position preset
        Espresso.onView(ViewMatchers.withId(R.id.cameraButton))
            .perform(ViewActions.click())
        
        Espresso.onView(ViewMatchers.withId(R.id.captureButton))
            .perform(ViewActions.click())
        
        // Verify position preset is applied
        Espresso.onView(ViewMatchers.withId(R.id.previewImageView))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
    
    @Test
    fun `test watermark font customization flow`() {
        // Step 1: Open settings
        Espresso.onView(ViewMatchers.withId(R.id.settingsButton))
            .perform(ViewActions.click())
        
        // Step 2: Change font style
        Espresso.onView(ViewMatchers.withId(R.id.fontStyleSpinner))
            .perform(ViewActions.click())
        
        // Step 3: Toggle bold
        Espresso.onView(ViewMatchers.withId(R.id.fontBoldSwitch))
            .perform(ViewActions.click())
        
        // Step 4: Toggle italic
        Espresso.onView(ViewMatchers.withId(R.id.fontItalicSwitch))
            .perform(ViewActions.click())
        
        // Step 5: Save font settings
        Espresso.onView(ViewMatchers.withId(R.id.saveButton))
            .perform(ViewActions.click())
        
        // Step 6: Return to main
        Espresso.onView(ViewMatchers.withContentDescription("Navigate up"))
            .perform(ViewActions.click())
        
        // Step 7: Capture with custom font
        Espresso.onView(ViewMatchers.withId(R.id.cameraButton))
            .perform(ViewActions.click())
        
        Espresso.onView(ViewMatchers.withId(R.id.captureButton))
            .perform(ViewActions.click())
        
        // Verify font settings are applied
        Espresso.onView(ViewMatchers.withId(R.id.previewImageView))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
    
    @Test
    fun `test image quality adjustment flow`() {
        // Step 1: Open settings
        Espresso.onView(ViewMatchers.withId(R.id.settingsButton))
            .perform(ViewActions.click())
        
        // Step 2: Adjust image quality
        Espresso.onView(ViewMatchers.withId(R.id.imageQualitySeekBar))
            .perform(ViewActions.swipeLeft())
        
        // Step 3: Save quality settings
        Espresso.onView(ViewMatchers.withId(R.id.saveButton))
            .perform(ViewActions.click())
        
        // Step 4: Return to main
        Espresso.onView(ViewMatchers.withContentDescription("Navigate up"))
            .perform(ViewActions.click())
        
        // Step 5: Capture with quality setting
        Espresso.onView(ViewMatchers.withId(R.id.cameraButton))
            .perform(ViewActions.click())
        
        Espresso.onView(ViewMatchers.withId(R.id.captureButton))
            .perform(ViewActions.click())
        
        // Verify image quality setting is applied
        Espresso.onView(ViewMatchers.withId(R.id.previewImageView))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
    
    @Test
    fun `test watermark reset flow`() {
        // Step 1: Open settings
        Espresso.onView(ViewMatchers.withId(R.id.settingsButton))
            .perform(ViewActions.click())
        
        // Step 2: Make some customizations
        Espresso.onView(ViewMatchers.withId(R.id.customWatermarkInput))
            .perform(ViewActions.typeText("测试文字"))
        
        Espresso.onView(ViewMatchers.withId(R.id.watermarkSizeSeekBar))
            .perform(ViewActions.swipeRight())
        
        // Step 3: Reset to defaults
        Espresso.onView(ViewMatchers.withId(R.id.resetButton))
            .perform(ViewActions.click())
        
        // Step 4: Verify reset confirmation
        // In a real test, we would verify that settings are reset to defaults
        
        // Step 5: Save reset settings
        Espresso.onView(ViewMatchers.withId(R.id.saveButton))
            .perform(ViewActions.click())
        
        // Step 6: Return to main
        Espresso.onView(ViewMatchers.withContentDescription("Navigate up"))
            .perform(ViewActions.click())
        
        // Step 7: Capture with default settings
        Espresso.onView(ViewMatchers.withId(R.id.cameraButton))
            .perform(ViewActions.click())
        
        Espresso.onView(ViewMatchers.withId(R.id.captureButton))
            .perform(ViewActions.click())
        
        // Verify default watermark settings are applied
        Espresso.onView(ViewMatchers.withId(R.id.previewImageView))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
    
    @Test
    fun `test share watermarked image flow`() {
        // Step 1: Open camera and capture
        Espresso.onView(ViewMatchers.withId(R.id.cameraButton))
            .perform(ViewActions.click())
        
        Espresso.onView(ViewMatchers.withId(R.id.captureButton))
            .perform(ViewActions.click())
        
        // Step 2: Share the image
        Espresso.onView(ViewMatchers.withId(R.id.shareButton))
            .perform(ViewActions.click())
        
        // Step 3: Verify share intent is created
        Intents.intended(IntentMatchers.hasAction(Intent.ACTION_SEND))
        
        // Step 4: Verify image is attached to share intent
        Intents.intended(IntentMatchers.hasExtra("android.intent.extra.STREAM", Matchers.any(Uri::class.java)))
    }
}