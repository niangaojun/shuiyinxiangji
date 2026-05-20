package com.example.watermarkcamera

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.*
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

/**
 * LocationManager 单元测试
 * 测试位置管理器的所有功能
 */
class LocationManagerTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockLocationManager: LocationManager
    
    @Mock
    private lateinit var mockLoggerManager: LoggerManager
    
    @Mock
    private lateinit var mockLocation: Location
    
    @Mock
    private lateinit var mockPreferences: SharedPreferences
    
    private lateinit var locationManager: TestLocationManager
    
    // 位置更新回调
    private var locationUpdateCallback: ((Location) -> Unit)? = null
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        
        // Mock LocationManager
        `when`(mockContext.getSystemService(Context.LOCATION_SERVICE)).thenReturn(mockLocationManager)
        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPreferences)
        
        // Mock permissions
        `when`(mockContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION))
            .thenReturn(PackageManager.PERMISSION_GRANTED)
        `when`(mockContext.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION))
            .thenReturn(PackageManager.PERMISSION_GRANTED)
        
        // Mock location providers status
        `when`(mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true)
        `when`(mockLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)).thenReturn(true)
        
        // Mock last known location
        `when`(mockLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER))
            .thenReturn(mockLocation)
        `when`(mockLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER))
            .thenReturn(mockLocation)
        
        // Mock location properties
        `when`(mockLocation.latitude).thenReturn(39.9042)
        `when`(mockLocation.longitude).thenReturn(116.4074)
        `when`(mockLocation.accuracy).thenReturn(10f)
        `when`(mockLocation.time).thenReturn(System.currentTimeMillis())
        
        locationManager = TestLocationManager(mockContext, mockLoggerManager) { location ->
            locationUpdateCallback?.invoke(location)
        }
    }
    
    @Test
    fun `test initialization`() {
        assertNotNull(locationManager)
        assertFalse(locationManager.isLocationUpdatesActive())
    }
    
    @Test
    fun `test is location enabled`() {
        // GPS enabled, network enabled
        `when`(mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true)
        `when`(mockLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)).thenReturn(true)
        
        assertTrue(locationManager.isLocationEnabled())
        
        // GPS disabled, network enabled
        `when`(mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(false)
        assertTrue(locationManager.isLocationEnabled())
        
        // Both disabled
        `when`(mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(false)
        `when`(mockLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)).thenReturn(false)
        assertFalse(locationManager.isLocationEnabled())
    }
    
    @Test
    fun `test get last known location`() {
        val location = locationManager.getLastKnownLocation()
        
        assertNotNull(location)
        assertEquals(39.9042, location?.latitude, 0.001)
        assertEquals(116.4074, location?.longitude, 0.001)
        assertEquals(10f, location?.accuracy, 0.1f)
    }
    
    @Test
    fun `test get quick location`() {
        val quickLocation = locationManager.getQuickLocation()
        
        assertNotNull(quickLocation)
        assertEquals(39.9042, quickLocation?.latitude, 0.001)
        assertEquals(116.4074, quickLocation?.longitude, 0.001)
    }
    
    @Test
    fun `test location accuracy`() {
        val accuracy = locationManager.getLocationAccuracy()
        
        assertEquals(10f, accuracy, 0.1f)
    }
    
    @Test
    fun `test location age`() {
        val age = locationManager.getLocationAge()
        
        assertTrue(age >= 0)
        assertTrue(age < 1000) // Should be recent
    }
    
    @Test
    fun `test start and stop location updates`() {
        // Start location updates
        locationManager.startLocationUpdates()
        assertTrue(locationManager.isLocationUpdatesActive())
        
        // Stop location updates
        locationManager.stopLocationUpdates()
        assertFalse(locationManager.isLocationUpdatesActive())
    }
    
    @Test
    fun `test location update callback`() {
        var callbackInvoked = false
        var receivedLocation: Location? = null
        
        locationUpdateCallback = { location ->
            callbackInvoked = true
            receivedLocation = location
        }
        
        // Simulate location update
        locationManager.simulateLocationUpdate(mockLocation)
        
        assertTrue(callbackInvoked)
        assertNotNull(receivedLocation)
        assertEquals(mockLocation, receivedLocation)
    }
    
    @Test
    fun `test location caching`() {
        val testLocation = Location("test")
        testLocation.latitude = 31.2304
        testLocation.longitude = 121.4737
        testLocation.time = System.currentTimeMillis()
        
        locationManager.cacheLocation(testLocation)
        
        val cachedLocation = locationManager.getCachedLocation()
        assertNotNull(cachedLocation)
        assertEquals(31.2304, cachedLocation?.latitude, 0.001)
        assertEquals(121.4737, cachedLocation?.longitude, 0.001)
    }
    
    @Test
    fun `test location cache expiry`() {
        val oldLocation = Location("test")
        oldLocation.latitude = 31.2304
        oldLocation.longitude = 121.4737
        oldLocation.time = System.currentTimeMillis() - (25 * 60 * 60 * 1000) // 25 hours ago
        
        locationManager.cacheLocation(oldLocation)
        
        val cachedLocation = locationManager.getCachedLocation()
        assertNull(cachedLocation) // Should be expired
    }
    
    @Test
    fun `test permission denied scenario`() {
        // Mock permission denied
        `when`(mockContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION))
            .thenReturn(PackageManager.PERMISSION_DENIED)
        `when`(mockContext.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION))
            .thenReturn(PackageManager.PERMISSION_DENIED)
        
        val location = locationManager.getLastKnownLocation()
        
        assertNull(location)
    }
    
    @Test
    fun `test provider status updates`() {
        // GPS provider enabled
        `when`(mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true)
        locationManager.onProviderEnabled(LocationManager.GPS_PROVIDER)
        
        // Network provider disabled
        `when`(mockLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)).thenReturn(false)
        locationManager.onProviderDisabled(LocationManager.NETWORK_PROVIDER)
        
        // Status changed (deprecated method)
        locationManager.onStatusChanged(LocationManager.GPS_PROVIDER, PackageManager.PERMISSION_GRANTED, null)
        
        // These should not throw exceptions
        assertTrue(true) // Test passed if no exception thrown
    }
    
    // Test LocationManager class for testing purposes
    private inner class TestLocationManager(
        context: Context,
        loggerManager: LoggerManager,
        private val onLocationUpdate: (Location) -> Unit
    ) : LocationManager(context, loggerManager, onLocationUpdate) {
        
        fun simulateLocationUpdate(location: Location) {
            // Simulate location update for testing
            onLocationUpdate(location)
        }
        
        fun isLocationUpdatesActive(): Boolean {
            return isLocationUpdatesActive
        }
        
        fun cacheLocation(location: Location) {
            cacheLocation(location)
        }
        
        fun getCachedLocation(): Location? {
            return getCachedLocation()
        }
    }
}