package com.example.prayreminder

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.prayreminder.ui.theme.Emerald400
import com.example.prayreminder.ui.theme.Gold400
import com.example.prayreminder.ui.theme.Midnight800
import com.example.prayreminder.ui.theme.Midnight850
import com.example.prayreminder.ui.theme.Midnight900
import com.example.prayreminder.ui.theme.Midnight950
import com.example.prayreminder.ui.theme.OutlineSoft
import com.example.prayreminder.ui.theme.PrayReminderTheme
import com.example.prayreminder.ui.theme.Rose400
import com.example.prayreminder.ui.theme.Teal400
import com.example.prayreminder.ui.theme.TextMuted
import com.example.prayreminder.ui.theme.TextPrimary
import com.example.prayreminder.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class ScreenContent {
    LOADING,
    ERROR,
    EMPTY,
    SUCCESS,
}

private data class UpcomingReminder(
    val prayer: Prayer,
    val prayerTime: LocalTime,
    val reminderTime: LocalTime,
    val triggerAt: Instant,
)

@Composable
fun PrayerReminderScreen(
    state: MainUiState,
    onRefresh: () -> Unit,
    onReschedule: () -> Unit,
    onEnableExactAlarms: () -> Unit,
    onRequestNotifications: () -> Unit,
    onEnableFullScreen: () -> Unit,
    onEnableOverlay: () -> Unit,
    onSaveReminderSettings: (ReminderSettings) -> Unit,
    onPreviewReminder: (ReminderSettings) -> Unit,
    onStopPreviewReminder: () -> Unit,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        var settingsVisible by rememberSaveable { mutableStateOf(false) }
        var previewVisible by rememberSaveable { mutableStateOf(false) }
        val backgroundBlur by animateDpAsState(
            targetValue = if (settingsVisible || previewVisible) 5.dp else 0.dp,
            animationSpec = tween(220),
            label = "ضبابية الخلفية",
        )

        PremiumBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(backgroundBlur)
                        .safeDrawingPadding(),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        HeaderSection(onSettingsClick = { settingsVisible = true })
                    }

                    item {
                        AnimatedVisibility(
                            visible = !state.exactAlarmsAllowed,
                            enter = fadeIn(tween(300)),
                            exit = fadeOut(tween(200)),
                        ) {
                            PermissionNotice(
                                title = "المنبهات الدقيقة غير مفعلة",
                                message = "فعّلها حتى تصل التذكيرات في الوقت المحدد.",
                                actionLabel = "تفعيل المنبهات الدقيقة",
                                onAction = onEnableExactAlarms,
                            )
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        item {
                            AnimatedVisibility(
                                visible = !state.notificationsAllowed,
                                enter = fadeIn(tween(300)),
                                exit = fadeOut(tween(200)),
                            ) {
                                PermissionNotice(
                                    title = "الإشعارات غير مفعلة",
                                    message = "امنح الإذن لعرض تذكيرات الصلاة.",
                                    actionLabel = "تفعيل الإشعارات",
                                    onAction = onRequestNotifications,
                                )
                            }
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        item {
                            AnimatedVisibility(
                                visible = !state.fullScreenAllowed,
                                enter = fadeIn(tween(300)),
                                exit = fadeOut(tween(200)),
                            ) {
                                PermissionNotice(
                                    title = "العرض بملء الشاشة غير مفعل",
                                    message = "سيبقى التذكير متاحاً كإشعار حتى تفعّل هذا الخيار.",
                                    actionLabel = "تفعيل العرض بملء الشاشة",
                                    onAction = onEnableFullScreen,
                                )
                            }
                        }
                    }

                    item {
                        AnimatedVisibility(
                            visible = !state.overlayAllowed,
                            enter = fadeIn(tween(300)),
                            exit = fadeOut(tween(200)),
                        ) {
                            PermissionNotice(
                                title = "الظهور فوق التطبيقات غير مفعل",
                                message = "فعّله حتى يظهر سؤال الصلاة أثناء استخدام التطبيقات الأخرى.",
                                actionLabel = "تفعيل الظهور فوق التطبيقات",
                                onAction = onEnableOverlay,
                            )
                        }
                    }

                    item {
                        val content = when {
                            state.loading && state.prayerTimes == null -> ScreenContent.LOADING
                            state.prayerTimes != null -> ScreenContent.SUCCESS
                            state.message.contains("تعذر") -> ScreenContent.ERROR
                            else -> ScreenContent.EMPTY
                        }
                        AnimatedContent(
                            targetState = content,
                            transitionSpec = {
                                fadeIn(tween(350)) togetherWith fadeOut(tween(220))
                            },
                            label = "حالة المواقيت",
                        ) { target ->
                            when (target) {
                                ScreenContent.LOADING -> LoadingPrayerCards()
                                ScreenContent.ERROR -> ErrorStateCard(onRetry = onRefresh)
                                ScreenContent.EMPTY -> EmptyStateCard()
                                ScreenContent.SUCCESS -> {
                                    val prayerTimes = state.prayerTimes
                                    if (prayerTimes != null) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(16.dp),
                                        ) {
                                            NextPrayerHeroCard(prayerTimes)
                                            PrayerTimesSection(prayerTimes)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        StatusSection(state)
                    }

                    item {
                        ActionButtons(
                            loading = state.loading,
                            canReschedule = state.prayerTimes != null && !state.loading,
                            onRefresh = onRefresh,
                            onReschedule = onReschedule,
                        )
                    }

                    item {
                        Spacer(Modifier.height(6.dp))
                    }
                }

                SettingsOverlay(
                    visible = settingsVisible,
                    settings = state.reminderSettings,
                    onDismiss = { settingsVisible = false },
                    onSettingsChanged = onSaveReminderSettings,
                    onSave = {
                        onSaveReminderSettings(state.reminderSettings)
                        settingsVisible = false
                    },
                    onPreview = {
                        previewVisible = true
                        onPreviewReminder(state.reminderSettings)
                    },
                )

                PreviewReminderOverlay(
                    visible = previewVisible,
                    onDismiss = {
                        onStopPreviewReminder()
                        previewVisible = false
                    },
                )
            }
        }
    }
}

