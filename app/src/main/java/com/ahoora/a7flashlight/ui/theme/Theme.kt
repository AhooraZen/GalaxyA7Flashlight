package com.ahoora.a7flashlight.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val CustomDarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = PureBlack,
    primaryContainer = Color(0xFF003816),
    onPrimaryContainer = NeonGreen,
    inversePrimary = NeonGreen,

    secondary = ElectricCyan,
    onSecondary = PureBlack,
    secondaryContainer = Color(0xFF00363D),
    onSecondaryContainer = ElectricCyan,

    tertiary = DeepBlue,
    onTertiary = TextPrimary,
    tertiaryContainer = Color(0xFF001F5C),
    onTertiaryContainer = ElectricCyan,

    background = PureBlack,
    onBackground = TextPrimary,

    surface = DeepMidnight,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary,
    surfaceTint = NeonGreen,
    inverseSurface = TextPrimary,
    inverseOnSurface = PureBlack,

    outline = BorderSubtle,
    outlineVariant = BorderHighlight,
    scrim = PureBlack,
    error = DangerRed,
    onError = PureBlack,
    errorContainer = DangerRedGlow,
    onErrorContainer = DangerRed
)

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun GalaxyA7FlashlightTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity()
            if (activity != null) {
                val window = activity.window
                window.statusBarColor = PureBlack.toArgb()
                window.navigationBarColor = PureBlack.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = CustomDarkColorScheme,
        typography = Typography,
        content = content
    )
}
