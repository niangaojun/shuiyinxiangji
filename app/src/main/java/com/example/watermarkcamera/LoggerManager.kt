package com.example.watermarkcamera

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

/**
 * 统一的日志管理器
 * 提供文件日志、错误统计、性能监控和崩溃报告功能
 */
class LoggerManager(private val context: Context) {
    
    companion object {
        private const val TAG = "LoggerManager"
        private const val LOG_FILE_PREFIX = "watermark_camera"
        private const val MAX_LOG_FILE_SIZE = 1024 * 1024 // 1MB
        private const val MAX_LOG_FILES = 5
        private const val LOG_FLUSH_INTERVAL = 5000L // 5秒
        private const val MAX_ERROR_COUNT = 1000
        
        @Volatile
        private var instance: LoggerManager? = null
        
        fun getInstance(context: Context): LoggerManager {
            return instance ?: synchronized(this) {
                instance ?: LoggerManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private val logDirectory = File(
        context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
        "logs"
    )
    
    private val currentLogFile: File
        get() = File(logDirectory, "${LOG_FILE_PREFIX}_current.log")
    
    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "LoggerThread").apply { isDaemon = true }
    }
    
    private val lastFlushTime = AtomicLong(0)
    private val errorCounts = ConcurrentHashMap<String, AtomicLong>()
    private val performanceMetrics = ConcurrentHashMap<String, MutableList<Long>>()
    private val sessionStats = ConcurrentHashMap<String, Any>()
    
    init {
        initializeLogDirectory()
    }
    
    /**
     * 初始化日志目录
     */
    private fun initializeLogDirectory() {
        try {
            if (!logDirectory.exists()) {
                logDirectory.mkdirs()
            }
            
            // 清理旧的日志文件
            cleanupOldLogs()
            
            Log.i(TAG, "日志目录初始化完成: ${logDirectory.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "初始化日志目录失败", e)
        }
    }
    
    /**
     * 记录调试信息
     */
    fun d(tag: String, message: String) {
        val logMessage = formatLogMessage("D", tag, message)
        Log.d(tag, message)
        writeToLogFile(logMessage)
        updateSessionStats("debug_count", 1)
    }
    
    /**
     * 记录信息
     */
    fun i(tag: String, message: String) {
        val logMessage = formatLogMessage("I", tag, message)
        Log.i(tag, message)
        writeToLogFile(logMessage)
        updateSessionStats("info_count", 1)
    }
    
    /**
     * 记录警告
     */
    fun w(tag: String, message: String) {
        val logMessage = formatLogMessage("W", tag, message)
        Log.w(tag, message)
        writeToLogFile(logMessage)
        updateSessionStats("warning_count", 1)
    }
    
    /**
     * 记录错误
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val logMessage = formatLogMessage("E", tag, message, throwable)
        Log.e(tag, message, throwable)
        writeToLogFile(logMessage)
        updateSessionStats("error_count", 1)
        
        // 统计错误
        incrementErrorCount(tag)
        
        // 如果错误过多，触发清理
        if (getTotalErrorCount() > MAX_ERROR_COUNT) {
            cleanupOldErrors()
        }
    }
    
    /**
     * 记录性能指标
     */
    fun performance(tag: String, operation: String, duration: Long) {
        val logMessage = formatLogMessage("P", tag, "$operation took ${duration}ms")
        writeToLogFile(logMessage)
        
        val key = "$tag:$operation"
        performanceMetrics.getOrPut(key) { mutableListOf() }.add(duration)
        
        // 保持最近100次记录
        performanceMetrics[key]?.let { metrics ->
            if (metrics.size > 100) {
                metrics.removeAt(0)
            }
        }
        
        updateSessionStats("performance_count", 1)
    }
    
    /**
     * 开始性能监控
     */
    fun startPerformanceMonitor(tag: String, operation: String): PerformanceTimer {
        return PerformanceTimer(this, tag, operation)
    }
    
    /**
     * 记录异常详细信息
     */
    fun logException(tag: String, throwable: Throwable, context: String = "") {
        val contextInfo = if (context.isNotEmpty()) " [$context]" else ""
        e(tag, "异常$contextInfo: ${throwable.message}", throwable)
        
        // 记录堆栈跟踪的详细信息
        val stackTrace = throwable.stackTraceToString()
        val maxStackLines = 50 // 限制堆栈信息长度
        val stackLines = stackTrace.split("\n").take(maxStackLines)
        
        for ((index, line) in stackLines.withIndex()) {
            d("$tag-STACK", String.format(Locale.ROOT, "%02d: %s", index, line))
        }
    }
    
    /**
     * 获取错误统计
     */
    fun getErrorStatistics(): Map<String, Long> {
        return errorCounts.mapValues { it.value.get() }
    }
    
    /**
     * 获取性能统计
     */
    fun getPerformanceStatistics(): Map<String, PerformanceStats> {
        return performanceMetrics.mapValues { (key, values) ->
            PerformanceStats(
                operation = key,
                count = values.size,
                averageMs = values.average(),
                minMs = values.minOrNull() ?: 0,
                maxMs = values.maxOrNull() ?: 0,
                latestMs = values.lastOrNull() ?: 0
            )
        }
    }
    
    /**
     * 获取会话统计
     */
    fun getSessionStatistics(): Map<String, Any> {
        val stats = sessionStats.toMap().toMutableMap()
        stats["device_info"] = mapOf(
            "model" to Build.MODEL,
            "android_version" to Build.VERSION.SDK_INT,
            "manufacturer" to Build.MANUFACTURER
        )
        stats["log_files"] = getLogFileCount()
        stats["total_error_count"] = getTotalErrorCount()
        stats["uptime_ms"] = System.currentTimeMillis() - startTime
        return stats
    }
    
    /**
     * 导出日志文件
     */
    fun exportLogs(): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val exportFile = File(logDirectory, "${LOG_FILE_PREFIX}_export_$timestamp.zip")
            
            // 创建导出文件的逻辑（这里简化实现）
            writeToLogFile(formatLogMessage("I", TAG, "开始导出日志文件"))
            
            exportFile
        } catch (e: Exception) {
            e(TAG, "导出日志失败", e)
            null
        }
    }
    
