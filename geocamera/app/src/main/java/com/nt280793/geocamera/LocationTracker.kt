package com.nt280793.geocamera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat

class LocationTracker(
    context: Context,
    private val onUpdate: (Location) -> Unit
) {
    private val manager = context.getSystemService(LocationManager::class.java)
    private val appContext = context.applicationContext
    private var started = false
    private val listener = LocationListener { location -> onUpdate(location) }

    fun providersEnabled(): Boolean {
        return try {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) {
            false
        }
    }

    fun start() {
        if (started || !hasPermission()) return
        started = true
        for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
            try {
                if (!manager.isProviderEnabled(provider)) continue
                manager.getLastKnownLocation(provider)?.let(onUpdate)
                manager.requestLocationUpdates(provider, 2_000L, 1f, listener, Looper.getMainLooper())
            } catch (_: Exception) {
                // A disabled or missing provider should not stop the other one.
            }
        }
    }

    fun stop() {
        if (!started) return
        started = false
        try {
            manager.removeUpdates(listener)
        } catch (_: Exception) {
        }
    }

    private fun hasPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }
}
