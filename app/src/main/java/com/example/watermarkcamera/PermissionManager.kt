package com.example.watermarkcamera

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionManager(private val context: Context) {

    companion object {
        const val ALL_PERMISSIONS_REQUEST_CODE = 100

        private val CAMERA_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

        private val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        private fun getStoragePermissions(): Array<String> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
        }

        private val ALL_PERMISSIONS: Array<String>
            get() = CAMERA_PERMISSIONS + LOCATION_PERMISSIONS + getStoragePermissions()
    }

    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasLocationPermission(): Boolean {
        return LOCATION_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasStoragePermission(): Boolean {
        return getStoragePermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasAllRequiredPermissions(): Boolean {
        return hasCameraPermission() && hasLocationPermission() && hasStoragePermission()
    }

    fun requestAllPermissions(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            ALL_PERMISSIONS,
            ALL_PERMISSIONS_REQUEST_CODE
        )
    }

    fun requestLocationPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            LOCATION_PERMISSIONS,
            ALL_PERMISSIONS_REQUEST_CODE
        )
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        onPermissionGranted: () -> Unit,
        onPermissionDenied: () -> Unit
    ) {
        if (requestCode == ALL_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                onPermissionGranted()
            } else {
                onPermissionDenied()
            }
        }
    }

    fun getMissingPermissions(): List<String> {
        return ALL_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
    }
}
