package com.srlakes.tone.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Identidade visual do SR Lakes Tone.
 *
 * Sempre escuro: o aplicativo vive preso a um pedestal, em palco, com
 * pouca luz. Nao existe tema claro de proposito.
 *
 * A cor tem significado fixo em todo o aplicativo:
 *   azul eletrico  -> o que esta ativo / selecionado
 *   ambar          -> drive, solo, ganho, energia sobrando
 *   verde          -> conectado, dentro do esperado
 *   vermelho       -> clipping ou problema
 * Fora disso, tudo e cinza-azulado. Cor demais em palco vira ruido.
 */
object SR {
    val Background = Color(0xFF05070C)
    val Surface = Color(0xFF0A0F17)
    val SurfaceRaised = Color(0xFF111925)
    val SurfaceSunken = Color(0xFF070B11)
    val Outline = Color(0xFF1C2836)
    val OutlineStrong = Color(0xFF2A3B4F)

    val Electric = Color(0xFF2F9BFF)
    val ElectricDim = Color(0xFF16456F)
    val ElectricGlow = Color(0x552F9BFF)

    val Amber = Color(0xFFFFB020)
    val AmberDim = Color(0xFF6B4A0E)
    val AmberGlow = Color(0x55FFB020)

    val Green = Color(0xFF35D07F)
    val GreenDim = Color(0xFF15462C)

    val Red = Color(0xFFFF4D5E)
    val RedDim = Color(0xFF5A1420)

    val TextPrimary = Color(0xFFE9F0F8)
    val TextSecondary = Color(0xFF8DA2B9)
    val TextTertiary = Color(0xFF56697F)

    /** Fundo dos paineis, com um leve degrade para dar profundidade. */
    val PanelBrush = Brush.verticalGradient(listOf(Color(0xFF121B27), Color(0xFF0B121B)))

    val StageGlow = Brush.verticalGradient(
        listOf(Color(0xFF0B1420), Color(0xFF05070C))
    )

    val PanelCorner = 14.dp
    val ScreenPadding = 14.dp
}

private val SRTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 28.sp,
        letterSpacing = 0.5.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = 1.4.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 1.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 0.8.sp
    )
)

private val SRColorScheme = darkColorScheme(
    primary = SR.Electric,
    onPrimary = Color.White,
    secondary = SR.Amber,
    onSecondary = Color.Black,
    background = SR.Background,
    onBackground = SR.TextPrimary,
    surface = SR.Surface,
    onSurface = SR.TextPrimary,
    surfaceVariant = SR.SurfaceRaised,
    onSurfaceVariant = SR.TextSecondary,
    outline = SR.Outline,
    error = SR.Red,
    onError = Color.White
)

@Composable
fun SRLakesTheme(content: @Composable () -> Unit) {
    // O tema e sempre escuro; a leitura do sistema existe so para nao
    // brigar com o Compose Preview em modo claro.
    isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = SRColorScheme,
        typography = SRTypography,
        content = content
    )
}
