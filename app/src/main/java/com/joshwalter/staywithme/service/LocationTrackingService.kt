package com.joshwalter.staywithme.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.joshwalter.staywithme.data.database.StayWithMeDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LocationTrackingService(private val context: Context) {
    
    private val database = StayWithMeDatabase.getDatabase(context)
    private val scope = CoroutineScope(Dispatchers.IO)
    private var fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private var locationCallback: LocationCallback? = null
    
    fun startLocationTracking() {
        if (!hasLocationPermission()) {
            return
        }
        
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5 * 60 * 1000L // 5 minutes
        ).apply {
            setMinUpdateIntervalMillis(2 * 60 * 1000L) // 2 minutes minimum
            setMaxUpdateDelayMillis(10 * 60 * 1000L) // 10 minutes maximum
        }.build()
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    updateCurrentSessionLocation(location)
                }
            }
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // Permission denied
        }
    }
    
    fun stopLocationTracking() {
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
        }
        locationCallback = null
    }
    
    private fun updateCurrentSessionLocation(location: Location) {
        scope.launch {
            val currentSession = database.checkInSessionDao().getCurrentSessionSync()
            currentSession?.let { session ->
                val locationString = "${location.latitude},${location.longitude}"
                database.checkInSessionDao().update(
                    session.copy(location = locationString)
                )
            }
        }
    }
    
    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) {
            return null
        }
        
        return try {
            fusedLocationClient.lastLocation.result
        } catch (e: SecurityException) {
            null
        }
    }
}