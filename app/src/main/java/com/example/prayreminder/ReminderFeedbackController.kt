package com.example.prayreminder

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class ReminderFeedbackController(context: Context) {
    private val appContext = context.applicationContext
    private var ringtone: Ringtone? = null

    fun play(settings: ReminderSettings) {
        stop()
        if (settings.soundEnabled) playSound(settings.tone)
        if (settings.vibrationEnabled) vibrate()
    }

    fun stop() {
        ringtone?.stop()
        ringtone = null
        vibrator().cancel()
    }

    private fun playSound(tone: ReminderTone) {
        val soundUri = tone.soundUri() ?: return
        ringtone = RingtoneManager.getRingtone(appContext, soundUri)?.apply {
            audioAttributes = AudioAttributes.Builder()
                .setUsage(
                    if (tone == ReminderTone.CLEAR) {
                        AudioAttributes.USAGE_ALARM
                    } else {
                        AudioAttributes.USAGE_NOTIFICATION_EVENT
                    },
                )
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                isLooping = false
                volume = if (tone == ReminderTone.SOFT) 0.45f else 1f
            }
            play()
        }
    }

    private fun vibrate() {
        val vibrator = vibrator()
        if (!vibrator.hasVibrator()) return
        val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect.createWaveform(VIBRATION_PATTERN, -1)
        } else {
            null
        }

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                val attributes = VibrationAttributes.Builder()
                    .setUsage(VibrationAttributes.USAGE_NOTIFICATION)
                    .build()
                vibrator.vibrate(effect!!, attributes)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> vibrator.vibrate(effect!!)
            else -> {
                @Suppress("DEPRECATION")
                vibrator.vibrate(VIBRATION_PATTERN, -1)
            }
        }
    }

    private fun vibrator(): Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            appContext.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

    private companion object {
        val VIBRATION_PATTERN = longArrayOf(0L, 180L, 90L, 220L)
    }
}
