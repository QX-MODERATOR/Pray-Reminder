package com.example.prayreminder

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.prayreminder.ui.theme.PrayReminderTheme
import java.time.Instant

data class MainUiState(
    val prayerTimes: PrayerTimes? = null,
    val loading: Boolean = false,
    val message: String = "",
    val lastAttemptAt: Instant? = null,
    val notificationsAllowed: Boolean = false,
    val exactAlarmsAllowed: Boolean = false,
    val fullScreenAllowed: Boolean = true,
    val overlayAllowed: Boolean = false,
    val reminderSettings: ReminderSettings = ReminderSettings(),
)

class MainActivity : ComponentActivity() {
    private lateinit var repository: PrayerRepository
    private lateinit var scheduler: AlarmScheduler
    private lateinit var settingsStorage: ReminderSettingsStorage
    private lateinit var feedbackController: ReminderFeedbackController
    private var uiState by mutableStateOf(MainUiState())
    private var previousExactAlarmStatus: Boolean? = null
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        refreshVisibleState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        repository = PrayerRepository(this)
        scheduler = AlarmScheduler(this)
        settingsStorage = ReminderSettingsStorage(this)
        feedbackController = ReminderFeedbackController(this)
        val reminderSettings = settingsStorage.load()
        uiState = uiState.copy(reminderSettings = reminderSettings)
        PrayerNotifications.createChannel(this, reminderSettings)
        refreshVisibleState()

        setContent {
            PrayReminderTheme {
                PrayerReminderScreen(
                    state = uiState,
                    onRefresh = ::refreshPrayerTimes,
                    onReschedule = ::rescheduleFromCache,
                    onEnableExactAlarms = ::openExactAlarmSettings,
                    onRequestNotifications = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onEnableFullScreen = ::openFullScreenSettings,
                    onEnableOverlay = ::openOverlaySettings,
                    onSaveReminderSettings = ::saveReminderSettings,
                    onPreviewReminder = feedbackController::play,
                    onStopPreviewReminder = feedbackController::stop,
                )
            }
        }

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !notificationsAllowed()
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        refreshPrayerTimes()
    }

    override fun onDestroy() {
        if (::feedbackController.isInitialized) feedbackController.stop()
        super.onDestroy()
    }

    override fun onStop() {
        if (::feedbackController.isInitialized) feedbackController.stop()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (!::scheduler.isInitialized) return
        val allowedNow = scheduler.canScheduleExactAlarms()
        if (previousExactAlarmStatus == false && allowedNow) {
            repository.cachedForToday()?.let { scheduler.schedulePrayerReminders(it) }
        }
        previousExactAlarmStatus = allowedNow
        refreshVisibleState()
    }

    private fun refreshPrayerTimes() {
        uiState = uiState.copy(loading = true)
        AppRefreshCoordinator.refreshInBackground(this) { result ->
            runOnUiThread {
                uiState = uiState.copy(
                    prayerTimes = result.prayerTimes,
                    loading = false,
                    message = result.message,
                    lastAttemptAt = repository.lastAttemptAt(),
                    notificationsAllowed = notificationsAllowed(),
                    exactAlarmsAllowed = scheduler.canScheduleExactAlarms(),
                    fullScreenAllowed = fullScreenAllowed(),
                    overlayAllowed = overlayAllowed(),
                )
            }
        }
    }

    private fun rescheduleFromCache() {
        val cached = repository.cachedForToday()
        val message = when {
            cached == null -> "تعذر جلب مواقيت الصلاة"
            !scheduler.canScheduleExactAlarms() ->
                "فعّل المنبهات الدقيقة أولاً لإعادة الجدولة"
            scheduler.schedulePrayerReminders(cached) -> "تمت إعادة جدولة التذكيرات"
            else -> "تعذر إعادة جدولة التذكيرات"
        }
        scheduler.scheduleNextMidnightRefresh()
        uiState = uiState.copy(
            prayerTimes = cached,
            message = message,
            exactAlarmsAllowed = scheduler.canScheduleExactAlarms(),
        )
    }

    private fun refreshVisibleState() {
        if (!::repository.isInitialized || !::scheduler.isInitialized) return
        uiState = uiState.copy(
            prayerTimes = repository.cachedForToday(),
            message = repository.lastFetchStatus().orEmpty(),
            lastAttemptAt = repository.lastAttemptAt(),
            notificationsAllowed = notificationsAllowed(),
            exactAlarmsAllowed = scheduler.canScheduleExactAlarms(),
            fullScreenAllowed = fullScreenAllowed(),
            overlayAllowed = overlayAllowed(),
        )
    }

    private fun notificationsAllowed(): Boolean =
        NotificationManagerCompat.from(this).areNotificationsEnabled() &&
            (
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) == PackageManager.PERMISSION_GRANTED
                )

    private fun fullScreenAllowed(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        return getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
    }

    private fun overlayAllowed(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        startActivity(
            Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:$packageName"),
            ),
        )
    }

    private fun openFullScreenSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                Uri.parse("package:$packageName"),
            ),
        )
    }

    private fun openOverlaySettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"),
            ),
        )
    }

    private fun saveReminderSettings(settings: ReminderSettings) {
        settingsStorage.save(settings)
        PrayerNotifications.createChannel(this, settings)
        uiState = uiState.copy(reminderSettings = settings)
    }
}