@Composable
fun HeaderSection(onSettingsClick: () -> Unit = {}) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier = Modifier.fillMaxWidth()) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .absolutePadding(left = 62.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = "تذكير الصلاة",
                        style = MaterialTheme.typography.displaySmall,
                        color = TextPrimary,
                    )
                    Text(
                        text = formatArabicDate(LocalDate.now(AppTime.ZONE)),
                        style = MaterialTheme.typography.titleMedium,
                        color = Emerald400,
                    )
                    Text(
                        text = "مواقيت الصلاة والتنبيهات اليومية",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }
            SettingsIconButton(
                onClick = onSettingsClick,
                modifier = Modifier.align(Alignment.TopStart),
            )
        }
    }
}

@Composable
private fun SettingsIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.91f else 1f,
        animationSpec = tween(120),
        label = "ضغط الإعدادات",
    )
    Box(
        modifier = modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(10.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.11f),
                        Midnight850.copy(alpha = 0.95f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Emerald400.copy(alpha = 0.28f),
                        Color.White.copy(alpha = 0.04f),
                    ),
                ),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_settings),
            contentDescription = "الإعدادات",
            modifier = Modifier.size(22.dp),
            tint = Emerald400,
        )
    }
}

@Composable
private fun SettingsOverlay(
    visible: Boolean,
    settings: ReminderSettings,
    onDismiss: () -> Unit,
    onSettingsChanged: (ReminderSettings) -> Unit,
    onSave: () -> Unit,
    onPreview: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(tween(240)) + slideInVertically(
            animationSpec = tween(320),
            initialOffsetY = { it / 8 },
        ),
        exit = fadeOut(tween(180)) + slideOutVertically(
            animationSpec = tween(220),
            targetOffsetY = { it / 10 },
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.58f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
            SettingsSheet(
                settings = settings,
                onSettingsChanged = onSettingsChanged,
                onSave = onSave,
                onPreview = onPreview,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .safeDrawingPadding()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun SettingsSheet(
    settings: ReminderSettings,
    onSettingsChanged: (ReminderSettings) -> Unit,
    onSave: () -> Unit,
    onPreview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var savedNoticeVisible by remember { mutableStateOf(false) }
    LaunchedEffect(savedNoticeVisible) {
        if (savedNoticeVisible) {
            delay(1_600)
            savedNoticeVisible = false
        }
    }
    val saveSettings: (ReminderSettings) -> Unit = { updated ->
        onSettingsChanged(updated)
        savedNoticeVisible = true
    }
    val sheetInteractionSource = remember { MutableInteractionSource() }

    PremiumSurface(
        modifier = modifier
            .heightIn(max = 720.dp)
            .clickable(
                interactionSource = sheetInteractionSource,
                indication = null,
                onClick = {},
            ),
        shape = RoundedCornerShape(30.dp),
        elevation = 24.dp,
        borderBrush = Brush.linearGradient(
            listOf(
                Emerald400.copy(alpha = 0.42f),
                Color.White.copy(alpha = 0.05f),
            ),
        ),
        background = Brush.linearGradient(
            listOf(
                Color(0xFF162D35),
                Midnight850,
                Midnight900,
            ),
        ),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 46.dp, height = 5.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f)),
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "الإعدادات",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                )
                Text(
                    text = "خصّص طريقة تنبيهك للصلاة",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }

            SettingsToggleCard(
                title = "الصوت عند التذكير",
                description = "تشغيل صوت عند ظهور تذكير الصلاة",
                checked = settings.soundEnabled,
                onCheckedChange = {
                    saveSettings(settings.copy(soundEnabled = it))
                },
            )

            SettingsToggleCard(
                title = "الاهتزاز عند التذكير",
                description = "اهتزاز الجهاز عند وقت التذكير",
                checked = settings.vibrationEnabled,
                onCheckedChange = {
                    saveSettings(settings.copy(vibrationEnabled = it))
                },
            )

            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(
                    text = "نغمة التذكير",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                ReminderTone.entries.forEach { tone ->
                    ToneOptionCard(
                        tone = tone,
                        selected = tone == settings.tone,
                        onClick = {
                            if (tone != settings.tone) {
                                saveSettings(settings.copy(tone = tone))
                            }
                        },
                    )
                }
            }

            SettingsActionSection(
                title = "معاينة التذكير",
                buttonText = "تجربة التذكير",
                primary = true,
                onClick = onPreview,
            )

            SettingsActionSection(
                title = "حفظ الإعدادات",
                buttonText = "حفظ الإعدادات",
                primary = true,
                onClick = onSave,
            )

            AnimatedVisibility(
                visible = savedNoticeVisible,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(180)),
            ) {
                Text(
                    text = "تم حفظ الإعدادات",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Emerald400.copy(alpha = 0.10f))
                        .border(
                            1.dp,
                            Emerald400.copy(alpha = 0.16f),
                            RoundedCornerShape(14.dp),
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = Emerald400,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun SettingsToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.055f))
            .border(
                1.dp,
                Color.White.copy(alpha = 0.065f),
                RoundedCornerShape(20.dp),
            )
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 15.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Midnight950,
                checkedTrackColor = Emerald400,
                checkedBorderColor = Emerald400,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = Midnight800,
                uncheckedBorderColor = OutlineSoft,
            ),
        )
    }
}

