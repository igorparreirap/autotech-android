package com.autotech.android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Paleta do layout "TestLayout": tema claro com as cores da logo (branco e azul).
 */
object AtColors {
    // Paleta baseada na logo do AutoTech (azul #1878D8, azul-claro #74B8F0, azul-marinho)
    val Background = Color(0xFFEEF2F8)
    val Card = Color(0xFFFFFFFF)
    val Ink = Color(0xFF0F1B2D)        // textos e ícones (azul quase preto)
    val InkSoft = Color(0xFF2A3A55)
    val Muted = Color(0xFF8A94A6)      // textos de apoio
    val Faint = Color(0xFFC5CDD8)
    val Line = Color(0xFFE3E9F1)
    val Field = Color(0xFFF3F6FA)
    val Primary = Color(0xFF1569CC)    // botões, blocos e itens selecionados (branco sobre ele: 5,4:1)
    val Deep = Color(0xFF0E2A57)       // caixas escuras de informação
    val Accent = Color(0xFF74B8F0)     // destaques, medidores e marcadores
    val AccentLight = Color(0xFFB9DAF8)
    val AccentSoft = Color(0xFFE6F1FD)
    val Danger = Color(0xFFE5484D)
    val DangerSoft = Color(0xFFFDECEC)
    val PlateBlue = Color(0xFF1C3F94)
}

private val LightColors = lightColorScheme(
    primary = AtColors.Primary,
    onPrimary = Color.White,
    primaryContainer = AtColors.AccentSoft,
    onPrimaryContainer = AtColors.Ink,
    secondary = AtColors.Accent,
    onSecondary = AtColors.Ink,
    tertiary = AtColors.Accent,
    onTertiary = AtColors.Ink,
    background = AtColors.Background,
    onBackground = AtColors.Ink,
    surface = AtColors.Card,
    onSurface = AtColors.Ink,
    surfaceVariant = AtColors.Field,
    onSurfaceVariant = AtColors.Muted,
    surfaceContainerLowest = AtColors.Card,
    surfaceContainerLow = AtColors.Card,
    surfaceContainer = AtColors.Card,
    surfaceContainerHigh = AtColors.Card,
    surfaceContainerHighest = AtColors.Field,
    outline = AtColors.Faint,
    outlineVariant = AtColors.Line,
    error = AtColors.Danger,
    onError = Color.White,
    errorContainer = AtColors.DangerSoft
)

private val AtTypography = Typography(
    displaySmall = TextStyle(fontSize = 34.sp, lineHeight = 38.sp, fontWeight = FontWeight.Medium, letterSpacing = (-1.2).sp),
    headlineMedium = TextStyle(fontSize = 27.sp, lineHeight = 32.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.8).sp),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp)
)

private val AtShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

/** O layout de referência é claro; o app usa sempre o tema claro para ficar fiel a ele. */
@Composable
fun AutoTechTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AtTypography,
        shapes = AtShapes,
        content = content
    )
}
