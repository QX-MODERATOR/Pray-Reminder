package com.example.prayreminder

import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalTime

enum class Prayer(val storageKey: String, val arabicName: String) {
    FAJR("fajr", "الفجر"),
    SUNRISE("sunrise", "الشروق"),
    DHUHR("dhuhr", "الظهر"),
    ASR("asr", "العصر"),
    MAGHRIB("maghrib", "المغرب"),
    ISHA("isha", "العشاء");

    companion object {
        val reminderPrayers = listOf(FAJR, DHUHR, ASR, MAGHRIB, ISHA)

        fun fromPageName(value: String): Prayer? {
            val normalized = normalizeArabic(value)
            return entries.firstOrNull { normalizeArabic(it.arabicName) == normalized }
        }
    }
}

data class PrayerTimes(
    val date: LocalDate,
    val times: Map<Prayer, LocalTime>,
) {
    fun timeFor(prayer: Prayer): LocalTime? = times[prayer]

    fun reminderDateFor(prayer: Prayer): LocalDate =
        if (prayer == Prayer.ISHA) date.plusDays(1) else date

    fun reminderTimeFor(
        prayer: Prayer,
        nextDayFajr: LocalTime? = null,
    ): LocalTime? {
        // Reminder time is 15 minutes before the current prayer period ends.
        val periodEnd = when (prayer) {
            Prayer.FAJR -> times[Prayer.SUNRISE]
            Prayer.SUNRISE -> null
            Prayer.DHUHR -> times[Prayer.ASR]
            Prayer.ASR -> times[Prayer.MAGHRIB]
            Prayer.MAGHRIB -> times[Prayer.ISHA]
            Prayer.ISHA -> {
                // Isha reminder uses next day Fajr. If next day data is missing, today Fajr + 1 day is used as fallback.
                nextDayFajr ?: times[Prayer.FAJR]
            }
        }
        return periodEnd?.minusMinutes(15)
    }
}

fun normalizeArabic(value: String): String {
    val decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
    return buildString {
        decomposed.forEach { character ->
            val type = Character.getType(character)
            if (
                type != Character.NON_SPACING_MARK.toInt() &&
                type != Character.COMBINING_SPACING_MARK.toInt() &&
                character != '\u0640' &&
                Character.isLetter(character)
            ) {
                append(character)
            }
        }
    }
}