@Composable
private fun ToneOptionCard(
    tone: ReminderTone,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = if (selected) Emerald400 else TextMuted
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(17.dp))
            .background(accent.copy(alpha = if (selected) 0.11f else 0.045f))
            .border(
                width = 1.dp,
                color = accent.copy(alpha = if (selected) 0.26f else 0.08f),
                shape = RoundedCornerShape(17.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = tone.arabicLabel,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) TextPrimary else TextSecondary,
        )
        Box(
            modifier = Modifier
                .size(19.dp)
                .clip(CircleShape)
                .border(1.5.dp, accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Emerald400),
                )
            }
        }
    }
}

@Composable
private fun SettingsActionSection(
    title: String,
    buttonText: String,
    primary: Boolean,
    onClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
        )
        AnimatedActionButton(
            text = buttonText,
            enabled = true,
            primary = primary,
            onClick = onClick,
        )
    }
}

@Composable
private fun PreviewReminderOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
) {
    val cardInteractionSource = remember { MutableInteractionSource() }
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(tween(220)) + slideInVertically(
            animationSpec = tween(300),
            initialOffsetY = { it / 12 },
        ),
        exit = fadeOut(tween(170)) + slideOutVertically(
            animationSpec = tween(210),
            targetOffsetY = { it / 12 },
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.66f))
                .safeDrawingPadding()
                .padding(22.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
            PremiumSurface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clickable(
                        interactionSource = cardInteractionSource,
                        indication = null,
                        onClick = {},
                    ),
                shape = RoundedCornerShape(30.dp),
                elevation = 24.dp,
                borderBrush = Brush.linearGradient(
                    listOf(
                        Emerald400.copy(alpha = 0.52f),
                        Color.White.copy(alpha = 0.05f),
                    ),
                ),
                background = Brush.linearGradient(
                    listOf(Color(0xFF183A39), Midnight850),
                ),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 25.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(Emerald400.copy(alpha = 0.13f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "ع",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Emerald400,
                        )
                    }
                    Text(
                        text = "هل صليت العصر؟",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Emerald400,
                                contentColor = Midnight950,
                            ),
                        ) {
                            Text("نعم", style = MaterialTheme.typography.labelLarge)
                        }
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, OutlineSoft),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = TextPrimary,
                            ),
                        ) {
                            Text("لا", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NextPrayerHeroCard(prayerTimes: PrayerTimes) {
    val now by produceState(initialValue = Instant.now()) {
        while (true) {
            value = Instant.now()
            delay(1_000)
        }
    }
    val upcoming = remember(prayerTimes, now.epochSecond) {
        findUpcomingReminder(prayerTimes, now)
    }
    val glowTransition = rememberInfiniteTransition(label = "وهج الصلاة القادمة")
    val glow by glowTransition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "شفافية الوهج",
    )

    PremiumSurface(
        modifier = Modifier.animateContentSize(),
        shape = RoundedCornerShape(30.dp),
        elevation = 18.dp,
        borderBrush = Brush.linearGradient(
            listOf(
                Emerald400.copy(alpha = glow),
                Teal400.copy(alpha = 0.12f),
                Gold400.copy(alpha = glow * 0.55f),
            ),
        ),
        background = Brush.linearGradient(
            listOf(
                Color(0xFF183B3B),
                Color(0xFF14273A),
                Color(0xFF151E32),
            ),
        ),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "الصلاة القادمة",
                style = MaterialTheme.typography.labelLarge,
                color = Emerald400,
            )

            AnimatedContent(
                targetState = upcoming,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                label = "الصلاة القادمة",
            ) { next ->
                if (next == null) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "اكتملت تنبيهات اليوم",
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary,
                        )
                        Text(
                            text = "نراك غداً بإذن الله",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = next.prayer.arabicName,
                            style = MaterialTheme.typography.displaySmall,
                            color = TextPrimary,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            HeroMetric(
                                label = "وقت الصلاة",
                                value = formatTime(next.prayerTime),
                                modifier = Modifier.weight(1f),
                            )
                            HeroMetric(
                                label = "وقت التذكير",
                                value = formatTime(next.reminderTime),
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White.copy(alpha = 0.055f))
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "الوقت المتبقي",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                            )
                            TimeValue(
                                text = formatCountdown(Duration.between(now, next.triggerAt)),
                                color = Gold400,
                                style = MaterialTheme.typography.titleLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.055f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
        )
        TimeValue(
            text = value,
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
fun PrayerTimesSection(prayerTimes: PrayerTimes) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(
            title = "مواقيت اليوم",
            subtitle = "التذكير قبل نهاية وقت الصلاة بخمس عشرة دقيقة",
        )
        Prayer.reminderPrayers.forEachIndexed { index, prayer ->
            PrayerTimeCard(
                prayer = prayer,
                prayerTime = prayerTimes.timeFor(prayer),
                reminderTime = prayerTimes.reminderTimeFor(prayer),
                accent = prayerAccent(index),
            )
        }
    }
}

@Composable
fun PrayerTimeCard(
    prayer: Prayer,
    prayerTime: LocalTime?,
    reminderTime: LocalTime?,
    accent: Color,
) {
    PremiumSurface(
        shape = RoundedCornerShape(24.dp),
        elevation = 10.dp,
        background = Brush.linearGradient(
            listOf(Midnight850, Midnight900),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(17.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = prayer.arabicName.take(1),
                    style = MaterialTheme.typography.titleLarge,
                    color = accent,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = prayer.arabicName,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                )
                TimeRow(
                    label = "وقت الصلاة",
                    value = prayerTime?.let(::formatTime) ?: "غير متاح",
                )
                TimeRow(
                    label = "وقت التذكير",
                    value = reminderTime?.let(::formatTime) ?: "غير متاح",
                    valueColor = accent,
                )
                if (prayer == Prayer.FAJR) {
                    Text(
                        text = "تذكير الفجر يعتمد على وقت الشروق",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeRow(
    label: String,
    value: String,
    valueColor: Color = TextPrimary,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.weight(1f),
        )
        TimePill(value = value, color = valueColor)
    }
}

@Composable
private fun TimePill(value: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.10f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.16f)),
    ) {
        TimeValue(
            text = value,
            color = color,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun TimeValue(
    text: String,
    color: Color,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Text(
            text = text,
            modifier = modifier,
            style = style,
            color = color,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Start,
        )
    }
}

@Composable
fun StatusSection(state: MainUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(
            title = "الحالة",
            subtitle = "جاهزية التنبيهات وآخر مزامنة",
        )
        PremiumSurface(
            shape = RoundedCornerShape(26.dp),
            elevation = 12.dp,
            background = Brush.linearGradient(listOf(Midnight850, Midnight900)),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatusChip(
                    label = "الإشعارات",
                    value = permissionText(state.notificationsAllowed),
                    positive = state.notificationsAllowed,
                )
                StatusChip(
                    label = "المنبهات الدقيقة",
                    value = permissionText(state.exactAlarmsAllowed),
                    positive = state.exactAlarmsAllowed,
                )
                StatusChip(
                    label = "العرض بملء الشاشة",
                    value = permissionText(state.fullScreenAllowed),
                    positive = state.fullScreenAllowed,
                )
                StatusChip(
                    label = "الظهور فوق التطبيقات",
                    value = permissionText(state.overlayAllowed),
                    positive = state.overlayAllowed,
                )
                StatusChip(
                    label = "آخر جلب",
                    value = state.message.ifBlank { "لم يتم بعد" },
                    positive = !state.message.contains("تعذر"),
                )
                state.lastAttemptAt?.let { attempt ->
                    StatusChip(
                        label = "وقت آخر محاولة",
                        value = formatAttemptTime(attempt),
                        positive = true,
                        neutral = true,
                    )
                }
            }
        }
    }
}

@Composable
fun StatusChip(
    label: String,
    value: String,
    positive: Boolean,
    neutral: Boolean = false,
) {
    val accent = when {
        neutral -> Teal400
        positive -> Emerald400
        else -> Rose400
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(alpha = 0.075f))
            .border(1.dp, accent.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .padding(horizontal = 13.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accent),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.45f),
        )
    }
}

