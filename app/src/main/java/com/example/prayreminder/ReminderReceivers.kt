package com.example.prayreminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayer = intent.getStringExtra(AlarmScheduler.EXTRA_PRAYER)
            ?.let { key -> Prayer.entries.firstOrNull { it.storageKey == key } }
            ?: return
        if (ReminderOverlayService.canShow(context)) {
            ReminderOverlayService.start(context, prayer)
        } else {
            PrayerNotifications.show(context, prayer)
        }
    }
}

class PrayerActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayer = intent.getStringExtra(AlarmScheduler.EXTRA_PRAYER)
            ?.let { key -> Prayer.entries.firstOrNull { it.storageKey == key } }
            ?: return
        val answer = intent.getBooleanExtra(EXTRA_ANSWER, false)
        PrayerStorage(context).saveAnswer(prayer, answer)
        ReminderOverlayService.stop(context)
        PrayerNotifications.dismiss(context, prayer)
        context.sendBroadcast(
            Intent(AlarmScheduler.ACTION_ANSWERED)
                .setPackage(context.packageName)
                .putExtra(AlarmScheduler.EXTRA_PRAYER, prayer.storageKey),
        )
    }

    companion object {
        const val EXTRA_ANSWER = "answer"
    }
}

class MidnightRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        AppRefreshCoordinator.refreshInBackground(context) {
            pendingResult.finish()
        }
    }
}

class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        AppRefreshCoordinator.refreshInBackground(context) {
            pendingResult.finish()
        }
    }
}
