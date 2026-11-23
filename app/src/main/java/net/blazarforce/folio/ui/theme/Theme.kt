package net.blazarforce.folio.ui.theme

import net.blazarforce.folio.ui.common.FolioMotion
import net.blazarforce.folio.ui.common.LocalMotion
import net.blazarforce.folio.ui.common.systemReducesMotion
import androidx.compose.ui.graphics.Brush

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import net.blazarforce.folio.R
import net.blazarforce.folio.data.AppTheme

val Serif = FontFamily(
    Font(R.font.cormorant_medium, FontWeight.Medium),
    Font(R.font.cormorant_semibold, FontWeight.SemiBold),
    Font(R.font.cormorant_italic, FontWeight.Medium, FontStyle.Italic),
)
val Sans = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

/** Extra colours beyond Material roles. */
@Immutable
data class FolioPalette(
    val isInk: Boolean,
    val canvas: Color,
    val surface: Color,
    val raised: Color,
    val text: Color,
    val muted: Color,
    val faint: Color,
    val accent: Color,
    val accentSoft: Color,
    val hairline: Color,
    val shadow: Color,
    /** Highlight and deep ends of the gold foil. */
    val goldHi: Color = Color(0xFFF3DFAE),
    val goldLo: Color = Color(0xFFA9844A),
    /** Ink on top of gold. */
    val onAccent: Color = Color(0xFF1A1408),
    /** Warm glow used for soft lighting behind hero content. */
    val glow: Color = Color(0x2EC9A96E),
) {
    /** Foil gradient for primary buttons, badges and the active tab. */
    val goldBrush: Brush get() = Brush.linearGradient(listOf(goldHi, accent, goldLo))
    val goldEdge: Brush get() = Brush.linearGradient(listOf(goldHi.copy(alpha = 0.75f), accent.copy(alpha = 0.18f), goldLo.copy(alpha = 0.55f)))
}

val InkPalette = FolioPalette(
    isInk = true,
    canvas = Color(0xFF0F0E0D),
    surface = Color(0xFF181614),
    raised = Color(0xFF221F1C),
    text = Color(0xFFEFE8D8),
    muted = Color(0xFFA39A8A),
    faint = Color(0xFF6B645A),
    accent = Color(0xFFC9A96E),
    accentSoft = Color(0x33C9A96E),
    hairline = Color(0x1FEFE8D8),
    shadow = Color(0xFF000000),
)

val PaperPalette = FolioPalette(
    isInk = false,
    canvas = Color(0xFFF3EEE4),
    surface = Color(0xFFFAF7F1),
    raised = Color(0xFFFFFFFF),
    text = Color(0xFF1F1C18),
    muted = Color(0xFF6E665B),
    faint = Color(0xFFA59D90),
    accent = Color(0xFF8E6A2C),
    accentSoft = Color(0x268E6A2C),
    hairline = Color(0x1F1F1C18),
    shadow = Color(0xFF3A2E1E),
    goldHi = Color(0xFFC79B4E),
    goldLo = Color(0xFF6E5020),
    onAccent = Color(0xFFFFFBF2),
    glow = Color(0x228E6A2C),
)

val LocalPalette = staticCompositionLocalOf { InkPalette }

private fun typography(p: FolioPalette) = Typography(
    displayLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 52.sp, lineHeight = 54.sp, letterSpacing = (-0.01).em),
    displayMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 40.sp, lineHeight = 44.sp),
    displaySmall = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Medium, fontSize = 32.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 25.sp),
    titleMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 21.sp),
    titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.02.em),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.04.em),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 10.5.sp, letterSpacing = 0.16.em),
)

/** Small-caps style eyebrow label. */
val Eyebrow = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 10.5.sp, letterSpacing = 0.18.em)

@Composable
fun FolioTheme(theme: AppTheme, motion: FolioMotion? = null, content: @Composable () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val m = motion ?: androidx.compose.runtime.remember(ctx) { FolioMotion(reduce = ctx.systemReducesMotion(), ambient = FolioMotion.ambientByDefault) }
    val p = if (theme == AppTheme.INK) InkPalette else PaperPalette
    val scheme = if (p.isInk) darkColorScheme(
        primary = p.accent, onPrimary = Color(0xFF1A1408), secondary = p.accent,
        background = p.canvas, onBackground = p.text, surface = p.surface, onSurface = p.text,
        surfaceVariant = p.raised, onSurfaceVariant = p.muted, outline = p.hairline, outlineVariant = p.hairline,
        surfaceContainer = p.surface, surfaceContainerHigh = p.raised, surfaceContainerLow = p.surface,
        secondaryContainer = p.accentSoft, onSecondaryContainer = p.text,
    ) else lightColorScheme(
        primary = p.accent, onPrimary = Color(0xFFFFFBF2), secondary = p.accent,
        background = p.canvas, onBackground = p.text, surface = p.surface, onSurface = p.text,
        surfaceVariant = p.raised, onSurfaceVariant = p.muted, outline = p.hairline, outlineVariant = p.hairline,
        surfaceContainer = p.surface, surfaceContainerHigh = p.raised, surfaceContainerLow = p.surface,
        secondaryContainer = p.accentSoft, onSecondaryContainer = p.text,
    )
    CompositionLocalProvider(LocalPalette provides p, LocalMotion provides m) {
        MaterialTheme(colorScheme = scheme, typography = typography(p), content = content)
    }
}