@Composable
fun ActionButtons(
    loading: Boolean,
    canReschedule: Boolean,
    onRefresh: () -> Unit,
    onReschedule: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        AnimatedActionButton(
            text = if (loading) "جارٍ تحديث المواقيت" else "تحديث المواقيت",
            enabled = !loading,
            primary = true,
            loading = loading,
            onClick = onRefresh,
        )
        AnimatedActionButton(
            text = "إعادة جدولة التذكيرات",
            enabled = canReschedule,
            primary = false,
            onClick = onReschedule,
        )
    }
}

@Composable
private fun AnimatedActionButton(
    text: String,
    enabled: Boolean,
    primary: Boolean,
    loading: Boolean = false,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = tween(120),
        label = "ضغط الزر",
    )
    val elevation by animateDpAsState(
        targetValue = if (pressed) 2.dp else 9.dp,
        animationSpec = tween(120),
        label = "ارتفاع الزر",
    )
    val modifier = Modifier
        .fillMaxWidth()
        .height(56.dp)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .shadow(elevation, RoundedCornerShape(18.dp))

    if (primary) {
        Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            interactionSource = interactionSource,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Emerald400,
                contentColor = Midnight950,
                disabledContainerColor = OutlineSoft,
                disabledContentColor = TextMuted,
            ),
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Midnight950,
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.size(10.dp))
            }
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            interactionSource = interactionSource,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, OutlineSoft),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = TextPrimary,
                disabledContentColor = TextMuted,
            ),
        ) {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun LoadingPrayerCards() {
    val transition = rememberInfiniteTransition(label = "تحميل المواقيت")
    val alpha by transition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.62f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "وميض التحميل",
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PlaceholderCard(height = 205.dp, alpha = alpha)
        repeat(5) {
            PlaceholderCard(height = 126.dp, alpha = alpha)
        }
    }
}

