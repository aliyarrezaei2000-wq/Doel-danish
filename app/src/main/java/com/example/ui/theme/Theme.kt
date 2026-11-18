package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = LapisTealPrimary,
    onPrimary = OnLapisTealPrimary,
    primaryContainer = LapisTealContainer,
    onPrimaryContainer = OnLapisTealContainer,
    secondary = SaffronSecondary,
    onSecondary = OnSaffronSecondary,
    secondaryContainer = SaffronContainer,
    onSecondaryContainer = OnSaffronContainer,
    tertiary = TerracottaTertiary,
    onTertiary = OnTerracottaTertiary,
    tertiaryContainer = TerracottaContainer,
    onTertiaryContainer = OnTerracottaContainer,
    background = IvoryBackground,
    onBackground = OnIvoryBackground,
    surface = WarmSurface,
    onSurface = OnWarmSurface,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = OnWarmSurfaceVariant,
    outline = WarmOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = TurquoiseDarkPrimary,
    onPrimary = OnTurquoiseDarkPrimary,
    primaryContainer = TurquoiseDarkContainer,
    onPrimaryContainer = OnTurquoiseDarkContainer,
    secondary = GoldDarkSecondary,
    onSecondary = OnGoldDarkSecondary,
    secondaryContainer = GoldDarkContainer,
    onSecondaryContainer = OnGoldDarkContainer,
    tertiary = CoralDarkTertiary,
    onTertiary = OnCoralDarkTertiary,
    tertiaryContainer = CoralDarkContainer,
    onTertiaryContainer = OnCoralDarkContainer,
    background = NightLapisBackground,
    onBackground = OnNightLapisBackground,
    surface = NightLapisSurface,
    onSurface = OnNightLapisSurface,
    surfaceVariant = NightLapisSurfaceVariant,
    onSurfaceVariant = OnNightLapisSurfaceVariant,
    outline = NightOutline
)

@Immutable
data class SemanticQuizColors(
    val correct: Color,
    val correctContainer: Color,
    val onCorrectContainer: Color,
    val wrong: Color,
    val wrongContainer: Color,
    val onWrongContainer: Color
)

private val LightSemanticColors = SemanticQuizColors(
    correct = FeedbackCorrectBg,
    correctContainer = FeedbackCorrectContainerLight,
    onCorrectContainer = FeedbackOnCorrectContainerLight,
    wrong = FeedbackWrongBg,
    wrongContainer = FeedbackWrongContainerLight,
    onWrongContainer = FeedbackOnWrongContainerLight
)

private val DarkSemanticColors = SemanticQuizColors(
    correct = FeedbackCorrectBg,
    correctContainer = FeedbackCorrectContainerDark,
    onCorrectContainer = FeedbackOnCorrectContainerDark,
    wrong = FeedbackWrongBg,
    wrongContainer = FeedbackWrongContainerDark,
    onWrongContainer = FeedbackOnWrongContainerDark
)

val LocalSemanticQuizColors = staticCompositionLocalOf { LightSemanticColors }

val DuelDaneshShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DuelDaneshTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalSemanticQuizColors provides semanticColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DuelDaneshTypography,
            shapes = DuelDaneshShapes,
            content = content
        )
    }
}
