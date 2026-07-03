package com.example.prayreminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.time.LocalTime
import java.time.ZonedDateTime

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun schedulePrayerReminders(
        prayerTimes: PrayerTimes,
        nextDayPrayerTimes: PrayerTimes? = null,
    ): Boolean {
        cancelPrayerReminders()
        if (!canScheduleExactAlarms()) return false

        val now = java.time.Instant.now()
        return try {
            Prayer.reminderPrayers.forEach { prayer ->
                val nextDayFajr = if (prayer == Prayer.ISHA) {
                    nextDayPrayerTimes
                        ?.takeIf { it.date == prayerTimes.date.plusDays(1) }
                        ?.timeFor(Prayer.FAJR)
                } else {
                    null
                }
                val reminderTime = prayerTimes.reminderTimeFor(
                    prayer = prayer,
                    nextDayFajr = nextDayFajr,
                ) ?: return@forEach
                val triggerAt = prayerTimes.reminderDateFor(prayer)
                    .atTime(reminderTime)
                    .atZone(AppTime.ZONE)
                    .toInstant()
                if (triggerAt.isAfter(now)) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt.toEpochMilli(),
                        reminderPendingIntent(prayer, AlarmSlot.CURRENT_PERIOD),
                    )
                }
            }

            schedulePendingIshaReminder(prayerTimes, now)
            true
        } catch (error: SecurityException) {
            Log.e(TAG, "Exact alarm permission was revoked while scheduling", error)
            cancelPrayerReminders()
            false
        }
    }

    private fun schedulePendingIshaReminder(
        prayerTimes: PrayerTimes,
        now: java.time.Instant,
    ) {
        val todayFajr = prayerTimes.timeFor(Prayer.FAJR) ?: return

        // Reminder time is 15 minutes before the current prayer period ends.
        // Today's Fajr is the known end of the Isha period that began yesterday.
        // This is scheduled after the daily fetch makes that next-day Fajr known.
        val reminderTime = prayerTimes.reminderTimeFor(
            prayer = Prayer.ISHA,
            nextDayFajr = todayFajr,
        ) ?: return
        val triggerAt = prayerTimes.date
            .atTime(reminderTime)
            .atZone(AppTime.ZONE)
            .toInstant()

        if (triggerAt.isAfter(now)) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt.toEpochMilli(),
                reminderPendingIntent(Prayer.ISHA, AlarmSlot.CARRYOVER_ISHA),
            )
        }
    }

    fun scheduleNextMidnightRefresh() {
        val now = ZonedDateTime.now(AppTime.ZONE)
        val nextRefresh = now.toLocalDate()
            .plusDays(1)
            .atTime(LocalTime.of(0, 5))
            .atZone(AppTime.ZONE)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MIDNIGHT_REQUEST_CODE,
            Intent(context, MidnightRefreshReceiver::class.java)
                .setAction(ACTION_MIDNIGHT_REFRESH),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        try {
            if (canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextRefresh.toInstant().toEpochMilli(),
                    pendingIntent,
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextRefresh.toInstant().toEpochMilli(),
                    pendingIntent,
                )
            }
        } catch (error: SecurityException) {
            Log.e(TAG, "Unable to schedule midnight refresh", error)
            runCatching {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextRefresh.toInstant().toEpochMilli(),
                    pendingIntent,
                )
            }.onFailure {
                Log.e(TAG, "Unable to schedule fallback midnight refresh", it)
            }
        }
    }

    private fun cancelPrayerReminders() {
        Prayer.reminderPrayers.forEach { prayer ->
            alarmManager.cancel(reminderPendingIntent(prayer, AlarmSlot.CURRENT_PERIOD))
        }
        alarmManager.cancel(reminderPendingIntent(Prayer.ISHA, AlarmSlot.CARRYOVER_ISHA))
    }

    private fun reminderPendingIntent(
        prayer: Prayer,
        slot: AlarmSlot,
    ): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java)
            .setAction("$ACTION_PRAYER_REMINDER.${prayer.storageKey}.${slot.actionSuffix}")
            .putExtra(EXTRA_PRAYER, prayer.storageKey)
        val requestCode = when (slot) {
            AlarmSlot.CURRENT_PERIOD -> PRAYER_REQUEST_CODE_BASE + prayer.ordinal
            AlarmSlot.CARRYOVER_ISHA -> ISHA_CARRYOVER_REQUEST_CODE
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private enum class AlarmSlot(val actionSuffix: String) {
        CURRENT_PERIOD("current"),
        CARRYOVER_ISHA("carryover"),
    }

    companion object {
        private const val TAG = "AlarmScheduler"
        const val EXTRA_PRAYER = "prayer"
        const val ACTION_ANSWERED = "com.example.prayreminder.ACTION_ANSWERED"
        private const val ACTION_PRAYER_REMINDER =
            "com.example.prayreminder.ACTION_PRAYER_REMINDER"
        private const val ACTION_MIDNIGHT_REFRESH =
            "com.example.prayreminder.ACTION_MIDNIGHT_REFRESH"
        private const val PRAYER_REQUEST_CODE_BASE = 1_000
        private const val ISHA_CARRYOVER_REQUEST_CODE = 1_100
        private const val MIDNIGHT_REQUEST_CODE = 2_000
    }
}
