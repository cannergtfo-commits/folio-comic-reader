package net.blazarforce.folio.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.blazarforce.folio.ui.theme.Eyebrow
import net.blazarforce.folio.ui.theme.LocalPalette

/** Gold-foil primary button with a springy press, a soft gold under-glow and an occasional glint. */
@Composable
fun GoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 44.dp,
    glint: Boolean = true,
) {
    val p = LocalPalette.current
    val src = remember { MutableInteractionSource() }
    val a by animateFloatAsState(if (enabled) 1f else 0.45f, label = "en")
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .springPress(src)
            .graphicsLayer { alpha = a }
            .shadow(if (enabled) 14.dp else 0.dp, shape, ambientColor = p.accent, spotColor = p.accent)
            .clip(shape)
            .background(p.goldBrush)
            .then(if (glint && enabled) Modifier.foilGlint(strength = 0.38f, pauseMs = 3200) else Modifier)
            .border(0.8.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.45f), Color.Transparent)), shape)
            .clickable(src, indication = null, enabled = enabled, onClick = onClick)
            .defaultMinSize(minHeight = height)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { Icon(icon, null, tint = p.onAccent, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(8.dp)) }
            Text(text, style = MaterialTheme.typography.labelLarge, color = p.onAccent, maxLines = 1)
        }
    }
}

/** Hairline-outlined secondary button with the same press physics. */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 44.dp,
    tint: Color? = null,
) {
    val p = LocalPalette.current
    val src = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(50)
    Box(
        modifier.springPress(src).clip(shape)
            .background(p.raised.copy(alpha = if (p.isInk) 0.55f else 0.8f))
            .border(0.8.dp, p.goldEdge, shape)
            .clickable(src, indication = null, onClick = onClick)
            .defaultMinSize(minHeight = height).padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { Icon(icon, null, tint = tint ?: p.accent, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(8.dp)) }
            Text(text, style = MaterialTheme.typography.labelLarge, color = tint ?: p.text, maxLines = 1)
        }
    }
}

/** Shimmering gold "NEW" badge. */
@Composable
fun NewBadge(text: String = "NEW", modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Box(
        modifier.shadow(6.dp, RoundedCornerShape(50), ambientColor = p.accent, spotColor = p.accent)
            .clip(RoundedCornerShape(50)).background(p.goldBrush)
            .foilGlint(periodMs = 1700, strength = 0.7f, pauseMs = 900)
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) { Text(text, style = Eyebrow, color = p.onAccent, maxLines = 1, softWrap = false) }
}

/** Soft warm light pooled behind content ("lamp on the desk"). */
fun Modifier.softGlow(color: Color, center: Offset = Offset(0.25f, 0.2f), radius: Float = 0.9f): Modifier = drawBehind {
    val c = Offset(size.width * center.x, size.height * center.y)
    drawCircle(Brush.radialGradient(listOf(color, Color.Transparent), c, size.maxDimension * radius), size.maxDimension * radius, c)
}

/** Gold hairline that fades out at both ends. */
@Composable
fun GoldRule(modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Box(
        modifier.fillMaxWidth().height(0.8.dp).background(
            Brush.horizontalGradient(listOf(Color.Transparent, p.accent.copy(alpha = 0.55f), p.hairline, Color.Transparent)),
        ),
    )
}

/** Card surface with a gold-edged hairline, deep soft shadow and a faint top sheen. */
fun Modifier.foilCard(shape: RoundedCornerShape = RoundedCornerShape(22.dp), elevation: Dp = 18.dp): Modifier =
    this.then(Modifier.composed {
        val p = LocalPalette.current
        shadow(elevation, shape, ambientColor = p.shadow, spotColor = p.shadow)
            .clip(shape)
            .background(p.surface)
            .border(0.8.dp, p.goldEdge, shape)
    })

/** Skeleton block with a moving shimmer (static when motion is off). */
@Composable
fun SkeletonBlock(modifier: Modifier, shape: RoundedCornerShape = RoundedCornerShape(8.dp)) {
    val p = LocalPalette.current
    val phase by loopPhase(1300, still = 0.4f)
    Box(
        modifier.clip(shape).drawBehind {
            drawRect(p.raised)
            val x = -size.width + phase * size.width * 3f
            drawRect(
                Brush.linearGradient(
                    listOf(Color.Transparent, p.accent.copy(alpha = 0.10f), Color.Transparent),
                    start = Offset(x, 0f), end = Offset(x + size.width, size.height),
                ),
            )
        },
    )
}

/** Small-caps eyebrow with a short gold dash before it. */
@Composable
fun DashEyebrow(text: String, modifier: Modifier = Modifier, color: Color? = null) {
    val p = LocalPalette.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.width(16.dp).height(1.dp).background(p.goldBrush))
        Text(text, style = Eyebrow, color = color ?: p.accent, maxLines = 1)
    }
}

@Composable
fun BoxScope.TopFade(color: Color, height: Dp = 120.dp) {
    Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(height).background(Brush.verticalGradient(listOf(color, Color.Transparent))))
}

val NoPadding = PaddingValues(0.dp)

/** Snackbar replacement: raised glass card with a gold spark, gold-edged hairline and deep shadow. */
@Composable
fun FolioToast(message: String, action: String? = null, onAction: () -> Unit = {}) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().shadow(24.dp, shape, ambientColor = p.shadow, spotColor = p.shadow).clip(shape)
            .background(p.raised).border(0.8.dp, p.goldEdge, shape)
            .then(if (action != null) Modifier.clickable(onClick = onAction) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(28.dp).background(p.goldBrush, RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
            Icon(androidx.compose.material.icons.Icons.Outlined.AutoAwesome, null, tint = p.onAccent, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = p.text, modifier = Modifier.weight(1f))
        if (action != null) {
            Spacer(Modifier.width(10.dp))
            Text(action.uppercase(), style = net.blazarforce.folio.ui.theme.Eyebrow, color = p.accent)
        }
    }
}
