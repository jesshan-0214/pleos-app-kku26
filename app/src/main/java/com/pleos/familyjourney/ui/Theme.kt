package com.pleos.familyjourney.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Brand = Color(0xFF1E88E5)
val BrandDark = Color(0xFF90CAF9)
val Accent = Color(0xFFFFB300)
val Success = Color(0xFF43A047)
val Danger = Color(0xFFE53935)

private val Light = lightColorScheme(
    primary = Brand,
    secondary = Accent,
    tertiary = Success,
    error = Danger,
    background = Color(0xFFF5F7FA),
    surface = Color.White,
    surfaceVariant = Color(0xFFE8EEF5),
)

private val Dark = darkColorScheme(
    primary = BrandDark,
    secondary = Accent,
    tertiary = Color(0xFF81C784),
    error = Color(0xFFEF9A9A),
    background = Color(0xFF0F1419),
    surface = Color(0xFF1A2129),
    surfaceVariant = Color(0xFF26313C),
)

/** 차량 화면은 대부분 어두운 테마라서 시스템 설정을 따른다. */
@Composable
fun FamilyJourneyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
