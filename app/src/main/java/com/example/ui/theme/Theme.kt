package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val EditorialLightColorScheme = lightColorScheme(
    primary = InkBlack,
    onPrimary = PureWhite,
    primaryContainer = SoftPlinth,
    onPrimaryContainer = InkBlack,
    secondary = CharcoalText,
    onSecondary = PureWhite,
    secondaryContainer = SoftPlinth,
    onSecondaryContainer = CharcoalText,
    tertiary = EditorialGrey,
    onTertiary = PureWhite,
    background = PureWhite,
    onBackground = InkBlack,
    surface = PureWhite,
    onSurface = InkBlack,
    surfaceVariant = SoftPlinth,
    onSurfaceVariant = EditorialGrey,
    outline = HairlineGrey,
    outlineVariant = SubtleDivider,
    error = EditorialCrimson,
    onError = PureWhite,
)

private val EditorialShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

@Composable
fun InstinctaTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = EditorialLightColorScheme,
        typography = InstinctaTypography,
        shapes = EditorialShapes,
        content = content,
    )
}
