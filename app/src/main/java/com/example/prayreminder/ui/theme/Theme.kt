package com.example.prayreminder.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val PrayerColorScheme = darkColorScheme(
    primary = Emerald400,
    onPrimary = Midnight950,
    primaryContainer = Midnight800,
    onPrimaryContainer = TextPrimary,
    secondary = Teal400,
    onSecondary = Midnight950,
    secondaryContainer = Slate700,
    onSecondaryContainer = TextPrimary,
    tertiary = Gold400,
    onTertiary = Midnight950,
    background = Midnight950,
    onBackground = TextPrimary,
    surface = Midnight900,
    onSurface = TextPrimary,
    surfaceVariant = Midnight850,
    onSurfaceVariant = TextSecondary,
    outline = OutlineSoft,
    error = Rose400,
    onError = Midnight950,
)

private val PrayerShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun PrayReminderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PrayerColorScheme,
        typography = Typography,
        shapes = PrayerShapes,
        content = content,
    )
}
