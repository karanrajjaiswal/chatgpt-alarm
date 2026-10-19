package com.prabhat.alarm

import android.content.Context
import java.time.LocalDate

data class UserAlarm(
    val id: Long,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean = true,
    val label: String = "Daily alarm"
)

data class MuhurtaInfo(
    val sunrise: java.time.ZonedDateTime,
    val windowStart: java.time.ZonedDateTime,
    val windowEnd: java.time.ZonedDateTime,
    val recommended: java.time.ZonedDateTime
)

object AlarmStore {
    private const val PREFS = "prabhat_preferences"
    private const val KEY_ALARMS = "alarms_v1"
    private const val KEY_BRAHMA_ENABLED = "brahma_enabled"
    private const val KEY_OFFSET = "brahma_offset_minutes"
    private const val KEY_LATITUDE = "latitude"
    private const val KEY_LONGITUDE = "longitude"
    private const val KEY_LOCATION_UPDATED = "location_updated_at"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getAlarms(context: Context): List<UserAlarm> = prefs(context).getString(KEY_ALARMS, "")
        .orEmpty()
        .lineSequence()
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val p = line.split('|', limit = 5)
            if (p.size < 5) return@mapNotNull null
            runCatching {
                UserAlarm(
                    id = p[0].toLong(),
                    hour = p[1].toInt().coerceIn(0, 23),
                    minute = p[2].toInt().coerceIn(0, 59),
                    enabled = p[3].toBooleanStrict(),
                    label = p[4].replace('_', ' ').ifBlank { "Daily alarm" }
                )
            }.getOrNull()
        }
        .sortedWith(compareBy<UserAlarm> { it.hour }.thenBy { it.minute })
        .toList()

    fun saveAlarms(context: Context, alarms: List<UserAlarm>) {
        val encoded = alarms.joinToString("\n") { a ->
            val safeLabel = a.label.trim().replace('|', ' ').replace('\n', ' ').replace('_', ' ')
                .ifBlank { "Daily alarm" }.replace(' ', '_')
            "${a.id}|${a.hour}|${a.minute}|${a.enabled}|$safeLabel"
        }
        prefs(context).edit().putString(KEY_ALARMS, encoded).apply()
    }

    fun setBrahmaEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_BRAHMA_ENABLED, enabled).apply()
    }

    fun isBrahmaEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_BRAHMA_ENABLED, false)

    fun setBrahmaOffset(context: Context, minutes: Int) {
        prefs(context).edit().putInt(KEY_OFFSET, minutes.coerceIn(48, 96)).apply()
    }

    fun brahmaOffset(context: Context): Int = prefs(context).getInt(KEY_OFFSET, 72).coerceIn(48, 96)

    fun saveLocation(context: Context, latitude: Double, longitude: Double, observedAt: Long = System.currentTimeMillis()) {
        prefs(context).edit()
            .putString(KEY_LATITUDE, latitude.toString())
            .putString(KEY_LONGITUDE, longitude.toString())
            .putLong(KEY_LOCATION_UPDATED, observedAt)
            .apply()
    }

    fun location(context: Context): Pair<Double, Double>? {
        val p = prefs(context)
        val lat = p.getString(KEY_LATITUDE, null)?.toDoubleOrNull() ?: return null
        val lon = p.getString(KEY_LONGITUDE, null)?.toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
        return lat to lon
    }

    fun locationUpdatedAt(context: Context): Long = prefs(context).getLong(KEY_LOCATION_UPDATED, 0L)

    fun updateAlarm(context: Context, updated: UserAlarm) {
        val next = getAlarms(context).filterNot { it.id == updated.id } + updated
        saveAlarms(context, next)
    }

    fun removeAlarm(context: Context, id: Long) {
        saveAlarms(context, getAlarms(context).filterNot { it.id == id })
    }
}

fun LocalDate.toReadableDate(): String =
    format(java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM"))
