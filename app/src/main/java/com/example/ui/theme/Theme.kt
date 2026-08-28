package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.data.model.OverchargeCategory
import com.example.data.model.TrafficLevel

private val LightColors = lightColorScheme(
    primary = JadePrimaryLight,
    onPrimary = JadeOnPrimaryLight,
    primaryContainer = JadePrimaryContainerLight,
    onPrimaryContainer = JadeOnPrimaryContainerLight,
    inversePrimary = InversePrimaryLight,
    secondary = SlateSecondaryLight,
    onSecondary = SlateOnSecondaryLight,
    secondaryContainer = SlateSecondaryContainerLight,
    onSecondaryContainer = SlateOnSecondaryContainerLight,
    tertiary = BronzeTertiaryLight,
    onTertiary = BronzeOnTertiaryLight,
    tertiaryContainer = BronzeTertiaryContainerLight,
    onTertiaryContainer = BronzeOnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight
)

private val DarkColors = darkColorScheme(
    primary = JadePrimaryDark,
    onPrimary = JadeOnPrimaryDark,
    primaryContainer = JadePrimaryContainerDark,
    onPrimaryContainer = JadeOnPrimaryContainerDark,
    inversePrimary = InversePrimaryDark,
    secondary = SlateSecondaryDark,
    onSecondary = SlateOnSecondaryDark,
    secondaryContainer = SlateSecondaryContainerDark,
    onSecondaryContainer = SlateOnSecondaryContainerDark,
    tertiary = BronzeTertiaryDark,
    onTertiary = BronzeOnTertiaryDark,
    tertiaryContainer = BronzeTertiaryContainerDark,
    onTertiaryContainer = BronzeOnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark
)

private val MonochromeDarkColors = darkColorScheme(
    primary = MonoWhite,
    onPrimary = MonoBlack,
    primaryContainer = MonoSurfaceContainerHighDark,
    onPrimaryContainer = MonoWhite,
    inversePrimary = MonoGrey300,
    secondary = MonoGrey300,
    onSecondary = MonoBlack,
    secondaryContainer = MonoSurfaceContainerLowDark,
    onSecondaryContainer = MonoWhite,
    tertiary = MonoGrey400,
    onTertiary = MonoBlack,
    tertiaryContainer = MonoSurfaceContainerDark,
    onTertiaryContainer = MonoGrey200,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = MonoPitchDark,
    onBackground = MonoWhite,
    surface = MonoPitchDark,
    onSurface = MonoWhite,
    surfaceVariant = MonoSurfaceContainerDark,
    onSurfaceVariant = MonoGrey300,
    surfaceContainerLowest = MonoPitchDark,
    surfaceContainerLow = MonoSurfaceContainerLowDark,
    surfaceContainer = MonoSurfaceContainerDark,
    surfaceContainerHigh = MonoSurfaceContainerHighDark,
    surfaceContainerHighest = MonoSurfaceContainerHighestDark,
    outline = MonoGrey600,
    outlineVariant = MonoBorderDark,
    inverseSurface = MonoWhite,
    inverseOnSurface = MonoBlack
)

private val MonochromeLightColors = lightColorScheme(
    primary = MonoBlack,
    onPrimary = MonoWhite,
    primaryContainer = MonoSurfaceContainerLight,
    onPrimaryContainer = MonoBlack,
    inversePrimary = MonoGrey700,
    secondary = MonoGrey800,
    onSecondary = MonoWhite,
    secondaryContainer = MonoSurfaceContainerLowLight,
    onSecondaryContainer = MonoBlack,
    tertiary = MonoGrey700,
    onTertiary = MonoWhite,
    tertiaryContainer = MonoSurfaceContainerHighLight,
    onTertiaryContainer = MonoBlack,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = MonoPureWhite,
    onBackground = MonoBlack,
    surface = MonoPureWhite,
    onSurface = MonoBlack,
    surfaceVariant = MonoSurfaceContainerLight,
    onSurfaceVariant = MonoGrey800,
    surfaceContainerLowest = MonoPureWhite,
    surfaceContainerLow = MonoSurfaceContainerLowLight,
    surfaceContainer = MonoSurfaceContainerLight,
    surfaceContainerHigh = MonoSurfaceContainerHighLight,
    surfaceContainerHighest = MonoSurfaceContainerHighestLight,
    outline = MonoGrey500,
    outlineVariant = MonoBorderLight,
    inverseSurface = MonoBlack,
    inverseOnSurface = MonoWhite
)

