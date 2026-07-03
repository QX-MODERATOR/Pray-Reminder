package com.example.prayreminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.prayreminder.ui.theme.Emerald400
import com.example.prayreminder.ui.theme.Midnight850
import com.example.prayreminder.ui.theme.Midnight950
import com.example.prayreminder.ui.theme.OutlineSoft
import com.example.prayreminder.ui.theme.PrayReminderTheme
import com.example.prayreminder.ui.theme.TextPrimary
import com.example.prayreminder.ui.theme.TextSecondary

class ReminderActivity : ComponentActivity() {
    private var prayer: Prayer? = null

    private val answerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val answeredKey = intent.getStringExtra(AlarmScheduler.EXTRA_PRAYER)
            if (answeredKey == prayer?.storageKey) finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }

        prayer = intent.getStringExtra(AlarmScheduler.EXTRA_PRAYER)
            ?.let { key -> Prayer.entries.firstOrNull { it.storageKey == key } }
        val currentPrayer = prayer
        if (currentPrayer == null || currentPrayer == Prayer.SUNRISE) {
            finish()
            return
        }
        ReminderOverlayService.stop(this)

        setContent {
            PrayReminderTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    ReminderScreen(
                        prayer = currentPrayer,
                        onAnswer = { answer ->
                            PrayerStorage(this).saveAnswer(currentPrayer, answer)
                            PrayerNotifications.dismiss(this, currentPrayer)
                            finish()
                        },
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            this,
            answerReceiver,
            IntentFilter(AlarmScheduler.ACTION_ANSWERED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onStop() {
        unregisterReceiver(answerReceiver)
        super.onStop()
    }
}

@Composable
private fun ReminderScreen(prayer: Prayer, onAnswer: (Boolean) -> Unit) {
    PremiumBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(22.dp),
            contentAlignment = Alignment.Center,
        ) {
            PremiumSurface(
                shape = RoundedCornerShape(32.dp),
                elevation = 22.dp,
                borderBrush = Brush.linearGradient(
                    listOf(
                        Emerald400.copy(alpha = 0.55f),
                        Color.White.copy(alpha = 0.05f),
                    ),
                ),
                background = Brush.linearGradient(
                    listOf(Color(0xFF183A39), Midnight850),
                ),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(Emerald400.copy(alpha = 0.13f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = prayer.arabicName.take(1),
                            style = MaterialTheme.typography.displaySmall,
                            color = Emerald400,
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "تذكير الصلاة",
                            style = MaterialTheme.typography.titleMedium,
                            color = Emerald400,
                        )
                        Text(
                            text = "هل صليت ${prayer.arabicName}؟",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = "سجّل إجابتك لتبقى متابعاً لصلواتك اليومية",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ReminderAnswerButton(
                            text = "نعم",
                            primary = true,
                            onClick = { onAnswer(true) },
                            modifier = Modifier.weight(1f),
                        )
                        ReminderAnswerButton(
                            text = "لا",
                            primary = false,
                            onClick = { onAnswer(false) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderAnswerButton(
    text: String,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(120),
        label = "ضغط الإجابة",
    )
    val elevation by animateDpAsState(
        targetValue = if (pressed) 2.dp else 10.dp,
        animationSpec = tween(120),
        label = "ارتفاع الإجابة",
    )
    val animatedModifier = modifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .shadow(elevation, RoundedCornerShape(18.dp))

    if (primary) {
        Button(
            onClick = onClick,
            modifier = animatedModifier,
            interactionSource = interactionSource,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Emerald400,
                contentColor = Midnight950,
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 15.dp),
        ) {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = animatedModifier,
            interactionSource = interactionSource,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, OutlineSoft),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 15.dp),
        ) {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}
