package dev.daniil.iskra.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import dev.daniil.iskra.R
import dev.daniil.iskra.pet.Palettes

val Nunito = FontFamily(
    Font(R.font.nunito_medium, FontWeight.Normal),
    Font(R.font.nunito_medium, FontWeight.Medium),
    Font(R.font.nunito_bold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
    Font(R.font.nunito_black, FontWeight.Black),
)

data class IskraColors(
    val bg: Color,
    val card: Color,
    val card2: Color,
    val accent: Color,
    val accent2: Color,
    val text: Color,
    val dim: Color,
    val track: Color,
    val onAccent: Color,
)

fun colorsFor(palette: Int): IskraColors {
    val p = Palettes.get(palette)
    val accent = Color(p.accent)
    val bg = Color(0xFF13100F)
    val card = lerp(Color(0xFF211A17), accent, 0.05f)
    return IskraColors(
        bg = bg,
        card = card,
        card2 = lerp(Color(0xFF2B221E), accent, 0.08f),
        accent = accent,
        accent2 = Color(p.accent2),
        text = Color(0xFFF5EBE3),
        dim = Color(0xFFA4958D),
        track = Color(0xFF3A2F2A),
        onAccent = Color(0xFF1E0E06),
    )
}

val LocalColors = staticCompositionLocalOf { colorsFor(0) }

private fun TextStyle.nunito() = copy(fontFamily = Nunito)

private fun typography(): Typography {
    val b = Typography()
    return Typography(
        displayLarge = b.displayLarge.nunito(),
        displayMedium = b.displayMedium.nunito(),
        displaySmall = b.displaySmall.nunito(),
        headlineLarge = b.headlineLarge.nunito(),
        headlineMedium = b.headlineMedium.nunito(),
        headlineSmall = b.headlineSmall.nunito(),
        titleLarge = b.titleLarge.nunito(),
        titleMedium = b.titleMedium.nunito(),
        titleSmall = b.titleSmall.nunito(),
        bodyLarge = b.bodyLarge.nunito(),
        bodyMedium = b.bodyMedium.nunito(),
        bodySmall = b.bodySmall.nunito(),
        labelLarge = b.labelLarge.nunito(),
        labelMedium = b.labelMedium.nunito(),
        labelSmall = b.labelSmall.nunito(),
    )
}

@Composable
fun IskraTheme(colors: IskraColors, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalColors provides colors) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = colors.accent,
                onPrimary = colors.onAccent,
                secondary = colors.accent2,
                onSecondary = colors.onAccent,
                background = colors.bg,
                surface = colors.card,
                onBackground = colors.text,
                onSurface = colors.text,
                surfaceVariant = colors.card2,
                onSurfaceVariant = colors.dim,
                surfaceContainer = colors.card,
                surfaceContainerLow = colors.card,
                surfaceContainerHigh = colors.card2,
                secondaryContainer = colors.accent.copy(alpha = 0.22f),
                onSecondaryContainer = colors.text,
                outline = colors.track,
                outlineVariant = colors.track,
            ),
            typography = typography(),
            content = content,
        )
    }
}
