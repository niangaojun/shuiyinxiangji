package com.example.watermarkcamera

import android.content.Context
import android.location.Location
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * 海拔信息获取工具类
 * 使用Elevation API获取指定经纬度的海拔高度
 */
class ElevationManager(private val context: Context) {

    companion object {
        private const val TAG = "ElevationManager"
        private const val ELEVATION_API_URL = "https://api.open-elevation.com/api/v1/lookup"
        private const val MAX_CACHE_SIZE = 100
        private const val CACHE_EXPIRY_TIME = 24 * 60 * 60 * 1000L // 24小时
        private const val REQUEST_TIMEOUT_MS = 2000L
        private const val CONNECT_TIMEOUT_MS = 2000
        private const val READ_TIMEOUT_MS = 2000
    }

    // 使用LRU缓存替代ConcurrentHashMap
    private val elevationCache = object : LinkedHashMap<String, Pair<Float, Long>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<Float, Long>>?): Boolean {
            return size > MAX_CACHE_SIZE
        }
    }

    private val cacheLock = Any()

    suspend fun getElevationForLocation(location: Location): Location = withContext(Dispatchers.IO) {
        // 如果已经有海拔信息，直接返回
        if (location.hasAltitude()) {
            return@withContext location
        }

        val cacheKey = String.format(Locale.ROOT, "%.4f,%.4f", location.latitude, location.longitude)

        // 检查缓存
        synchronized(cacheLock) {
            elevationCache[cacheKey]?.let { (elevation, timestamp) ->
                if (System.currentTimeMillis() - timestamp < CACHE_EXPIRY_TIME) {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "使用缓存的海拔信息: $elevation 米")
                    }
                    return@withContext location.copyWithElevation(elevation)
                }
                elevationCache.remove(cacheKey)
            }
        }

        try {
            val result = withTimeout(REQUEST_TIMEOUT_MS) {
                fetchElevationFromApi(location, cacheKey)
            }
            result?.let { return@withContext it }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "获取海拔信息失败: ${e.message}")
            }
        }

        return@withContext location
    }

    private fun fetchElevationFromApi(location: Location, cacheKey: String): Location? {
        var connection: HttpURLConnection? = null

        return try {
            val apiUrl = URL("$ELEVATION_API_URL?locations=${location.latitude},${location.longitude}")

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "请求海拔信息: $apiUrl")
            }

            connection = apiUrl.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doInput = true
            }

            val responseCode = connection.responseCode
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "海拔API响应码: $responseCode")
            }

            if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        val response = reader.readText()
                        parseElevationResponse(response, location, cacheKey)
                    }
                }
            } else {
                null
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "API请求失败: ${e.message}")
            }
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseElevationResponse(response: String, location: Location, cacheKey: String): Location? {
        return try {
            val jsonResponse = JSONObject(response)
            val results = jsonResponse.getJSONArray("results")

            if (results.length() > 0) {
                val elevationData = results.getJSONObject(0)
                val elevation = elevationData.getDouble("elevation").toFloat()

                synchronized(cacheLock) {
                    elevationCache[cacheKey] = Pair(elevation, System.currentTimeMillis())
                }

                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "获取到海拔信息: $elevation 米")
                }

                location.copyWithElevation(elevation)
            } else {
                null
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "解析海拔响应失败: ${e.message}")
            }
            null
        }
    }

    private fun Location.copyWithElevation(elevation: Float): Location {
        return Location(this).apply {
            altitude = elevation.toDouble()
        }
    }
}
