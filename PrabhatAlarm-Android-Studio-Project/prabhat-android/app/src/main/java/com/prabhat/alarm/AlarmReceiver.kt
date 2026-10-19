package com.prabhat.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_TYPE) ?: AlarmScheduler.TYPE_REGULAR
        val id = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        val label = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_LABEL) ?: "Time to wake up"

        when (type) {
            AlarmScheduler.TYPE_REGULAR -> {
                val saved = AlarmStore.getAlarms(context).firstOrNull { it.id == id } ?: return
                if (!saved.enabled) return
                // Daily recurrence: queue tomorrow before beginning the ringing flow.
                AlarmScheduler.scheduleRegular(context, saved)
            }
            AlarmScheduler.TYPE_BRAHMA -> {
                if (!AlarmStore.isBrahmaEnabled(context)) return
                // Recalculate for the next local sunrise rather than repeating at a fixed clock time.
                AlarmScheduler.scheduleBrahma(context)
            }
            AlarmScheduler.TYPE_SNOOZE -> Unit
            else -> return
        }

        val serviceIntent = Intent(context, AlarmRingingService::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, id)
            putExtra(AlarmScheduler.EXTRA_ALARM_TYPE, type)
            putExtra(AlarmScheduler.EXTRA_ALARM_LABEL, label)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ContextCompat.startForegroundService(context, serviceIntent)
            else context.startService(serviceIntent)
        } catch (_: Exception) {
            // If a device vendor blocks background service starts, the exact-alarm
            // notification is still the platform entry point to the ringing screen.
            AlarmRingingService.postFallbackNotification(context, id, type, label)
        }
    }
}