enum class AppThemeMode(val displayName: String) {
    SYSTEM("System Default"),
    LIGHT("Day (Jade)"),
    DARK("Night (Graphite)"),
    MONOCHROME_DARK("AMOLED Pitch Black"),
    MONOCHROME_LIGHT("Monochrome Light")
}

/**
 * Semantic colours that Material's [androidx.compose.material3.ColorScheme] does
 * not model: the overcharge fairness scale, traffic congestion scale, journey
 * markers and comparison badges. Kept in a CompositionLocal so a single palette
 * switch flips both light and dark variants everywhere.
 */
data class FairFareColors(
    val isDark: Boolean,
    val fair: Color,
    val fairContainer: Color,
    val onFairContainer: Color,
    val slightlyHigh: Color,
    val slightlyHighContainer: Color,
    val onSlightlyHighContainer: Color,
    val high: Color,
    val highContainer: Color,
    val onHighContainer: Color,
    val veryHigh: Color,
    val veryHighContainer: Color,
    val onVeryHighContainer: Color,
    val origin: Color,
    val destination: Color,
    val cheapestContainer: Color,
    val onCheapestContainer: Color,
    val fastestContainer: Color,
    val onFastestContainer: Color,
    val bestValueContainer: Color,
    val onBestValueContainer: Color,
    val trafficFreeFlow: Color,
    val trafficModerate: Color,
    val trafficHeavy: Color,
    val trafficSevere: Color
) {
    /** Accent colour for an overcharge verdict — icons, borders, emphasis. */
    fun accentFor(category: OverchargeCategory): Color = when (category) {
        OverchargeCategory.FAIR -> fair
        OverchargeCategory.SLIGHTLY_HIGH -> slightlyHigh
        OverchargeCategory.HIGH -> high
        OverchargeCategory.VERY_HIGH -> veryHigh
    }

    /** Tinted surface for an overcharge verdict — use behind text. */
    fun containerFor(category: OverchargeCategory): Color = when (category) {
        OverchargeCategory.FAIR -> fairContainer
        OverchargeCategory.SLIGHTLY_HIGH -> slightlyHighContainer
        OverchargeCategory.HIGH -> highContainer
        OverchargeCategory.VERY_HIGH -> veryHighContainer
    }

    /** Legible foreground for [containerFor]. */
    fun onContainerFor(category: OverchargeCategory): Color = when (category) {
        OverchargeCategory.FAIR -> onFairContainer
        OverchargeCategory.SLIGHTLY_HIGH -> onSlightlyHighContainer
        OverchargeCategory.HIGH -> onHighContainer
        OverchargeCategory.VERY_HIGH -> onVeryHighContainer
    }

    fun colorFor(level: TrafficLevel): Color = when (level) {
        TrafficLevel.FREE_FLOW -> trafficFreeFlow
        TrafficLevel.MODERATE -> trafficModerate
        TrafficLevel.HEAVY -> trafficHeavy
        TrafficLevel.SEVERE -> trafficSevere
    }
}

private val LightFairFareColors = FairFareColors(
    isDark = false,
    fair = FairGreen,
    fairContainer = FairGreenLight,
    onFairContainer = FairGreenText,
    slightlyHigh = SlightlyHighAmber,
    slightlyHighContainer = SlightlyHighAmberLight,
    onSlightlyHighContainer = SlightlyHighAmberText,
    high = HighOrange,
    highContainer = HighOrangeLight,
    onHighContainer = HighOrangeText,
    veryHigh = VeryHighRed,
    veryHighContainer = VeryHighRedLight,
    onVeryHighContainer = VeryHighRedText,
    origin = OriginMarkerLight,
    destination = DestinationMarkerLight,
    cheapestContainer = BadgeCheapestBg,
    onCheapestContainer = BadgeCheapestText,
    fastestContainer = BadgeFastestBg,
    onFastestContainer = BadgeFastestText,
    bestValueContainer = BadgeBestValueBg,
    onBestValueContainer = BadgeBestValueText,
    trafficFreeFlow = TrafficFreeFlow,
    trafficModerate = TrafficModerate,
    trafficHeavy = TrafficHeavy,
    trafficSevere = TrafficSevere
)

