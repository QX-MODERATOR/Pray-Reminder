package com.example.prayreminder

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri

enum class ReminderTone(
    val storageKey: String,
    val arabicLabel: String,
    val ringtoneType: Int,
) {
    SOFT(
        storageKey = "soft",
        arabicLabel = "هادئة",
        ringtoneType = RingtoneManager.TYPE_NOTIFICATION,
    ),
    SHORT(
        storageKey = "short",
        arabicLabel = "تنبيه قصير",
        ringtoneType = RingtoneManager.TYPE_NOTIFICATION,
    ),
    CLEAR(
        storageKey = "clear",
        arabicLabel = "تنبيه واضح",
        ringtoneType = RingtoneManager.TYPE_ALARM,
    ),
    ;

    fun soundUri(): Uri? =
        RingtoneManager.getDefaultUri(ringtoneType)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
}

data class ReminderSettings(
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val tone: ReminderTone = ReminderTone.SHORT,
)

class ReminderSettingsStorage(context: Context) {
    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun load(): ReminderSettings {
        val toneKey = preferences.getString(KEY_TONE, ReminderTone.SHORT.storageKey)
        return ReminderSettings(
            soundEnabled = preferences.getBoolean(KEY_SOUND_ENABLED, true),
            vibrationEnabled = preferences.getBoolean(KEY_VIBRATION_ENABLED, true),
            tone = ReminderTone.entries.firstOrNull { it.storageKey == toneKey }
                ?: ReminderTone.SHORT,
        )
    }

    fun save(settings: ReminderSettings) {
        preferences.edit()
            .putBoolean(KEY_SOUND_ENABLED, settings.soundEnabled)
            .putBoolean(KEY_VIBRATION_ENABLED, settings.vibrationEnabled)
            .putString(KEY_TONE, settings.tone.storageKey)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "reminder_settings"
        const val KEY_SOUND_ENABLED = "sound_enabled"
        const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        const val KEY_TONE = "reminder_tone"
    }
}
