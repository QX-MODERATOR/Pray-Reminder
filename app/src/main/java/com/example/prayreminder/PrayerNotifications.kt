package com.example.prayreminder

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object PrayerNotifications {
    private const val CHANNEL_SOUND_VIBRATE = "prayer_reminder_sound_vibrate"
    private const val CHANNEL_SOUND_ONLY = "prayer_reminder_sound_only"
    private const val CHANNEL_VIBRATE_ONLY = "prayer_reminder_vibrate_only"
    private const val CHANNEL_SILENT = "prayer_reminder_silent"
    private const val NOTIFICATION_ID_BASE = 3_000
    private val VIBRATION_PATTERN = longArrayOf(0L, 250L, 100L, 300L)

    fun createChannel(
        context: Context,
        settings: ReminderSettings = ReminderSettingsStorage(context).load(),
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val soundEnabled = settings.soundEnabled
        val vibrationEnabled = settings.vibrationEnabled
        val channel = NotificationChannel(
            channelId(settings),
            channelName(soundEnabled, vibrationEnabled, settings.tone),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "تنبيهات مواقيت الصلاة"
            setSound(
                if (soundEnabled) settings.tone.soundUri() else null,
                if (soundEnabled) notificationAudioAttributes(settings.tone) else null,
            )
            enableVibration(vibrationEnabled)
            vibrationPattern = if (vibrationEnabled) VIBRATION_PATTERN else null
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun show(context: Context, prayer: Prayer) {
        val notificationManager = NotificationManagerCompat.from(context)
        if (
            !notificationManager.areNotificationsEnabled() ||
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notification = buildNotification(context, prayer, ongoing = false)

        try {
            notificationManager.notify(notificationId(prayer), notification)
        } catch (error: SecurityException) {
            Log.e(TAG, "Notification permission was revoked before delivery", error)
        }
    }

    internal fun buildNotification(
        context: Context,
        prayer: Prayer,
        ongoing: Boolean,
    ): Notification {
        val settings = ReminderSettingsStorage(context).load()
        createChannel(context, settings)
        val question = "هل صليت ${prayer.arabicName}؟"
        val fullScreenIntent = Intent(context, ReminderActivity::class.java)
            .putExtra(AlarmScheduler.EXTRA_PRAYER, prayer.storageKey)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_BASE + prayer.ordinal,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, channelId(settings))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("تذكير الصلاة")
            .setContentText(question)
            .setStyle(NotificationCompat.BigTextStyle().bigText(question))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(!ongoing)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(0, "نعم", answerPendingIntent(context, prayer, true))
            .addAction(0, "لا", answerPendingIntent(context, prayer, false))
            .apply {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    setSound(if (settings.soundEnabled) settings.tone.soundUri() else null)
                    setVibrate(
                        if (settings.vibrationEnabled) {
                            VIBRATION_PATTERN
                        } else {
                            longArrayOf(0L)
                        },
                    )
                }
            }
            .build()
    }

    fun dismiss(context: Context, prayer: Prayer) {
        NotificationManagerCompat.from(context).cancel(notificationId(prayer))
    }

    private fun answerPendingIntent(
        context: Context,
        prayer: Prayer,
        answer: Boolean,
    ): PendingIntent {
        val requestCode = NOTIFICATION_ID_BASE + (prayer.ordinal * 2) + if (answer) 1 else 2
        val intent = Intent(context, PrayerActionReceiver::class.java)
            .setAction("com.example.prayreminder.ANSWER.${prayer.storageKey}.$answer")
            .putExtra(AlarmScheduler.EXTRA_PRAYER, prayer.storageKey)
            .putExtra(PrayerActionReceiver.EXTRA_ANSWER, answer)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    internal fun notificationId(prayer: Prayer) = NOTIFICATION_ID_BASE + prayer.ordinal

    private fun channelId(settings: ReminderSettings): String {
        val baseId = when {
            settings.soundEnabled && settings.vibrationEnabled -> CHANNEL_SOUND_VIBRATE
            settings.soundEnabled -> CHANNEL_SOUND_ONLY
            settings.vibrationEnabled -> CHANNEL_VIBRATE_ONLY
            else -> CHANNEL_SILENT
        }
        return if (settings.soundEnabled && settings.tone != ReminderTone.SHORT) {
            "${baseId}_${settings.tone.storageKey}"
        } else {
            baseId
        }
    }

    private fun channelName(
        soundEnabled: Boolean,
        vibrationEnabled: Boolean,
        tone: ReminderTone,
    ): String = when {
        soundEnabled && vibrationEnabled ->
            "تذكيرات الصلاة: ${tone.arabicLabel} واهتزاز"
        soundEnabled -> "تذكيرات الصلاة: ${tone.arabicLabel}"
        vibrationEnabled -> "تذكيرات الصلاة: اهتزاز"
        else -> "تذكيرات الصلاة: صامتة"
    }

    private fun notificationAudioAttributes(tone: ReminderTone): AudioAttributes =
        AudioAttributes.Builder()
            .setUsage(
                if (tone == ReminderTone.CLEAR) {
                    AudioAttributes.USAGE_ALARM
                } else {
                    AudioAttributes.USAGE_NOTIFICATION_EVENT
                },
            )
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

    private const val TAG = "PrayerNotifications"
}