private val DarkFairFareColors = FairFareColors(
    isDark = true,
    fair = FairGreenDark,
    fairContainer = FairGreenContainerDark,
    onFairContainer = FairGreenOnContainerDark,
    slightlyHigh = SlightlyHighAmberDark,
    slightlyHighContainer = SlightlyHighAmberContainerDark,
    onSlightlyHighContainer = SlightlyHighAmberOnContainerDark,
    high = HighOrangeDark,
    highContainer = HighOrangeContainerDark,
    onHighContainer = HighOrangeOnContainerDark,
    veryHigh = VeryHighRedDark,
    veryHighContainer = VeryHighRedContainerDark,
    onVeryHighContainer = VeryHighRedOnContainerDark,
    origin = OriginMarkerDark,
    destination = DestinationMarkerDark,
    cheapestContainer = BadgeCheapestBgDark,
    onCheapestContainer = BadgeCheapestTextDark,
    fastestContainer = BadgeFastestBgDark,
    onFastestContainer = BadgeFastestTextDark,
    bestValueContainer = BadgeBestValueBgDark,
    onBestValueContainer = BadgeBestValueTextDark,
    trafficFreeFlow = TrafficFreeFlow,
    trafficModerate = TrafficModerate,
    trafficHeavy = TrafficHeavy,
    trafficSevere = TrafficSevere
)

private val MonochromeDarkFairFareColors = FairFareColors(
    isDark = true,
    fair = MonoWhite,
    fairContainer = MonoSurfaceContainerHighDark,
    onFairContainer = MonoWhite,
    slightlyHigh = MonoGrey300,
    slightlyHighContainer = MonoSurfaceContainerDark,
    onSlightlyHighContainer = MonoGrey200,
    high = MonoGrey400,
    highContainer = MonoSurfaceContainerLowDark,
    onHighContainer = MonoWhite,
    veryHigh = ErrorDark,
    veryHighContainer = ErrorContainerDark,
    onVeryHighContainer = OnErrorContainerDark,
    origin = MonoWhite,
    destination = MonoGrey400,
    cheapestContainer = MonoSurfaceContainerHighDark,
    onCheapestContainer = MonoWhite,
    fastestContainer = MonoSurfaceContainerDark,
    onFastestContainer = MonoGrey200,
    bestValueContainer = MonoSurfaceContainerHighestDark,
    onBestValueContainer = MonoWhite,
    trafficFreeFlow = MonoWhite,
    trafficModerate = MonoGrey400,
    trafficHeavy = MonoGrey600,
    trafficSevere = ErrorDark
)

val LocalFairFareColors = staticCompositionLocalOf { LightFairFareColors }

/** Shorthand for the semantic palette: `FairFareTheme.colors.fair`. */
object FairFareTheme {
    val colors: FairFareColors
        @Composable get() = LocalFairFareColors.current
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeMode: AppThemeMode = if (darkTheme) AppThemeMode.DARK else AppThemeMode.LIGHT,
    content: @Composable () -> Unit
) {
    val (colorScheme, fairFareColors, isDarkBar) = when (themeMode) {
        AppThemeMode.MONOCHROME_DARK -> Triple(MonochromeDarkColors, MonochromeDarkFairFareColors, true)
        AppThemeMode.MONOCHROME_LIGHT -> Triple(MonochromeLightColors, LightFairFareColors, false)
        AppThemeMode.DARK -> Triple(DarkColors, DarkFairFareColors, true)
        AppThemeMode.LIGHT -> Triple(LightColors, LightFairFareColors, false)
        AppThemeMode.SYSTEM -> if (darkTheme) {
            Triple(DarkColors, DarkFairFareColors, true)
        } else {
            Triple(LightColors, LightFairFareColors, false)
        }
    }

    val view = LocalView.current
    val context = LocalContext.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !isDarkBar
            controller.isAppearanceLightNavigationBars = !isDarkBar
        }
    }

    CompositionLocalProvider(LocalFairFareColors provides fairFareColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = FairFareShapes,
            content = content
        )
    }
}


