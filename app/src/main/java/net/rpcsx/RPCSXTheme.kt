package net.rpcsx

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.view.WindowInsetsControllerCompat

// Fallback palette (used on Android < 12 or when dynamic color is disabled)
private object Palette {
    // Light
    val primaryLight = Color(0xFF4D5C92)
    val onPrimaryLight = Color(0xFFFFFFFF)
    val primaryContainerLight = Color(0xFFDCE1FF)
    val onPrimaryContainerLight = Color(0xFF354479)
    val secondaryLight = Color(0xFF595D72)
    val onSecondaryLight = Color(0xFFFFFFFF)
    val secondaryContainerLight = Color(0xFFDEE1F9)
    val onSecondaryContainerLight = Color(0xFF424659)
    val tertiaryLight = Color(0xFF75546F)
    val onTertiaryLight = Color(0xFFFFFFFF)
    val tertiaryContainerLight = Color(0xFFFFD7F5)
    val onTertiaryContainerLight = Color(0xFF5B3D57)
    val errorLight = Color(0xFFBA1A1A)
    val onErrorLight = Color(0xFFFFFFFF)
    val errorContainerLight = Color(0xFFFFDAD6)
    val onErrorContainerLight = Color(0xFF93000A)
    val backgroundLight = Color(0xFFFAF8FF)
    val onBackgroundLight = Color(0xFF1A1B21)
    val surfaceLight = Color(0xFFFAF8FF)
    val onSurfaceLight = Color(0xFF1A1B21)
    val surfaceVariantLight = Color(0xFFE2E1EC)
    val onSurfaceVariantLight = Color(0xFF45464F)
    val outlineLight = Color(0xFF767680)
    val outlineVariantLight = Color(0xFFC6C6D0)
    val scrimLight = Color(0xFF000000)
    val inverseSurfaceLight = Color(0xFF2F3036)
    val inverseOnSurfaceLight = Color(0xFFF1F0F7)
    val inversePrimaryLight = Color(0xFFB6C4FF)
    val surfaceDimLight = Color(0xFFDAD9E0)
    val surfaceBrightLight = Color(0xFFFAF8FF)
    val surfaceContainerLowestLight = Color(0xFFFFFFFF)
    val surfaceContainerLowLight = Color(0xFFF4F3FA)
    val surfaceContainerLight = Color(0xFFEEEDF4)
    val surfaceContainerHighLight = Color(0xFFE9E7EF)
    val surfaceContainerHighestLight = Color(0xFFE3E1E9)

    // Dark
    val primaryDark = Color(0xFFB6C4FF)
    val onPrimaryDark = Color(0xFF1D2D61)
    val primaryContainerDark = Color(0xFF354479)
    val onPrimaryContainerDark = Color(0xFFDCE1FF)
    val secondaryDark = Color(0xFFC2C5DD)
    val onSecondaryDark = Color(0xFF2B3042)
    val secondaryContainerDark = Color(0xFF424659)
    val onSecondaryContainerDark = Color(0xFFDEE1F9)
    val tertiaryDark = Color(0xFFE3BADA)
    val onTertiaryDark = Color(0xFF432740)
    val tertiaryContainerDark = Color(0xFF5B3D57)
    val onTertiaryContainerDark = Color(0xFFFFD7F5)
    val errorDark = Color(0xFFFFB4AB)
    val onErrorDark = Color(0xFF690005)
    val errorContainerDark = Color(0xFF93000A)
    val onErrorContainerDark = Color(0xFFFFDAD6)
    val backgroundDark = Color(0xFF121318)
    val onBackgroundDark = Color(0xFFE3E1E9)
    val surfaceDark = Color(0xFF121318)
    val onSurfaceDark = Color(0xFFE3E1E9)
    val surfaceVariantDark = Color(0xFF45464F)
    val onSurfaceVariantDark = Color(0xFFC6C6D0)
    val outlineDark = Color(0xFF90909A)
    val outlineVariantDark = Color(0xFF45464F)
    val scrimDark = Color(0xFF000000)
    val inverseSurfaceDark = Color(0xFFE3E1E9)
    val inverseOnSurfaceDark = Color(0xFF2F3036)
    val inversePrimaryDark = Color(0xFF4D5C92)
    val surfaceDimDark = Color(0xFF121318)
    val surfaceBrightDark = Color(0xFF38393F)
    val surfaceContainerLowestDark = Color(0xFF0D0E13)
    val surfaceContainerLowDark = Color(0xFF1A1B21)
    val surfaceContainerDark = Color(0xFF1E1F25)
    val surfaceContainerHighDark = Color(0xFF292A2F)
    val surfaceContainerHighestDark = Color(0xFF34343A)
}

