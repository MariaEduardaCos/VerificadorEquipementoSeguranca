package br.unirv.capsafe.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/*
 * Tema CapSafe — EXCLUSIVAMENTE Light Mode (enunciado, seção 7).
 * Design System "Obra Segura": laranja de sinalização + cinza aço.
 */
private val LightColors = lightColorScheme(
    primary = SafetyOrange,
    onPrimary = Color.White,
    primaryContainer = SafetyOrangeLight,
    onPrimaryContainer = SafetyOrangeDark,
    secondary = SteelBlue,
    onSecondary = Color.White,
    secondaryContainer = SteelLight,
    onSecondaryContainer = Color(0xFF1E293B),
    tertiary = HardHatYellow,
    onTertiary = Color(0xFF3F2D04),
    tertiaryContainer = HardHatYellowLight,
    onTertiaryContainer = Color(0xFF3F2D04),
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondary,
    error = ViolationRed,
    onError = Color.White,
    errorContainer = ViolationRedLight,
    onErrorContainer = ViolationRedDark,
    outline = OutlineGray,
    outlineVariant = Color(0xFFE2E8F0)
)

@Composable
fun CapSafeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
