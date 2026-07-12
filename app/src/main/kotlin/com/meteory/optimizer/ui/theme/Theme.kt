package com.meteory.optimizer.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary            = Primary400,
    onPrimary          = Neutral950,
    primaryContainer   = Primary800,
    onPrimaryContainer = Primary100,
    secondary          = Secondary300,
    onSecondary        = Neutral950,
    secondaryContainer = Secondary500,
    onSecondaryContainer = Secondary100,
    tertiary           = Accent500,
    onTertiary         = Neutral950,
    tertiaryContainer  = androidx.compose.ui.graphics.Color(0xFF003D22),
    onTertiaryContainer = Accent300,
    background         = Neutral950,
    onBackground       = Neutral100,
    surface            = Neutral900,
    onSurface          = Neutral100,
    surfaceVariant     = Neutral800,
    onSurfaceVariant   = Neutral300,
    outline            = Neutral600,
    outlineVariant     = Neutral700,
    error              = Error400,
    onError            = Neutral950,
    errorContainer     = Error500,
    onErrorContainer   = Error300,
    inverseSurface     = Neutral200,
    inverseOnSurface   = Neutral900,
    inversePrimary     = Primary600,
    scrim              = Neutral950
)

private val LightColorScheme = lightColorScheme(
    primary            = Primary600,
    onPrimary          = Neutral50,
    primaryContainer   = Primary100,
    onPrimaryContainer = Primary900,
    secondary          = Secondary300,
    onSecondary        = Neutral50,
    secondaryContainer = Secondary100,
    onSecondaryContainer = Secondary500,
    tertiary           = Accent500,
    onTertiary         = Neutral950,
    background         = Neutral50,
    onBackground       = Neutral900,
    surface            = Neutral100,
    onSurface          = Neutral900,
    surfaceVariant     = Neutral200,
    onSurfaceVariant   = Neutral700,
    outline            = Neutral400,
    error              = Error500,
    onError            = Neutral50
)

@Composable
fun MeteoryTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = MeteoryTypography,
        content     = content
    )
}