@Composable
private fun PlaceholderCard(height: androidx.compose.ui.unit.Dp, alpha: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Midnight800.copy(alpha = alpha),
                        Midnight850.copy(alpha = alpha * 0.7f),
                    ),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.035f), RoundedCornerShape(24.dp)),
    )
}

@Composable
fun ErrorStateCard(onRetry: () -> Unit) {
    PremiumSurface(
        shape = RoundedCornerShape(26.dp),
        elevation = 12.dp,
        borderBrush = Brush.linearGradient(
            listOf(Rose400.copy(alpha = 0.35f), Color.Transparent),
        ),
        background = Brush.linearGradient(
            listOf(Color(0xFF2B1822), Midnight900),
        ),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "تعذر جلب مواقيت الصلاة",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
            Text(
                text = "تحقق من الاتصال بالإنترنت ثم حاول مرة أخرى",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            AnimatedActionButton(
                text = "إعادة المحاولة",
                enabled = true,
                primary = true,
                onClick = onRetry,
            )
        }
    }
}

@Composable
fun EmptyStateCard() {
    PremiumSurface(
        shape = RoundedCornerShape(26.dp),
        elevation = 10.dp,
        background = Brush.linearGradient(listOf(Midnight850, Midnight900)),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                text = "لا توجد مواقيت متاحة حالياً",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
            Text(
                text = "استخدم زر التحديث لجلب مواقيت اليوم.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun PermissionNotice(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    PremiumSurface(
        shape = RoundedCornerShape(22.dp),
        elevation = 8.dp,
        borderBrush = Brush.linearGradient(
            listOf(Gold400.copy(alpha = 0.28f), Color.Transparent),
        ),
        background = Brush.linearGradient(
            listOf(Color(0xFF28251E), Midnight900),
        ),
    ) {
        Column(
            modifier = Modifier.padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Gold400,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            OutlinedButton(
                onClick = onAction,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(15.dp),
                border = BorderStroke(1.dp, Gold400.copy(alpha = 0.32f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold400),
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
        )
    }
}

@Composable
internal fun PremiumBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0C1526),
                        Midnight950,
                        Color(0xFF080D18),
                    ),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(280.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Emerald400.copy(alpha = 0.10f), Color.Transparent),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(240.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Teal400.copy(alpha = 0.07f), Color.Transparent),
                    ),
                ),
        )
        content()
    }
}

