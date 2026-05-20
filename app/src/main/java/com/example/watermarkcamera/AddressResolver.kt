package com.example.watermarkcamera

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.util.Locale

class AddressResolver(private val context: Context) {

    companion object {
        private const val TAG = "AddressResolver"
        private const val REQUEST_TIMEOUT_MS = 3000L
        private const val MAX_RESULTS = 1
    }

    private val geocoder: Geocoder? by lazy {
        if (Geocoder.isPresent()) {
            Geocoder(context, Locale.getDefault())
        } else {
            Log.w(TAG, "Geocoder 不可用")
            null
        }
    }

    suspend fun getAddressFromLocation(latitude: Double, longitude: Double): String? {
        val geocoderInstance = geocoder ?: return null

        return withContext(Dispatchers.IO) {
            try {
                withTimeout(REQUEST_TIMEOUT_MS) {
                    getAddressLegacy(geocoderInstance, latitude, longitude)
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "获取地址失败: ${e.message}")
                }
                null
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun getAddressLegacy(geocoder: Geocoder, latitude: Double, longitude: Double): String? {
        return try {
            val addresses = geocoder.getFromLocation(latitude, longitude, MAX_RESULTS)
            if (addresses != null && addresses.isNotEmpty()) {
                formatAddress(addresses[0])
            } else {
                null
            }
        } catch (e: IOException) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "获取地址IO错误: ${e.message}")
            }
            null
        }
    }

    private fun formatAddress(address: Address): String {
        val addressParts = ArrayList<String>()
        
        address.adminArea?.takeIf { it.isNotBlank() }?.let { addressParts.add(it) }
        address.locality?.takeIf { it.isNotBlank() }?.let { addressParts.add(it) }
        address.subLocality?.takeIf { it.isNotBlank() }?.let { addressParts.add(it) }
        address.thoroughfare?.takeIf { it.isNotBlank() }?.let { addressParts.add(it) }
        address.subThoroughfare?.takeIf { it.isNotBlank() }?.let { addressParts.add(it) }

        return if (addressParts.isEmpty()) {
            "地址: 获取中..."
        } else {
            addressParts.joinToString("")
        }
    }

    fun isGeocoderAvailable(): Boolean = Geocoder.isPresent()
}
