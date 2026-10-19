package com.prabhat.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class AlarmRingingService : Service() {
    private var ringtone: Ringtone? = null
    private var alarmId: Long = -1L
    private var alarmType: String = AlarmScheduler.TYPE_REGULAR
    private var alarmLabel: String = "Time to wake up"

    override fun onCreate() {
        super.onCreate()
        createChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopRinging()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        alarmId = intent?.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L) ?: -1L
        alarmType = intent?.getStringExtra(AlarmScheduler.EXTRA_ALARM_TYPE) ?: AlarmScheduler.TYPE_REGULAR
        alarmLabel = intent?.getStringExtra(AlarmScheduler.EXTRA_ALARM_LABEL) ?: "Time to wake up"
        val notification = buildNotification(this, alarmId, alarmType, alarmLabel)
        startForeground(NOTIFICATION_ID, notification)
        if (ringtone?.isPlaying != true) startRinging()
        return START_STICKY
    }

    private fun startRinging() {
        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(applicationContext, uri)
            ringtone?.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) ringtone?.isLooping = true
            ringtone?.play()
        }
    }

    private fun stopRinging() {
        runCatching { ringtone?.stop() }
        ringtone = null
    }

    override fun onDestroy() {
        stopRinging()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "prabhat_alarm_channel"
        private const val NOTIFICATION_ID = 501
        private const val ACTION_STOP = "com.prabhat.alarm.STOP_RINGING"

        fun stop(context: Context) {
            context.stopService(Intent(context, AlarmRingingService::class.java))
            runCatching { NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID) }
        }

        fun stopIntent(context: Context): Intent = Intent(context, AlarmRingingService::class.java).setAction(ACTION_STOP)

        fun postFallbackNotification(context: Context, id: Long, type: String, label: String) {
            createChannel(context)
            runCatching {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, buildNotification(context, id, type, label))
            }
        }

        private fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        context.getString(R.string.notification_channel_name),
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = context.getString(R.string.notification_channel_description)
                        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                        enableVibration(true)
                        // The foreground service plays the system alarm tone itself.
                        setSound(null, null)
                    }
                    manager.createNotificationChannel(channel)
                }
            }
        }

        private fun buildNotification(context: Context, id: Long, type: String, label: String): Notification {
            val fullScreen = Intent(context, RingActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(AlarmScheduler.EXTRA_ALARM_ID, id)
                putExtra(AlarmScheduler.EXTRA_ALARM_TYPE, type)
                putExtra(AlarmScheduler.EXTRA_ALARM_LABEL, label)
            }
            val screenPending = PendingIntent.getActivity(
                context, (id % Int.MAX_VALUE).toInt(), fullScreen,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val stopPending = PendingIntent.getService(
                context, 9911, stopIntent(context),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_prabhat)
                .setContentTitle(label)
                .setContentText("Solve the wake-up challenge to dismiss")
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(screenPending)
                .setFullScreenIntent(screenPending, true)
                .addAction(0, "Stop alarm", stopPending)
                .setLocalOnly(true)
                .build()
        }
    }
}
