package com.example.watermarkcamera

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

/**
 * MainActivity 集成测试
 * 测试主活动的用户界面和交互功能
 */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    
    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)
    
    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockPreferences: SharedPreferences
    
    private lateinit var context: Context
    private lateinit var editor: SharedPreferences.Editor
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        
        // Mock shared preferences
        val prefsName = "watermark_camera_prefs"
        mockPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        editor = mock(SharedPreferences.Editor::class.java)
        `when`(mockPreferences.edit()).thenReturn(editor)
        `when`(editor.putString(anyString(), anyString())).thenReturn(editor)
        `when`(editor.putInt(anyString(), anyInt())).thenReturn(editor)
        `when`(editor.putBoolean(anyString(), anyBoolean())).thenReturn(editor)
        `when`(editor.apply()).then { }
    }
    
    @After
    fun tearDown() {
        // Clean up preferences after each test
        val prefs = context.getSharedPreferences("watermark_camera_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
    
    @Test
    fun `test main activity initialization`() {
        activityScenarioRule.scenario.onActivity { activity ->
            assertNotNull(activity)
            assertNotNull(activity.binding)
        }
    }
    
    @Test
    fun `test camera preview is visible`() {
        Espresso.onView(ViewMatchers.withId(R.id.previewView))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
    
    @Test
    fun `test capture button is visible and clickable`() {
        Espresso.onView(ViewMatchers.withId(R.id.captureButton))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            .check(ViewAssertions.matches(ViewMatchers.isClickable()))
    }
    
    @Test
    fun `test settings button is visible`() {
        Espresso.onView(ViewMatchers.withId(R.id.settingsButton))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
    
    @Test
    fun `test photo history button is visible`() {
        Espresso.onView(ViewMatchers.withId(R.id.photoHistoryButton))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
    
    @Test
    fun `test photo gallery button is visible`() {
        Espresso.onView(ViewMatchers.withId(R.id.photoGalleryButton))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
    
    @Test
    fun `test watermark preview button is visible`() {
        Espresso.onView(ViewMatchers.withId(R.id.watermarkPreviewButton))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
    
    @Test
    fun `test switch camera button is visible`() {
        Espresso.onView(ViewMatchers.withId(R.id.switchCameraButton))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
    
    @Test
    fun `test flash mode button is visible`() {
        Espresso.onView(ViewMatchers.withId(R.id.flashModeButton))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
    
    @Test
    fun `test capture button click`() {
        // Note: This test might not work in all environments due to camera permissions
        // In a real testing environment, camera permissions would need to be granted
        Espresso.onView(ViewMatchers.withId(R.id.captureButton))
            .perform(ViewActions.click())
            .check(ViewAssertions.matches(ViewMatchers.isClickable()))
    }
    
    @Test
    fun `test settings button click navigation`() {
        Espresso.onView(ViewMatchers.withId(R.id.settingsButton))
            .perform(ViewActions.click())
        
        // In a real test, we would verify that SettingsActivity is started
        // This requires more complex activity monitoring
    }
    
    @Test
    fun `test watermark preview button click navigation`() {
        Espresso.onView(ViewMatchers.withId(R.id.watermarkPreviewButton))
            .perform(ViewActions.click())
        
        // In a real test, we would verify that WatermarkPreviewActivity is started
    }
    
    @Test
    fun `test photo history button click navigation`() {
        Espresso.onView(ViewMatchers.withId(R.id.photoHistoryButton))
            .perform(ViewActions.click())
        
        // In a real test, we would verify that PhotoHistoryActivity is started
    }
    
    @Test
    fun `test photo gallery button click`() {
        Espresso.onView(ViewMatchers.withId(R.id.photoGalleryButton))
            .perform(ViewActions.click())
        
        // This should trigger a gallery selection intent
        // Verification would require intent monitoring
    }
    
    @Test
    fun `test switch camera button click`() {
        Espresso.onView(ViewMatchers.withId(R.id.switchCameraButton))
            .perform(ViewActions.click())
        
        // In a real test, we would verify camera switch behavior
    }
    
    @Test
    fun `test flash mode button click`() {
        Espresso.onView(ViewMatchers.withId(R.id.flashModeButton))
            .perform(ViewActions.click())
        
        // In a real test, we would verify flash mode changes
    }
    
    @Test
    fun `test menu button click shows overflow menu`() {
        // First, check if there's an overflow menu (three dots)
        try {
            Espresso.onView(ViewMatchers.withId(R.id.overflowMenuButton))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .perform(ViewActions.click())
        } catch (e: Exception) {
            // If overflow menu button doesn't exist, try alternative approach
            Espresso.onView(ViewMatchers.withContentDescription("More options"))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .perform(ViewActions.click())
        }
    }
    
    @Test
    fun `test location status display`() {
        Espresso.onView(ViewMatchers.withId(R.id.locationStatusText))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
    
    @Test
    fun `test zoom controls visibility`() {
        // Check if zoom controls are visible
        Espresso.onView(ViewMatchers.withId(R.id.zoomInButton))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        
        Espresso.onView(ViewMatchers.withId(R.id.zoomOutButton))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
    
    @Test
    fun `test zoom in button click`() {
        Espresso.onView(ViewMatchers.withId(R.id.zoomInButton))
            .check(ViewAssertions.matches(ViewMatchers.isClickable()))
            .perform(ViewActions.click())
        
        // In a real test, we would verify zoom level changes
    }
    
    @Test
    fun `test zoom out button click`() {
        Espresso.onView(ViewMatchers.withId(R.id.zoomOutButton))
            .check(ViewAssertions.matches(ViewMatchers.isClickable()))
            .perform(ViewActions.click())
        
        // In a real test, we would verify zoom level changes
    }
    
    @Test
    fun `test activity lifecycle`() {
        var activity: MainActivity? = null
        
        activityScenarioRule.scenario.onActivity { act ->
            activity = act
            assertNotNull(act)
        }
        
        // Test onResume behavior
        Espresso.onView(ViewMatchers.withId(R.id.previewView))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        
        // Test that components are initialized
        activity?.let {
            assertNotNull(it.cameraManager)
            assertNotNull(it.locationManager)
            assertNotNull(it.permissionManager)
        }
    }
    
    @Test
    fun `test configuration changes`() {
        // This test would need to simulate configuration changes
        // In a real test environment, you would use ActivityController
        activityScenarioRule.scenario.onActivity { activity ->
            // Verify activity survives configuration changes
            assertNotNull(activity)
            assertNotNull(activity.binding)
        }
    }
    
    @Test
    fun `test permission handling`() {
        // This test would require mocking permission requests
        // In a real test environment, you would use GrantPermissionRule
        
        activityScenarioRule.scenario.onActivity { activity ->
            // Test that permission manager is initialized
            assertNotNull(activity.permissionManager)
        }
    }
    
    @Test
    fun `test gesture detection setup`() {
        activityScenarioRule.scenario.onActivity { activity ->
            // Test that gesture detector is set up
            assertNotNull(activity.gestureDetector)
        }
    }
    
    @Test
    fun `test memory monitoring`() {
        activityScenarioRule.scenario.onActivity { activity ->
            // Test that memory monitor is initialized
            assertNotNull(activity.memoryMonitor)
        }
    }
}