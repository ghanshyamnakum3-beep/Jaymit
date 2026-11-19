package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = CoralPrimary,
    onPrimary = OnCoralPrimary,
    primaryContainer = CoralContainer,
    onPrimaryContainer = OnCoralContainer,
    secondary = PlumSecondary,
    onSecondary = OnPlumSecondary,
    secondaryContainer = PlumContainer,
    onSecondaryContainer = OnPlumContainer,
    tertiary = MarigoldTertiary,
    onTertiary = OnMarigoldTertiary,
    tertiaryContainer = MarigoldContainer,
    onTertiaryContainer = OnMarigoldContainer,
    background = WarmCreamBackground,
    onBackground = OnWarmCream,
    surface = WarmCardSurface,
    onSurface = OnWarmCream,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = OnWarmSurfaceVariant,
    outline = WarmOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = CoralPrimaryDark,
    onPrimary = OnCoralPrimaryDark,
    primaryContainer = CoralContainerDark,
    onPrimaryContainer = OnCoralContainerDark,
    secondary = PlumSecondaryDark,
    onSecondary = OnPlumSecondaryDark,
    secondaryContainer = PlumContainerDark,
    onSecondaryContainer = OnPlumContainerDark,
    tertiary = MarigoldTertiaryDark,
    onTertiary = OnMarigoldTertiaryDark,
    tertiaryContainer = MarigoldContainerDark,
    onTertiaryContainer = OnMarigoldContainerDark,
    background = EspressoBackgroundDark,
    onBackground = OnEspressoDark,
    surface = EspressoSurfaceDark,
    onSurface = OnEspressoDark,
    surfaceVariant = EspressoSurfaceVariantDark,
    onSurfaceVariant = OnEspressoVariantDark,
    outline = EspressoOutlineDark
)

val CrewShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp)
)

@Composable
fun CrewCircleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = CrewShapes,
        content = content
    )
}
