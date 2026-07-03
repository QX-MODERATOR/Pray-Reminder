# 🕌 Pray Reminder — تذكير الصلاة

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="120" alt="Pray Reminder Icon"/>
</p>

<p align="center">
  <a href="https://github.com/QX-MODERATOR/Pray-Reminder/releases/latest">
    <img src="https://img.shields.io/github/v/release/QX-MODERATOR/Pray-Reminder?label=Latest%20Release&color=4CAF50&style=for-the-badge" alt="Latest Release"/>
  </a>
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android" alt="Android"/>
  <img src="https://img.shields.io/badge/Min%20SDK-24%20(Android%207.0)-blue?style=for-the-badge" alt="Min SDK 24"/>
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge" alt="Jetpack Compose"/>
</p>

---

## 📖 About

**Pray Reminder** (تذكير الصلاة) is a beautifully designed Android app that helps Muslims stay consistent with their daily prayers. It automatically fetches accurate prayer times for your city and delivers timely reminders — even when your screen is off or the app is closed.

---

## ✨ Features

- 🕐 **Accurate Prayer Times** — Fetches real-time prayer schedules for your city from online sources
- 🔔 **Smart Notifications** — Full-screen reminders with adhan notification at each prayer time
- 📴 **Works Offline (after first load)** — Caches prayer times locally so you never miss a prayer without internet
- 🌙 **Lock Screen Reminders** — Shows a dedicated reminder screen directly on your lock screen
- 🔁 **Boot Persistence** — Automatically restores alarms after device restart
- 🌐 **Midnight Auto-Refresh** — Updates prayer times at midnight for the next day automatically
- ⚙️ **Flexible Settings** — Enable/disable reminders per prayer, adjust timing and behavior
- 🎨 **Modern UI** — Built with Jetpack Compose using a clean Material 3 design
- 📱 **RTL Support** — Fully supports Arabic and right-to-left layouts

---

## 📲 Download

👉 **[Download the latest APK from Releases](https://github.com/QX-MODERATOR/Pray-Reminder/releases/latest)**

> No Google Play required — direct APK install.

---

## 🛠️ Tech Stack

| Technology | Purpose |
|---|---|
| **Kotlin** | Primary language |
| **Jetpack Compose** | UI framework |
| **Material 3** | Design system |
| **AlarmManager** | Exact alarm scheduling |
| **WorkManager / BroadcastReceiver** | Background tasks & boot restore |
| **Jsoup** | HTML parsing for prayer time scraping |
| **DataStore / SharedPreferences** | Local settings & cache storage |
| **Foreground Service** | Lock screen overlay delivery |

---

## 🏗️ Project Structure

```
app/src/main/java/com/example/prayreminder/
├── MainActivity.kt               # Entry point & navigation
├── PrayerModels.kt               # Data models (Prayer, PrayerTime, etc.)
├── PrayerRepository.kt           # Fetches & caches prayer times
├── PrayerStorage.kt              # Local persistence layer
├── PrayerReminderApplication.kt  # Application class
├── PrayerReminderUi.kt           # Main Composable UI screens
├── AlarmScheduler.kt             # Schedules exact alarms
├── AppRefreshCoordinator.kt      # Coordinates daily data refresh
├── ReminderActivity.kt           # Full-screen reminder screen
├── ReminderOverlayService.kt     # Foreground service for lock screen
├── ReminderFeedbackController.kt # Handles user prayer action (done/snooze)
├── ReminderReceivers.kt          # BroadcastReceivers (alarm, boot, etc.)
├── ReminderSettings.kt           # Settings model & storage
├── PrayerNotifications.kt        # Notification builder & channel setup
└── ui/
    ├── Color.kt                  # Color palette
    ├── Theme.kt                  # Material 3 theme
    └── Type.kt                   # Typography
```

---

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog or newer
- Android SDK 36
- JDK 11+

### Build & Run

```bash
# Clone the repository
git clone https://github.com/QX-MODERATOR/Pray-Reminder.git
cd Pray-Reminder

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

### Install on Device

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 📋 Permissions

| Permission | Reason |
|---|---|
| `INTERNET` | Fetch prayer times from online source |
| `POST_NOTIFICATIONS` | Display prayer reminder notifications |
| `RECEIVE_BOOT_COMPLETED` | Restore alarms after device reboot |
| `SCHEDULE_EXACT_ALARM` | Trigger reminders at precise prayer times |
| `FOREGROUND_SERVICE` | Run lock screen overlay service |
| `SYSTEM_ALERT_WINDOW` | Display reminder over lock screen |
| `USE_FULL_SCREEN_INTENT` | Full-screen reminder when screen is off |
| `VIBRATE` | Vibrate on reminder |

---

## 🤝 Contributing

Contributions are welcome! Feel free to open an issue or submit a pull request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

This project is open source. See [LICENSE](LICENSE) for more details.

---

<p align="center">Made with ❤️ for the Muslim community</p>