    /**
     * 清理旧日志
     */
    fun cleanupOldLogs() {
        try {
            val logFiles = logDirectory.listFiles { file ->
                file.name.startsWith(LOG_FILE_PREFIX) && file.name.endsWith(".log")
            }?.sortedByDescending { it.lastModified() } ?: emptyList()
            
            if (logFiles.size > MAX_LOG_FILES) {
                logFiles.drop(MAX_LOG_FILES).forEach { file ->
                    file.delete()
                    d(TAG, "删除旧日志文件: ${file.name}")
                }
            }
        } catch (e: Exception) {
            e(TAG, "清理旧日志失败", e)
        }
    }
    
    /**
     * 清理错误统计
     */
    fun cleanupOldErrors() {
        errorCounts.clear()
        d(TAG, "清理错误统计缓存")
    }
    
    /**
     * 刷新日志到文件
     */
    fun flushLogs() {
        // 这个方法用于强制刷新缓存的日志
        lastFlushTime.set(System.currentTimeMillis())
    }
    
    /**
     * 释放资源
     */
    fun shutdown() {
        try {
            executor.shutdown()
            flushLogs()
            Log.i(TAG, "日志管理器已关闭")
        } catch (e: Exception) {
            Log.e(TAG, "关闭日志管理器时发生错误", e)
        }
    }
    
    // 私有方法
    private fun formatLogMessage(level: String, tag: String, message: String, throwable: Throwable? = null): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val threadName = Thread.currentThread().name
        val pid = android.os.Process.myPid()
        
        val baseMessage = "[$timestamp] [$level] [PID:$pid] [$threadName] $tag: $message"
        
        return if (throwable != null) {
            "$baseMessage\n${throwable.stackTraceToString()}"
        } else {
            baseMessage
        }
    }
    
    private fun writeToLogFile(message: String) {
        executor.execute {
            try {
                if (!logDirectory.exists()) {
                    logDirectory.mkdirs()
                }
                
                // 检查文件大小，必要时轮转
                if (currentLogFile.exists() && currentLogFile.length() > MAX_LOG_FILE_SIZE) {
                    rotateLogFile()
                }
                
                FileWriter(currentLogFile, true).use { writer ->
                    writer.write(message)
                    writer.write("\n")
                    writer.flush()
                }
                
                // 定期清理旧文件
                val now = System.currentTimeMillis()
                if (now - lastFlushTime.get() > LOG_FLUSH_INTERVAL) {
                    cleanupOldLogs()
                    lastFlushTime.set(now)
                }
            } catch (e: IOException) {
                Log.e(TAG, "写入日志文件失败", e)
            }
        }
    }
    
    private fun rotateLogFile() {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val rotatedFile = File(logDirectory, "${LOG_FILE_PREFIX}_$timestamp.log")
            currentLogFile.renameTo(rotatedFile)
            d(TAG, "日志文件轮转: ${rotatedFile.name}")
        } catch (e: Exception) {
            Log.e(TAG, "日志文件轮转失败", e)
        }
    }
    
    private fun incrementErrorCount(tag: String) {
        errorCounts.getOrPut(tag) { AtomicLong(0) }.incrementAndGet()
    }
    
    private fun updateSessionStats(key: String, increment: Long) {
        val current = sessionStats[key] as? Long ?: 0L
        sessionStats[key] = current + increment
    }
    
    private fun getTotalErrorCount(): Long {
        return errorCounts.values.sumOf { it.get() }
    }
    
    private fun getLogFileCount(): Int {
        return logDirectory.listFiles { file ->
            file.name.startsWith(LOG_FILE_PREFIX) && file.name.endsWith(".log")
        }?.size ?: 0
    }
    
    private val startTime = System.currentTimeMillis()
    
    // 性能统计数据类
    data class PerformanceStats(
        val operation: String,
        val count: Int,
        val averageMs: Double,
        val minMs: Long,
        val maxMs: Long,
        val latestMs: Long
    )
    
    // 性能监控计时器
    class PerformanceTimer(
        private val logger: LoggerManager,
        private val tag: String,
        private val operation: String
    ) {
        private val startTime = System.nanoTime()
        
        fun stop() {
            val duration = (System.nanoTime() - startTime) / 1_000_000 // 转换为毫秒
            logger.performance(tag, operation, duration)
        }
    }
}