package com.prabhat.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

object AlarmScheduler {
    const val EXTRA_ALARM_ID = "alarm_id"
    const val EXTRA_ALARM_TYPE = "alarm_type"
    const val EXTRA_ALARM_LABEL = "alarm_label"
    const val TYPE_REGULAR = "REGULAR"
    const val TYPE_BRAHMA = "BRAHMA"
    const val TYPE_SNOOZE = "SNOOZE"
    private const val BRAHMA_ID = -7001L
    private const val SNOOZE_ID = -7002L
    private const val BRAHMA_REQUEST = 87001
    private const val SNOOZE_REQUEST = 87002

    fun scheduleAll(context: Context) {
        AlarmStore.getAlarms(context).forEach { scheduleRegular(context, it) }
        if (AlarmStore.isBrahmaEnabled(context)) scheduleBrahma(context) else cancelBrahma(context)
    }

    fun scheduleRegular(context: Context, alarm: UserAlarm) {
        if (!alarm.enabled) {
            cancelRegular(context, alarm.id)
            return
        }
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        var trigger = LocalDateTime.of(now.toLocalDate(), java.time.LocalTime.of(alarm.hour, alarm.minute)).atZone(zone)
        if (!trigger.isAfter(now.plusSeconds(2))) trigger = trigger.plusDays(1)
        schedule(context, alarm.id, TYPE_REGULAR, alarm.label, trigger.toInstant().toEpochMilli(), alarm.id.toInt())
    }

    fun cancelRegular(context: Context, id: Long) {
        val pending = regularPendingIntent(context, id)
        alarmManager(context).cancel(pending)
        pending.cancel()
    }

    fun scheduleBrahma(context: Context): Boolean {
        if (!AlarmStore.isBrahmaEnabled(context)) {
            cancelBrahma(context)
            return false
        }
        val location = AlarmStore.location(context) ?: return false
        val next = SolarCalculator.nextBrahmaInstant(
            latitude = location.first,
            longitude = location.second,
            offsetMinutes = AlarmStore.brahmaOffset(context)
        ) ?: return false
        schedule(context, BRAHMA_ID, TYPE_BRAHMA, "Brahma Muhurta", next.toInstant().toEpochMilli(), BRAHMA_REQUEST)
        return true
    }

    fun cancelBrahma(context: Context) {
        val pending = typePendingIntent(context, BRAHMA_ID, TYPE_BRAHMA, BRAHMA_REQUEST)
        alarmManager(context).cancel(pending)
        pending.cancel()
    }

    fun scheduleSnooze(context: Context) {
        val trigger = System.currentTimeMillis() + 5 * 60 * 1000L
        schedule(context, SNOOZE_ID, TYPE_SNOOZE, "Snoozed alarm", trigger, SNOOZE_REQUEST)
    }

    private fun schedule(context: Context, id: Long, type: String, label: String, triggerAtMillis: Long, requestCode: Int) {
        val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.prabhat.alarm.RING"
            data = Uri.parse("prabhat://alarm/$type/$id")
            putExtra(EXTRA_ALARM_ID, id)
            putExtra(EXTRA_ALARM_TYPE, type)
            putExtra(EXTRA_ALARM_LABEL, label)
        }
        val alarmPending = PendingIntent.getBroadcast(
            context, requestCode, alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val showIntent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val info = AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent)
        alarmManager(context).setAlarmClock(info, alarmPending)
    }

    private fun regularPendingIntent(context: Context, id: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.prabhat.alarm.RING"
            data = Uri.parse("prabhat://alarm/$TYPE_REGULAR/$id")
        }
        return PendingIntent.getBroadcast(
            context, id.toInt(), intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: PendingIntent.getBroadcast(
            context, id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun typePendingIntent(context: Context, id: Long, type: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.prabhat.alarm.RING"
            data = Uri.parse("prabhat://alarm/$type/$id")
        }
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun alarmManager(context: Context) = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
}
