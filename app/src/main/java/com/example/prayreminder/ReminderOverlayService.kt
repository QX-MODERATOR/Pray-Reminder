package com.example.prayreminder

import android.app.KeyguardManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

class ReminderOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var overlayView: View? = null

    private val timeoutRunnable = Runnable {
        removeOverlay()
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WindowManager::class.java)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prayer = intent?.getStringExtra(AlarmScheduler.EXTRA_PRAYER)
            ?.let { key -> Prayer.entries.firstOrNull { it.storageKey == key } }
            ?: run {
                stopSelf()
                return START_NOT_STICKY
            }
        val notification = PrayerNotifications.buildNotification(
            context = this,
            prayer = prayer,
            ongoing = true,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                PrayerNotifications.notificationId(prayer),
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE,
            )
        } else {
            startForeground(PrayerNotifications.notificationId(prayer), notification)
        }

        if (!Settings.canDrawOverlays(this)) {
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
            return START_NOT_STICKY
        }

        showOverlay(prayer)
        handler.removeCallbacks(timeoutRunnable)
        handler.postDelayed(timeoutRunnable, OVERLAY_TIMEOUT_MILLIS)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(timeoutRunnable)
        removeOverlay()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showOverlay(prayer: Prayer) {
        removeOverlay()
        val view = createOverlayView(prayer)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.CENTER
        }

        runCatching {
            windowManager.addView(view, params)
            overlayView = view
        }.onFailure {
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
        }
    }

    private fun createOverlayView(prayer: Prayer): View {
        val root = FrameLayout(this).apply {
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(Color.argb(190, 0, 0, 0))
            isClickable = true
            isFocusable = true
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(24), dp(28), dp(24), dp(28))
            elevation = dp(24).toFloat()
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(24, 58, 57), Color.rgb(16, 25, 42)),
            ).apply {
                cornerRadius = dp(32).toFloat()
                setStroke(dp(1), Color.argb(145, 81, 214, 162))
            }
        }
        root.addView(
            card,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER,
            ).apply {
                marginStart = dp(22)
                marginEnd = dp(22)
            },
        )

        val prayerCircle = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(34, 81, 214, 162))
            }
        }
        prayerCircle.addView(
            styledText(
                text = prayer.arabicName.take(1),
                sizeSp = 30f,
                color = EMERALD,
                bold = true,
                gravity = Gravity.CENTER,
            ),
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
        card.addView(
            prayerCircle,
            LinearLayout.LayoutParams(dp(70), dp(70)).apply {
                bottomMargin = dp(20)
            },
        )

        card.addView(
            styledText(
                text = "تذكير الصلاة",
                sizeSp = 17f,
                color = EMERALD,
                bold = true,
                gravity = Gravity.CENTER,
            ),
            fullWidthParams(bottomMargin = dp(8)),
        )
        card.addView(
            styledText(
                text = "هل صليت ${prayer.arabicName}؟",
                sizeSp = 28f,
                color = TEXT_PRIMARY,
                bold = true,
                gravity = Gravity.CENTER,
            ),
            fullWidthParams(bottomMargin = dp(9)),
        )
        card.addView(
            styledText(
                text = "سجّل إجابتك لتبقى متابعاً لصلواتك اليومية",
                sizeSp = 14f,
                color = TEXT_SECONDARY,
                gravity = Gravity.CENTER,
            ),
            fullWidthParams(bottomMargin = dp(24)),
        )

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            weightSum = 2f
        }
        buttons.addView(
            answerButton(
                text = "نعم",
                primary = true,
                onClick = { answer(prayer, true) },
            ),
            LinearLayout.LayoutParams(0, dp(54), 1f).apply {
                marginEnd = dp(6)
            },
        )
        buttons.addView(
            answerButton(
                text = "لا",
                primary = false,
                onClick = { answer(prayer, false) },
            ),
            LinearLayout.LayoutParams(0, dp(54), 1f).apply {
                marginStart = dp(6)
            },
        )
        card.addView(
            buttons,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        return root
    }

    private fun answer(prayer: Prayer, answer: Boolean) {
        PrayerStorage(this).saveAnswer(prayer, answer)
        PrayerNotifications.dismiss(this, prayer)
        sendBroadcast(
            Intent(AlarmScheduler.ACTION_ANSWERED)
                .setPackage(packageName)
                .putExtra(AlarmScheduler.EXTRA_PRAYER, prayer.storageKey),
        )
        removeOverlay()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun removeOverlay() {
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlayView = null
    }

    private fun answerButton(
        text: String,
        primary: Boolean,
        onClick: () -> Unit,
    ): TextView = styledText(
        text = text,
        sizeSp = 15f,
        color = if (primary) MIDNIGHT else TEXT_PRIMARY,
        bold = true,
        gravity = Gravity.CENTER,
    ).apply {
        isClickable = true
        isFocusable = true
        background = GradientDrawable().apply {
            cornerRadius = dp(18).toFloat()
            if (primary) {
                setColor(EMERALD)
            } else {
                setColor(Color.TRANSPARENT)
                setStroke(dp(1), Color.rgb(43, 56, 80))
            }
        }
        setOnClickListener { onClick() }
    }

    private fun styledText(
        text: String,
        sizeSp: Float,
        color: Int,
        bold: Boolean = false,
        gravity: Int,
    ): TextView = TextView(this).apply {
        this.text = text
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextColor(color)
        this.gravity = gravity
        textDirection = View.TEXT_DIRECTION_RTL
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    private fun fullWidthParams(bottomMargin: Int) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    ).apply {
        this.bottomMargin = bottomMargin
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val OVERLAY_TIMEOUT_MILLIS = 165_000L
        private val EMERALD = Color.rgb(81, 214, 162)
        private val TEXT_PRIMARY = Color.rgb(244, 247, 251)
        private val TEXT_SECONDARY = Color.rgb(174, 185, 204)
        private val MIDNIGHT = Color.rgb(7, 11, 22)

        fun canShow(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
            if (!Settings.canDrawOverlays(context)) return false
            val powerManager = context.getSystemService(PowerManager::class.java)
            val keyguardManager = context.getSystemService(KeyguardManager::class.java)
            return powerManager.isInteractive && !keyguardManager.isKeyguardLocked
        }

        fun start(context: Context, prayer: Prayer) {
            val intent = Intent(context, ReminderOverlayService::class.java)
                .putExtra(AlarmScheduler.EXTRA_PRAYER, prayer.storageKey)
            runCatching {
                ContextCompat.startForegroundService(context, intent)
            }.onFailure {
                PrayerNotifications.show(context, prayer)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ReminderOverlayService::class.java))
        }
    }
}
