package com.prabhat.alarm

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private var latitude by mutableStateOf<Double?>(null)
    private var longitude by mutableStateOf<Double?>(null)
    private var locationStatus by mutableStateOf("Location not set")

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_FINE_LOCATION] == true || hasLocationPermission()) {
            fetchLocation()
        } else {
            locationStatus = "Location permission denied · regular alarms still work"
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Alarm screen remains usable; Android controls notification display. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadSavedLocation()
        AlarmScheduler.scheduleAll(this)

        setContent {
            PrabhatTheme {
                MainScreen(
                    latitude = latitude,
                    longitude = longitude,
                    locationStatus = locationStatus,
                    onRequestLocation = { requestLocation() },
                    onOpenFullScreenSettings = { openFullScreenSettings() }
                )
            }
        }

        if (hasLocationPermission()) fetchLocation()

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun loadSavedLocation() {
        AlarmStore.location(this)?.let { (lat, lon) ->
            latitude = lat
            longitude = lon
            locationStatus = "Saved coordinates · %.4f, %.4f".format(lat, lon)
        }
    }

    private fun requestLocation() {
        if (!hasLocationPermission()) {
            locationPermissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ))
        } else {
            fetchLocation()
        }
    }

    @Suppress("MissingPermission")
    private fun fetchLocation() {
        val manager = getSystemService(LOCATION_SERVICE) as LocationManager
        var latest: Location? = null
        for (provider in runCatching { manager.getProviders(true) }.getOrNull().orEmpty()) {
            val found = runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
            if (found != null && (latest == null || found.time > latest!!.time)) latest = found
        }
        if (latest != null) applyLocation(latest!!)
        locationStatus = if (latest != null) "Using last known location · refreshing…" else "Finding your location…"

        var requestStarted = false
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                applyLocation(location)
                runCatching { manager.removeUpdates(this) }
            }
            @Deprecated("Deprecated in Android API")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
        }
        for (provider in runCatching { manager.getProviders(true) }.getOrNull().orEmpty()) {
            try {
                manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                requestStarted = true
            } catch (_: SecurityException) {
                // The user may have granted only approximate/coarse location.
            } catch (_: IllegalArgumentException) {
                // Provider is not available on this device.
            }
        }
        if (!requestStarted && latest == null) {
            locationStatus = "Location unavailable · turn on device location and try again"
        } else if (!requestStarted && latest != null) {
            locationStatus = "Saved coordinates · %.4f, %.4f".format(latest!!.latitude, latest!!.longitude)
        }
    }

    private fun applyLocation(location: Location) {
        latitude = location.latitude
        longitude = location.longitude
        locationStatus = "Current location · %.4f, %.4f".format(location.latitude, location.longitude)
        AlarmStore.saveLocation(this, location.latitude, location.longitude, location.time)
        if (AlarmStore.isBrahmaEnabled(this)) AlarmScheduler.scheduleBrahma(this)
    }

    private fun openFullScreenSettings() {
        if (Build.VERSION.SDK_INT >= 34) {
            val manager = getSystemService(android.app.NotificationManager::class.java)
            if (!manager.canUseFullScreenIntent()) {
                startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:$packageName")))
            }
        } else {
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
        }
    }
}
