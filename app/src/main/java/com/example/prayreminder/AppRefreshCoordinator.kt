package com.example.prayreminder

import android.content.Context
import java.util.concurrent.Executors

object AppRefreshCoordinator {
    fun refreshInBackground(context: Context, onComplete: ((FetchResult) -> Unit)? = null) {
        val appContext = context.applicationContext
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            try {
                val result = PrayerRepository(appContext).fetchToday()
                val scheduler = AlarmScheduler(appContext)
                result.prayerTimes?.let { scheduler.schedulePrayerReminders(it) }
                scheduler.scheduleNextMidnightRefresh()
                onComplete?.invoke(result)
            } finally {
                executor.shutdown()
            }
        }
    }
}