private val LightSchemeFallback = lightColorScheme(
    primary = Palette.primaryLight,
    onPrimary = Palette.onPrimaryLight,
    primaryContainer = Palette.primaryContainerLight,
    onPrimaryContainer = Palette.onPrimaryContainerLight,
    secondary = Palette.secondaryLight,
    onSecondary = Palette.onSecondaryLight,
    secondaryContainer = Palette.secondaryContainerLight,
    onSecondaryContainer = Palette.onSecondaryContainerLight,
    tertiary = Palette.tertiaryLight,
    onTertiary = Palette.onTertiaryLight,
    tertiaryContainer = Palette.tertiaryContainerLight,
    onTertiaryContainer = Palette.onTertiaryContainerLight,
    error = Palette.errorLight,
    onError = Palette.onErrorLight,
    errorContainer = Palette.errorContainerLight,
    onErrorContainer = Palette.onErrorContainerLight,
    background = Palette.backgroundLight,
    onBackground = Palette.onBackgroundLight,
    surface = Palette.surfaceLight,
    onSurface = Palette.onSurfaceLight,
    surfaceVariant = Palette.surfaceVariantLight,
    onSurfaceVariant = Palette.onSurfaceVariantLight,
    outline = Palette.outlineLight,
    outlineVariant = Palette.outlineVariantLight,
    scrim = Palette.scrimLight,
    inverseSurface = Palette.inverseSurfaceLight,
    inverseOnSurface = Palette.inverseOnSurfaceLight,
    inversePrimary = Palette.inversePrimaryLight,
    surfaceDim = Palette.surfaceDimLight,
    surfaceBright = Palette.surfaceBrightLight,
    surfaceContainerLowest = Palette.surfaceContainerLowestLight,
    surfaceContainerLow = Palette.surfaceContainerLowLight,
    surfaceContainer = Palette.surfaceContainerLight,
    surfaceContainerHigh = Palette.surfaceContainerHighLight,
    surfaceContainerHighest = Palette.surfaceContainerHighestLight,
)

private val DarkSchemeFallback = darkColorScheme(
    primary = Palette.primaryDark,
    onPrimary = Palette.onPrimaryDark,
    primaryContainer = Palette.primaryContainerDark,
    onPrimaryContainer = Palette.onPrimaryContainerDark,
    secondary = Palette.secondaryDark,
    onSecondary = Palette.onSecondaryDark,
    secondaryContainer = Palette.secondaryContainerDark,
    onSecondaryContainer = Palette.onSecondaryContainerDark,
    tertiary = Palette.tertiaryDark,
    onTertiary = Palette.onTertiaryDark,
    tertiaryContainer = Palette.tertiaryContainerDark,
    onTertiaryContainer = Palette.onTertiaryContainerDark,
    error = Palette.errorDark,
    onError = Palette.onErrorDark,
    errorContainer = Palette.errorContainerDark,
    onErrorContainer = Palette.onErrorContainerDark,
    background = Palette.backgroundDark,
    onBackground = Palette.onBackgroundDark,
    surface = Palette.surfaceDark,
    onSurface = Palette.onSurfaceDark,
    surfaceVariant = Palette.surfaceVariantDark,
    onSurfaceVariant = Palette.onSurfaceVariantDark,
    outline = Palette.outlineDark,
    outlineVariant = Palette.outlineVariantDark,
    scrim = Palette.scrimDark,
    inverseSurface = Palette.inverseSurfaceDark,
    inverseOnSurface = Palette.inverseOnSurfaceDark,
    inversePrimary = Palette.inversePrimaryDark,
    surfaceDim = Palette.surfaceDimDark,
    surfaceBright = Palette.surfaceBrightDark,
    surfaceContainerLowest = Palette.surfaceContainerLowestDark,
    surfaceContainerLow = Palette.surfaceContainerLowDark,
    surfaceContainer = Palette.surfaceContainerDark,
    surfaceContainerHigh = Palette.surfaceContainerHighDark,
    surfaceContainerHighest = Palette.surfaceContainerHighestDark,
)

// Unified shapes & typography
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8),
    small = RoundedCornerShape(12),
    medium = RoundedCornerShape(16),
    large = RoundedCornerShape(20),
    extraLarge = RoundedCornerShape(28),
)

private val AppTypography = Typography()

@Composable
fun RPCSXTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = true,  // Dynamic color only on Android 12+
    amoledBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val scheme: ColorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalView.current.context
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        else -> if (darkTheme) DarkSchemeFallback else LightSchemeFallback
    }.let { base ->
        if (darkTheme && amoledBlack) {
            base.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceDim = Color.Black,
                surfaceBright = Color(0xFF121212),
                surfaceContainerLowest = Color.Black,
                surfaceContainerLow = Color(0xFF0A0A0A),
                surfaceContainer = Color(0xFF0E0E0E),
                surfaceContainerHigh = Color(0xFF141414),
                surfaceContainerHighest = Color(0xFF181818)
            )
        } else base
    }

    // System bars (Android 10+ safe)
    val view = LocalView.current
    val activity = view.context as? Activity
    SideEffect {
        activity?.window?.let { w ->
            w.statusBarColor = Color.Transparent.toArgb()
            w.navigationBarColor = Color.Transparent.toArgb()
            w.isNavigationBarContrastEnforced = false
            WindowInsetsControllerCompat(w, w.decorView).apply {
                val light = !darkTheme
                isAppearanceLightStatusBars = light
                isAppearanceLightNavigationBars = light
            }
        }
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
