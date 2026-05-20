package com.example.watermarkcamera.util

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.util.Log
import kotlinx.coroutines.*

/**
 * 内存监控类
 * 负责监控应用内存使用情况，提供内存压力检测和清理功能
 */
class MemoryMonitor {
    
    companion object {
        private const val TAG = "MemoryMonitor"
        private const val LOW_MEMORY_THRESHOLD = 0.8f  // 内存使用率阈值
        private const val MONITOR_INTERVAL = 5000L     // 监控间隔（毫秒）
    }
    
    private var monitoringJob: Job? = null
    private var isMonitoring = false
    private var memoryPressureCallback: ((Boolean) -> Unit)? = null
    
    /**
     * 开始内存监控
     */
    fun startMonitoring() {
        if (isMonitoring) return
        
        isMonitoring = true
        monitoringJob = CoroutineScope(Dispatchers.IO).launch {
            while (isMonitoring) {
                try {
                    val memoryInfo = getMemoryInfo()
                    val isUnderPressure = isMemoryUnderPressure(memoryInfo)
                    
                    // 如果内存压力状态变化，触发回调
                    memoryPressureCallback?.invoke(isUnderPressure)
                    
                    // 如果内存严重不足，尝试清理
                    if (isMemoryCritical(memoryInfo)) {
                        System.gc() // 建议垃圾回收
                        Log.w(TAG, "内存严重不足，已建议垃圾回收")
                    }
                    
                    delay(MONITOR_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "内存监控异常: ${e.message}")
                }
            }
        }
    }
    
    /**
     * 停止内存监控
     */
    fun stopMonitoring() {
        isMonitoring = false
        monitoringJob?.cancel()
        monitoringJob = null
    }
    
    /**
     * 设置内存压力回调
     */
    fun setMemoryPressureCallback(callback: (Boolean) -> Unit) {
        this.memoryPressureCallback = callback
    }
    
    /**
     * 获取内存信息
     */
    private fun getMemoryInfo(): MemoryInfo {
        val memoryInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(memoryInfo)
        
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        
        return MemoryInfo(
            usedMemory = usedMemory,
            maxMemory = maxMemory,
            memoryInfo = memoryInfo
        )
    }
    
    /**
     * 判断是否处于内存压力状态
     */
    private fun isMemoryUnderPressure(memoryInfo: MemoryInfo): Boolean {
        val usageRatio = memoryInfo.usedMemory.toFloat() / memoryInfo.maxMemory
        return usageRatio > LOW_MEMORY_THRESHOLD
    }
    
    /**
     * 判断是否处于严重内存压力状态
     */
    private fun isMemoryCritical(memoryInfo: MemoryInfo): Boolean {
        val usageRatio = memoryInfo.usedMemory.toFloat() / memoryInfo.maxMemory
        return usageRatio > 0.9f
    }
    
    /**
     * 手动触发垃圾回收
     */
    fun triggerGC() {
        System.gc()
        Log.i(TAG, "手动触发垃圾回收")
    }
    
    /**
     * 获取内存使用率
     */
    fun getMemoryUsageRatio(): Float {
        val memoryInfo = getMemoryInfo()
        return memoryInfo.usedMemory.toFloat() / memoryInfo.maxMemory
    }
    
    /**
     * 内存信息数据类
     */
    data class MemoryInfo(
        val usedMemory: Long,
        val maxMemory: Long,
        val memoryInfo: Debug.MemoryInfo
    )
    
    /**
     * 释放资源
     */
    fun release() {
        stopMonitoring()
        memoryPressureCallback = null
    }
}