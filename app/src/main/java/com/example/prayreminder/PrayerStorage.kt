package com.example.prayreminder

import android.content.Context
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class PrayerStorage(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun savePrayerTimes(prayerTimes: PrayerTimes, fetchedAt: Instant = Instant.now()) {
        preferences.edit().apply {
            putString(KEY_DATE, prayerTimes.date.toString())
            Prayer.entries.forEach { prayer ->
                putString(timeKey(prayer), prayerTimes.timeFor(prayer)?.toString())
            }
            putLong(KEY_LAST_SUCCESS_AT, fetchedAt.toEpochMilli())
            apply()
        }
    }

    fun loadPrayerTimes(date: LocalDate): PrayerTimes? {
        if (preferences.getString(KEY_DATE, null) != date.toString()) return null

        val values = mutableMapOf<Prayer, LocalTime>()
        Prayer.entries.forEach { prayer ->
            val stored = preferences.getString(timeKey(prayer), null) ?: return null
            val time = runCatching { LocalTime.parse(stored) }.getOrNull() ?: return null
            values[prayer] = time
        }
        return PrayerTimes(date, values)
    }

    fun saveFetchStatus(message: String, attemptedAt: Instant = Instant.now()) {
        preferences.edit()
            .putString(KEY_LAST_FETCH_STATUS, message)
            .putLong(KEY_LAST_ATTEMPT_AT, attemptedAt.toEpochMilli())
            .apply()
    }

    fun lastFetchStatus(): String? = preferences.getString(KEY_LAST_FETCH_STATUS, null)

    fun lastAttemptAt(): Instant? {
        val value = preferences.getLong(KEY_LAST_ATTEMPT_AT, -1L)
        return value.takeIf { it >= 0 }?.let(Instant::ofEpochMilli)
    }

    fun saveAnswer(prayer: Prayer, answer: Boolean, timestamp: Instant = Instant.now()) {
        val date = timestamp.atZone(AppTime.ZONE).toLocalDate()
        val value = "${if (answer) "yes" else "no"}|${timestamp.toEpochMilli()}"
        preferences.edit().putString(answerKey(date, prayer), value).apply()
    }

    private fun timeKey(prayer: Prayer) = "time_${prayer.storageKey}"

    private fun answerKey(date: LocalDate, prayer: Prayer) =
        "answer_${date}_${prayer.storageKey}"

    companion object {
        private const val PREFERENCES_NAME = "prayer_reminder"
        private const val KEY_DATE = "prayer_date"
        private const val KEY_LAST_SUCCESS_AT = "last_success_at"
        private const val KEY_LAST_FETCH_STATUS = "last_fetch_status"
        private const val KEY_LAST_ATTEMPT_AT = "last_attempt_at"
    }
}
