package com.example.prayreminder

import android.app.Application

class PrayerReminderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PrayerNotifications.createChannel(
            context = this,
            settings = ReminderSettingsStorage(this).load(),
        )
    }
}
