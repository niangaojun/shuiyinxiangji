package com.example.watermarkcamera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class LocationManager(
    private val context: Context,
    private val onLocationReceived: (Location) -> Unit
) : DefaultLifecycleObserver, LocationListener {

    companion object {
        private const val TAG = "LocationManager"
        private const val UPDATE_INTERVAL_MS = 2000L // 2秒，更频繁更新
        private const val MIN_DISTANCE_M = 5.0f // 5米，更精确
        private const val MIN_ACCURACY = 50f // 最小精度要求（米）
        private const val MAX_ACCURACY_WAIT_TIME = 10000L // 最大等待时间10秒
    }

    private val locationManager: LocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var isLocationUpdatesActive = false
    private val mainLooper = Looper.getMainLooper()
    
    // 位置质量跟踪
    private var bestLocation: Location? = null
    private var locationUpdateCount = 0
    private var lastLocationUpdateTime = 0L

    override fun onResume(owner: LifecycleOwner) {
        startLocationUpdates()
    }

    override fun onPause(owner: LifecycleOwner) {
        stopLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (!hasLocationPermission()) {
            Log.d(TAG, "位置权限未授予")
            return
        }

        if (isLocationUpdatesActive) return

        try {
            // 重置位置跟踪
            bestLocation = null
            locationUpdateCount = 0
            lastLocationUpdateTime = System.currentTimeMillis()

            // 优先使用GPS获取精确位置
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    UPDATE_INTERVAL_MS,
                    MIN_DISTANCE_M,
                    this,
                    mainLooper
                )
                Log.d(TAG, "已注册GPS位置更新")
            }

            // 同时使用网络定位作为补充
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    UPDATE_INTERVAL_MS,
                    MIN_DISTANCE_M,
                    this,
                    mainLooper
                )
                Log.d(TAG, "已注册网络位置更新")
            }

            // 使用FUSED_PROVIDER（如果可用）
            if (locationManager.isProviderEnabled(LocationManager.FUSED_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.FUSED_PROVIDER,
                    UPDATE_INTERVAL_MS,
                    MIN_DISTANCE_M,
                    this,
                    mainLooper
                )
                Log.d(TAG, "已注册FUSED位置更新")
            }

            isLocationUpdatesActive = true
            Log.d(TAG, "位置更新已启动")
        } catch (e: Exception) {
            Log.e(TAG, "启动位置更新失败: ${e.message}")
        }
    }

    private fun stopLocationUpdates() {
        if (!isLocationUpdatesActive) return

        try {
            locationManager.removeUpdates(this)
            isLocationUpdatesActive = false
            Log.d(TAG, "位置更新已停止")
        } catch (e: Exception) {
            Log.e(TAG, "停止位置更新失败: ${e.message}")
        }
    }

    override fun onLocationChanged(location: Location) {
        locationUpdateCount++
        val accuracy = if (location.hasAccuracy()) location.accuracy else Float.MAX_VALUE
        
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "收到位置更新 #$locationUpdateCount: ${location.latitude}, ${location.longitude}")
            Log.d(TAG, "位置提供者: ${location.provider}")
            Log.d(TAG, "精度: ${accuracy}米")
            Log.d(TAG, "海拔: ${if (location.hasAltitude()) location.altitude else "无"}")
        }

        // 更新最佳位置
        if (isBetterLocation(location, bestLocation)) {
            bestLocation = location
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "更新最佳位置，精度: ${accuracy}米")
            }
        }

        // 如果精度足够好，立即通知
        if (accuracy <= MIN_ACCURACY) {
            onLocationReceived(location)
        } else {
            // 否则使用当前最佳位置
            bestLocation?.let { onLocationReceived(it) }
        }
    }

    /**
     * 判断新位置是否比当前最佳位置更好
     */
    private fun isBetterLocation(newLocation: Location, currentBestLocation: Location?): Boolean {
        if (currentBestLocation == null) return true

        // 检查时间差
        val timeDelta = newLocation.time - currentBestLocation.time
        val isSignificantlyNewer = timeDelta > 60000 // 1分钟以上算新
        val isSignificantlyOlder = timeDelta < -60000
        val isNewer = timeDelta > 0

        // 如果新位置超过1分钟，使用它
        if (isSignificantlyNewer) return true
        if (isSignificantlyOlder) return false

        // 检查精度
        val accuracyDelta = (newLocation.accuracy - currentBestLocation.accuracy).toInt()
        val isLessAccurate = accuracyDelta > 0
        val isMoreAccurate = accuracyDelta < 0
        val isSignificantlyLessAccurate = accuracyDelta > 100 // 精度差超过100米

        // 检查提供者
        val isFromSameProvider = isSameProvider(newLocation.provider, currentBestLocation.provider)

        // 如果新位置更精确，使用它
        if (isMoreAccurate) return true

        // 如果精度相同且更新，使用它
        if (isNewer && !isLessAccurate) return true

        // 如果精度稍差但来自同一提供者且更新，使用它
        if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) return true

        return false
    }

    private fun isSameProvider(provider1: String?, provider2: String?): Boolean {
        return if (provider1 == null) provider2 == null else provider1 == provider2
    }

    @Deprecated("已弃用但需要实现")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    override fun onProviderEnabled(provider: String) {
        Log.d(TAG, "位置提供商已启用: $provider")
    }

    override fun onProviderDisabled(provider: String) {
        Log.d(TAG, "位置提供商已禁用: $provider")
    }

    /**
     * 获取当前最佳位置（优先使用高精度位置）
     */
    fun getCurrentBestLocation(): Location? {
        return bestLocation
    }

    /**
     * 获取位置精度状态
     */
    fun getLocationAccuracyStatus(): LocationAccuracyStatus {
        val location = bestLocation ?: return LocationAccuracyStatus.NO_LOCATION
        
        if (!location.hasAccuracy()) return LocationAccuracyStatus.UNKNOWN
        
        return when {
            location.accuracy <= 10f -> LocationAccuracyStatus.EXCELLENT
            location.accuracy <= 30f -> LocationAccuracyStatus.GOOD
            location.accuracy <= 50f -> LocationAccuracyStatus.FAIR
            else -> LocationAccuracyStatus.POOR
        }
    }

    /**
     * 检查位置是否足够精确
     */
    fun isLocationAccurateEnough(): Boolean {
        val location = bestLocation ?: return false
        return location.hasAccuracy() && location.accuracy <= MIN_ACCURACY
    }

    /**
     * 等待获取精确位置
     */
    fun waitForAccurateLocation(timeoutMs: Long = MAX_ACCURACY_WAIT_TIME, callback: (Location?) -> Unit) {
        val startTime = System.currentTimeMillis()
        
        val checkRunnable = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTime
                
                when {
                    // 已获得精确位置
                    isLocationAccurateEnough() -> {
                        Log.d(TAG, "获得精确位置，耗时: ${elapsed}ms")
                        callback(bestLocation)
                    }
                    // 超时，返回最佳位置
                    elapsed >= timeoutMs -> {
                        Log.d(TAG, "等待超时，使用最佳位置，精度: ${bestLocation?.accuracy}米")
                        callback(bestLocation)
                    }
                    // 继续等待
                    else -> {
                        mainLooper.let { 
                            android.os.Handler(it).postDelayed(this, 500)
                        }
                    }
                }
            }
        }
        
        android.os.Handler(mainLooper).post(checkRunnable)
    }

    @SuppressLint("MissingPermission")
    fun getLastKnownLocation(): Location? {
        if (!hasLocationPermission()) return null

        // 优先使用最佳位置
        bestLocation?.let {
            Log.d(TAG, "使用当前最佳位置")
            return it
        }

        // 按优先级尝试获取最后已知位置
        val providers = listOf(
            LocationManager.FUSED_PROVIDER,
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        )

        for (provider in providers) {
            try {
                locationManager.getLastKnownLocation(provider)?.let {
                    // 检查位置是否太旧（超过5分钟）
                    val age = System.currentTimeMillis() - it.time
                    if (age < 300000) { // 5分钟内
                        Log.d(TAG, "获取到最后已知位置: $provider, 年龄: ${age/1000}秒")
                        bestLocation = it
                        return it
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "获取 $provider 最后位置失败: ${e.message}")
            }
        }

        return null
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 位置精度状态枚举
     */
    enum class LocationAccuracyStatus {
        NO_LOCATION,    // 无位置
        UNKNOWN,        // 未知精度
        POOR,           // 差 (>50米)
        FAIR,           // 一般 (30-50米)
        GOOD,           // 好 (10-30米)
        EXCELLENT       // 优秀 (<10米)
    }
}
