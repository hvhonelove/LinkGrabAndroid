package com.example.linkgrab.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.linkgrab.domain.ThemeMode

private val Light = lightColorScheme(primary = Color(0xFF8253F5), onPrimary = Color.White,
    primaryContainer = Color(0xFFEEE6FF), onPrimaryContainer = Color(0xFF5129AC),
    background = Color(0xFFF8F7FC), onBackground = Color(0xFF211C32),
    surface = Color.White, onSurface = Color(0xFF211C32), surfaceVariant = Color(0xFFF0EDF6),
    onSurfaceVariant = Color(0xFF777184), outline = Color(0xFFDCD7E8))
private val Dark = darkColorScheme(primary = Color(0xFFB79AFF), onPrimary = Color(0xFF28134C),
    primaryContainer = Color(0xFF33244C), onPrimaryContainer = Color(0xFFE5D8FF),
    background = Color(0xFF111017), onBackground = Color(0xFFF4EFFC),
    surface = Color(0xFF1D1A26), onSurface = Color(0xFFF4EFFC), surfaceVariant = Color(0xFF292432),
    onSurfaceVariant = Color(0xFFB5AEBF), outline = Color(0xFF453C54))
@Composable
fun LinkGrabTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = mode == ThemeMode.DARK || mode == ThemeMode.SYSTEM && isSystemInDarkTheme()
    val activity = LocalContext.current as? Activity
    SideEffect { activity?.window?.let {
        WindowCompat.getInsetsController(it, it.decorView).apply {
            isAppearanceLightStatusBars = !dark; isAppearanceLightNavigationBars = !dark
        }
    } }
    val typography = Typography(
        headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 39.sp, letterSpacing = (-.9).sp),
        headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 27.sp, lineHeight = 33.sp, letterSpacing = (-.5).sp),
        titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
        titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 23.sp),
        bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp),
        bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
        bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
        labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp)
    )
    MaterialTheme(colorScheme = if (dark) Dark else Light, typography = typography, content = content)
}