@Composable
internal fun PremiumSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    elevation: androidx.compose.ui.unit.Dp = 10.dp,
    borderBrush: Brush = Brush.linearGradient(
        listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.025f)),
    ),
    background: Brush = Brush.linearGradient(listOf(Midnight850, Midnight900)),
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation, shape, clip = false)
            .clip(shape)
            .background(background)
            .border(1.dp, borderBrush, shape),
    ) {
        content()
    }
}

private fun findUpcomingReminder(prayerTimes: PrayerTimes, now: Instant): UpcomingReminder? {
    return Prayer.reminderPrayers.mapNotNull { prayer ->
        val prayerTime = prayerTimes.timeFor(prayer) ?: return@mapNotNull null
        val reminderTime = prayerTimes.reminderTimeFor(prayer) ?: return@mapNotNull null
        val triggerAt = prayerTimes.reminderDateFor(prayer)
            .atTime(reminderTime)
            .atZone(AppTime.ZONE)
            .toInstant()
        UpcomingReminder(prayer, prayerTime, reminderTime, triggerAt)
    }.filter { it.triggerAt.isAfter(now) }
        .minByOrNull { it.triggerAt }
}

private fun prayerAccent(index: Int): Color = when (index) {
    0 -> Teal400
    1 -> Gold400
    2 -> Emerald400
    3 -> Rose400
    else -> Color(0xFFA9A7FF)
}

