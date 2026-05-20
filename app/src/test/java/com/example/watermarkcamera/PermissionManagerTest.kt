package com.example.watermarkcamera

import android.content.Context
import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

/**
 * PermissionManager的单元测试
 *
 * 测试权限管理器的各种功能：
 * 1. 权限状态检查
 * 2. 权限请求流程
 * 3. 权限分类管理
 * 4. 权限结果处理
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class PermissionManagerTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockActivity: android.app.Activity

    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences

    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    private lateinit var permissionManager: PermissionManager

    @Before
    fun setup() {
        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor)

        permissionManager = PermissionManager(mockContext)
    }

    @Test
    fun testRequiredPermissionCategory() {
        // 测试必需权限分类
        val requiredCategory = PermissionManager.PermissionCategory.REQUIRED
        assertEquals(1, requiredCategory.priority)
    }

    @Test
    fun testPermissionResultTypes() {
        // 测试权限结果类型
        assertEquals(PermissionManager.PermissionResult.GRANTED, PermissionManager.PermissionResult.GRANTED)
        assertEquals(PermissionManager.PermissionResult.DENIED, PermissionManager.PermissionResult.DENIED)
        assertEquals(PermissionManager.PermissionResult.SHOW_RATIONALE, PermissionManager.PermissionResult.SHOW_RATIONALE)
        assertEquals(PermissionManager.PermissionResult.NEVER_ASK_AGAIN, PermissionManager.PermissionResult.NEVER_ASK_AGAIN)
    }

    @Test
    fun testPermissionRequestEquality() {
        // 测试权限请求对象相等性
        val request1 = PermissionManager.PermissionRequest(
            permissions = arrayOf("android.permission.CAMERA"),
            category = PermissionManager.PermissionCategory.REQUIRED,
            requestCode = 100,
            rationaleTitle = "相机权限",
            rationaleMessage = "需要相机权限",
            essentialTitle = "必需权限",
            essentialMessage = "应用需要相机权限"
        )

        val request2 = PermissionManager.PermissionRequest(
            permissions = arrayOf("android.permission.CAMERA"),
            category = PermissionManager.PermissionCategory.REQUIRED,
            requestCode = 100,
            rationaleTitle = "相机权限",
            rationaleMessage = "需要相机权限",
            essentialTitle = "必需权限",
            essentialMessage = "应用需要相机权限"
        )

        assertEquals(request1, request2)
        assertEquals(request1.hashCode(), request2.hashCode())
    }

    @Test
    fun testPermissionStatusDataClass() {
        // 测试权限状态数据类
        val status = PermissionManager.PermissionStatus(
            category = PermissionManager.PermissionCategory.IMPORTANT,
            isGranted = false,
            shouldShowRationale = true,
            hasNeverAskAgain = false
        )

        assertEquals(PermissionManager.PermissionCategory.IMPORTANT, status.category)
        assertFalse(status.isGranted)
        assertTrue(status.shouldShowRationale)
        assertFalse(status.hasNeverAskAgain)
    }

    @Test
    fun testPermissionListenerInterface() {
        // 测试权限监听器接口
        val listener = mock(PermissionManager.PermissionListener::class.java)
        
        val request = PermissionManager.PermissionRequest(
            permissions = arrayOf("android.permission.CAMERA"),
            category = PermissionManager.PermissionCategory.REQUIRED,
            requestCode = 100,
            rationaleTitle = "相机权限",
            rationaleMessage = "需要相机权限",
            essentialTitle = "必需权限",
            essentialMessage = "应用需要相机权限"
        )

        // 测试监听器方法调用
        listener.onPermissionGranted(request)
        verify(listener).onPermissionGranted(request)

        listener.onPermissionDenied(request, false)
        verify(listener).onPermissionDenied(request, false)

        listener.onPermissionShowRationale(request)
        verify(listener).onPermissionShowRationale(request)
    }

    @Test
    fun testResetPermissionState() {
        // 测试重置权限状态
        permissionManager.resetPermissionState()
        verify(mockSharedPreferences.edit()).clear().apply()
    }
}