package com.example.prayreminder

import android.content.Context
import android.util.Log
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

object AppTime {
    val ZONE = java.time.ZoneId.of("Asia/Amman")
}

data class FetchResult(
    val prayerTimes: PrayerTimes?,
    val message: String,
    val usedCache: Boolean,
)

class PrayerRepository(context: Context) {
    private val storage = PrayerStorage(context)

    fun cachedForToday(): PrayerTimes? =
        storage.loadPrayerTimes(LocalDate.now(AppTime.ZONE))

    fun lastFetchStatus(): String? = storage.lastFetchStatus()

    fun lastAttemptAt(): Instant? = storage.lastAttemptAt()

    fun fetchToday(): FetchResult {
        val today = LocalDate.now(AppTime.ZONE)
        return try {
            val document = Jsoup.connect(PRAYER_TIMES_URL)
                .userAgent("Mozilla/5.0 (Android) PrayReminder/1.0")
                .timeout(10_000)
                .get()
            val parsed = parseDocument(document, today)
            storage.savePrayerTimes(parsed)
            val message = "تم تحديث مواقيت الصلاة"
            storage.saveFetchStatus(message)
            FetchResult(parsed, message, usedCache = false)
        } catch (error: Exception) {
            Log.e(TAG, "Unable to fetch or parse prayer times", error)
            val cached = storage.loadPrayerTimes(today)
            val message = if (cached != null) {
                "تعذر التحديث، تم استخدام المواقيت المحفوظة"
            } else {
                "تعذر جلب مواقيت الصلاة"
            }
            storage.saveFetchStatus(message)
            FetchResult(cached, message, usedCache = cached != null)
        }
    }

    internal fun parseDocument(document: Document, date: LocalDate): PrayerTimes {
        val parsed = mutableMapOf<Prayer, LocalTime>()
        document.select("ul.todayprayer li.onprayer").forEach { item ->
            val prayer = Prayer.fromPageName(item.selectFirst("div.temp")?.text().orEmpty())
                ?: return@forEach
            val rawTime = item.selectFirst("time")?.text().orEmpty()
            parseTwelveHourTime(rawTime)?.let { parsed[prayer] = it }
        }

        val missing = Prayer.entries.filterNot(parsed::containsKey)
        require(missing.isEmpty()) {
            "Malformed prayer HTML; missing: ${missing.joinToString { it.storageKey }}"
        }
        return PrayerTimes(date, parsed)
    }

    internal fun parseTwelveHourTime(value: String): LocalTime? {
        val normalized = value
            .replace('\u00A0', ' ')
            .trim()
            .uppercase(Locale.ENGLISH)
        val match = TIME_PATTERN.matchEntire(normalized) ?: return null
        val hour12 = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].toIntOrNull() ?: return null
        if (hour12 !in 1..12 || minute !in 0..59) return null

        val isPm = match.groupValues[3] == "P"
        val hour24 = when {
            hour12 == 12 && !isPm -> 0
            hour12 == 12 -> 12
            isPm -> hour12 + 12
            else -> hour12
        }
        return LocalTime.of(hour24, minute)
    }

    companion object {
        private const val TAG = "PrayerRepository"
        private const val PRAYER_TIMES_URL =
            "https://timesprayer.com/prayer-times-cities-jordan.html"
        private val TIME_PATTERN =
            Regex("""^\s*(\d{1,2})\s*:\s*(\d{2})\s*([AP])\.?\s*M\.?\s*$""")
    }
}