private fun permissionText(allowed: Boolean) = if (allowed) "مفعل" else "غير مفعل"

private fun formatTime(time: LocalTime): String {
    val hour = when (val value = time.hour % 12) {
        0 -> 12
        else -> value
    }
    val marker = if (time.hour < 12) "ص" else "م"
    return String.format(Locale.ENGLISH, "%d:%02d %s", hour, time.minute, marker)
}

private fun formatCountdown(duration: Duration): String {
    val seconds = duration.seconds.coerceAtLeast(0)
    val hours = seconds / 3_600
    val minutes = (seconds % 3_600) / 60
    val remainingSeconds = seconds % 60
    return String.format(
        Locale.ENGLISH,
        "%02d:%02d:%02d",
        hours,
        minutes,
        remainingSeconds,
    )
}

private fun formatArabicDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEEE، d MMMM yyyy", Locale("ar", "JO")))

private fun formatAttemptTime(instant: Instant): String =
    instant.atZone(AppTime.ZONE).format(
        DateTimeFormatter.ofPattern("d MMM، h:mm a", Locale("ar", "JO")),
    )

private fun samplePrayerTimes(): PrayerTimes = PrayerTimes(
    date = LocalDate.now(AppTime.ZONE),
    times = mapOf(
        Prayer.FAJR to LocalTime.of(3, 51),
        Prayer.SUNRISE to LocalTime.of(5, 26),
        Prayer.DHUHR to LocalTime.of(12, 38),
        Prayer.ASR to LocalTime.of(16, 18),
        Prayer.MAGHRIB to LocalTime.of(19, 51),
        Prayer.ISHA to LocalTime.of(21, 26),
    ),
)

@Preview(showBackground = true, backgroundColor = 0xFF070B16)
@Composable
private fun SuccessPreview() {
    PrayReminderTheme {
        PrayerReminderScreen(
            state = MainUiState(
                prayerTimes = samplePrayerTimes(),
                message = "تم تحديث مواقيت الصلاة",
                lastAttemptAt = Instant.now(),
                notificationsAllowed = true,
                exactAlarmsAllowed = true,
                fullScreenAllowed = true,
                overlayAllowed = true,
            ),
            onRefresh = {},
            onReschedule = {},
            onEnableExactAlarms = {},
            onRequestNotifications = {},
            onEnableFullScreen = {},
            onEnableOverlay = {},
            onSaveReminderSettings = {},
            onPreviewReminder = {},
            onStopPreviewReminder = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF070B16)
@Composable
private fun LoadingPreview() {
    PrayReminderTheme {
        PrayerReminderScreen(
            state = MainUiState(loading = true),
            onRefresh = {},
            onReschedule = {},
            onEnableExactAlarms = {},
            onRequestNotifications = {},
            onEnableFullScreen = {},
            onEnableOverlay = {},
            onSaveReminderSettings = {},
            onPreviewReminder = {},
            onStopPreviewReminder = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF070B16)
@Composable
private fun ErrorPreview() {
    PrayReminderTheme {
        PrayerReminderScreen(
            state = MainUiState(message = "تعذر جلب مواقيت الصلاة"),
            onRefresh = {},
            onReschedule = {},
            onEnableExactAlarms = {},
            onRequestNotifications = {},
            onEnableFullScreen = {},
            onEnableOverlay = {},
            onSaveReminderSettings = {},
            onPreviewReminder = {},
            onStopPreviewReminder = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF070B16)
@Composable
private fun EmptyPreview() {
    PrayReminderTheme {
        PrayerReminderScreen(
            state = MainUiState(),
            onRefresh = {},
            onReschedule = {},
            onEnableExactAlarms = {},
            onRequestNotifications = {},
            onEnableFullScreen = {},
            onEnableOverlay = {},
            onSaveReminderSettings = {},
            onPreviewReminder = {},
            onStopPreviewReminder = {},
        )
    }
}
