package dev.daniil.iskra.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import dev.daniil.iskra.R
import dev.daniil.iskra.pet.Palettes
import dev.daniil.iskra.scene.DayPhase

val Nunito = FontFamily(
    Font(R.font.nunito_medium, FontWeight.Normal),
    Font(R.font.nunito_medium, FontWeight.Medium),
    Font(R.font.nunito_bold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
    Font(R.font.nunito_black, FontWeight.Black),
)

/** Interface colours for the current part of the day. */
data class UiColors(
    val phase: DayPhase,
    val light: Boolean,
    val sheet: Color,
    val card: Color,
    val card2: Color,
    val text: Color,
    val dim: Color,
    val track: Color,
    val accent: Color,
    val accent2: Color,
    /** accent that is readable as text on [card] */
    val accentText: Color,
    val onAccent: Color,
    val nav: Color,
    val danger: Color,
)

fun uiColors(phase: DayPhase, palette: Int, auto: Boolean): UiColors {
    val ph = if (auto) phase else DayPhase.NIGHT
    val p = Palettes.get(palette)
    val accent = Color(p.accent)
    val accent2 = Color(p.accent2)
    val onAccent = Color(0xFF241005)
    return when (ph) {
        DayPhase.MORNING -> UiColors(
            ph, true,
            sheet = Color(0xFFFBF4EA), card = Color(0xFFFFFFFF), card2 = Color(0xFFF5EDE2),
            text = Color(0xFF2A221D), dim = Color(0xFF7D7067), track = Color(0xFFEDE3D6),
            accent = accent, accent2 = accent2, accentText = lerp(accent, Color.Black, 0.28f), onAccent = onAccent,
            nav = Color(0xFFFFFFFF), danger = Color(0xFFD64545),
        )
        DayPhase.DAY -> UiColors(
            ph, true,
            sheet = Color(0xFFF1F5FA), card = Color(0xFFFFFFFF), card2 = Color(0xFFE9F0F7),
            text = Color(0xFF18222E), dim = Color(0xFF66737F), track = Color(0xFFDDE6EF),
            accent = accent, accent2 = accent2, accentText = lerp(accent, Color.Black, 0.28f), onAccent = onAccent,
            nav = Color(0xFFFFFFFF), danger = Color(0xFFD64545),
        )
        DayPhase.DAWN -> UiColors(
            ph, false,
            sheet = Color(0xFF241C3A), card = Color(0xFF30264C), card2 = Color(0xFF3B3058),
            text = Color(0xFFFFF2EE), dim = Color(0xFFC7B3C8), track = Color(0xFF4A3D67),
            accent = accent, accent2 = accent2, accentText = accent, onAccent = onAccent,
            nav = Color(0xFF33294F), danger = Color(0xFFFF7B7B),
        )
        DayPhase.SUNSET -> UiColors(
            ph, false,
            sheet = Color(0xFF2A1832), card = Color(0xFF3A2342), card2 = Color(0xFF482B4F),
            text = Color(0xFFFFF1E6), dim = Color(0xFFDDB9AE), track = Color(0xFF5A3862),
            accent = accent, accent2 = accent2, accentText = accent, onAccent = onAccent,
            nav = Color(0xFF3D2546), danger = Color(0xFFFF7B7B),
        )
        DayPhase.DUSK -> UiColors(
            ph, false,
            sheet = Color(0xFF19163A), card = Color(0xFF241F4A), card2 = Color(0xFF2E2858),
            text = Color(0xFFF1EEFF), dim = Color(0xFFA9A3CF), track = Color(0xFF3C3669),
            accent = accent, accent2 = accent2, accentText = accent, onAccent = onAccent,
            nav = Color(0xFF27214F), danger = Color(0xFFFF7B7B),
        )
        DayPhase.NIGHT -> UiColors(
            ph, false,
            sheet = Color(0xFF0E1229), card = Color(0xFF171E3D), card2 = Color(0xFF20284B),
            text = Color(0xFFEEF1FF), dim = Color(0xFF98A2C8), track = Color(0xFF2B345E),
            accent = accent, accent2 = accent2, accentText = accent, onAccent = onAccent,
            nav = Color(0xFF1A2244), danger = Color(0xFFFF7B7B),
        )
    }
}

val LocalUi = staticCompositionLocalOf { uiColors(DayPhase.NIGHT, 0, true) }

/** Seconds since the app started; drives every animation from one frame loop. */
val LocalClock = staticCompositionLocalOf<State<Float>> { mutableFloatStateOf(0f) }

/** Flame flicker speed (radians per second). */
const val PHASE_SPEED = 7.54f

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
fun IskraTheme(ui: UiColors, content: @Composable () -> Unit) {
    val scheme = if (ui.light) {
        lightColorScheme(
            primary = ui.accent, onPrimary = ui.onAccent, secondary = ui.accent2, onSecondary = ui.onAccent,
            background = ui.sheet, onBackground = ui.text, surface = ui.card, onSurface = ui.text,
            surfaceVariant = ui.card2, onSurfaceVariant = ui.dim,
            surfaceContainer = ui.card, surfaceContainerLow = ui.card, surfaceContainerHigh = ui.card,
            surfaceContainerHighest = ui.card2, surfaceContainerLowest = ui.card,
            secondaryContainer = ui.accent.copy(alpha = 0.2f), onSecondaryContainer = ui.text,
            outline = ui.track, outlineVariant = ui.track, error = ui.danger,
        )
    } else {
        darkColorScheme(
            primary = ui.accent, onPrimary = ui.onAccent, secondary = ui.accent2, onSecondary = ui.onAccent,
            background = ui.sheet, onBackground = ui.text, surface = ui.card, onSurface = ui.text,
            surfaceVariant = ui.card2, onSurfaceVariant = ui.dim,
            surfaceContainer = ui.card, surfaceContainerLow = ui.card, surfaceContainerHigh = ui.card,
            surfaceContainerHighest = ui.card2, surfaceContainerLowest = ui.card,
            secondaryContainer = ui.accent.copy(alpha = 0.22f), onSecondaryContainer = ui.text,
            outline = ui.track, outlineVariant = ui.track, error = ui.danger,
        )
    }
    CompositionLocalProvider(LocalUi provides ui) {
        MaterialTheme(colorScheme = scheme, typography = typography(), content = content)
    }
}
