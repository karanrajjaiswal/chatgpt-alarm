package com.prabhat.alarm

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat

class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Reuse a newer system last-known fix when one exists. No background
        // location permission is requested; a fresh fix is obtained in the app.
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            runCatching {
                val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val latest = manager.getProviders(true).mapNotNull { provider ->
                    try { manager.getLastKnownLocation(provider) } catch (_: SecurityException) { null }
                }.maxByOrNull { it.time }
                if (latest != null && latest.time > AlarmStore.locationUpdatedAt(context)) {
                    AlarmStore.saveLocation(context, latest.latitude, latest.longitude, latest.time)
                }
            }
        }
        AlarmScheduler.scheduleAll(context)
    }
}
