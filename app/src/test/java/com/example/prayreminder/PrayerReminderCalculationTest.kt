package com.example.prayreminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class PrayerReminderCalculationTest {
    private val prayerTimes = PrayerTimes(
        date = LocalDate.of(2026, 6, 21),
        times = mapOf(
            Prayer.FAJR to LocalTime.of(3, 51),
            Prayer.SUNRISE to LocalTime.of(5, 26),
            Prayer.DHUHR to LocalTime.of(12, 38),
            Prayer.ASR to LocalTime.of(16, 18),
            Prayer.MAGHRIB to LocalTime.of(19, 51),
            Prayer.ISHA to LocalTime.of(21, 26),
        ),
    )

    @Test
    fun remindersAreBeforeTheEndOfEachPrayerPeriod() {
        assertEquals(LocalTime.of(5, 11), prayerTimes.reminderTimeFor(Prayer.FAJR))
        assertEquals(LocalTime.of(16, 3), prayerTimes.reminderTimeFor(Prayer.DHUHR))
        assertEquals(LocalTime.of(19, 36), prayerTimes.reminderTimeFor(Prayer.ASR))
        assertEquals(LocalTime.of(21, 11), prayerTimes.reminderTimeFor(Prayer.MAGHRIB))
    }

    @Test
    fun ishaUsesTodayFajrAsFallbackWhenNextDayDataIsMissing() {
        assertEquals(
            LocalTime.of(3, 36),
            prayerTimes.reminderTimeFor(Prayer.ISHA),
        )
        assertEquals(
            LocalDate.of(2026, 6, 22),
            prayerTimes.reminderDateFor(Prayer.ISHA),
        )
    }

    @Test
    fun ishaPrefersRealNextDayFajrWhenAvailable() {
        assertEquals(
            LocalTime.of(3, 37),
            prayerTimes.reminderTimeFor(
                prayer = Prayer.ISHA,
                nextDayFajr = LocalTime.of(3, 52),
            ),
        )
    }

    @Test
    fun sunriseNeverGetsItsOwnReminder() {
        assertNull(prayerTimes.reminderTimeFor(Prayer.SUNRISE))
    }
}
