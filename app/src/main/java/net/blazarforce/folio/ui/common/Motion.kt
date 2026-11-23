package net.blazarforce.folio.ui.common

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay

/**
 * Motion policy. [reduce] mirrors the system "Remove animations" setting (animator scale 0):
 * entrances, shimmer, hero zooms and 3D page turns are skipped. [ambient] gates looping effects
 * (shimmer, sheen); screenshot tests turn it off so frames are deterministic.
 */
data class FolioMotion(val reduce: Boolean = false, val ambient: Boolean = true) {
    val loops get() = ambient && !reduce

    companion object {
        /** Process-wide default for [ambient]; Robolectric tests that launch real activities switch it off. */
        @Volatile var ambientByDefault = true
    }
}

val LocalMotion = staticCompositionLocalOf { FolioMotion() }

fun Context.systemReducesMotion(): Boolean = runCatching {
    Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f ||
        Settings.Global.getFloat(contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f) == 0f
}.getOrDefault(false)

/** Bouncy-but-settled spring used for presses and indicators. */
fun <T> pressSpring() = spring<T>(dampingRatio = 0.55f, stiffness = 700f)
fun <T> softSpring() = spring<T>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)

/** Springy shrink while pressed. Pair with the same [interaction] given to clickable. */
fun Modifier.springPress(interaction: InteractionSource, pressedScale: Float = 0.955f): Modifier = composed {
    val pressed by interaction.collectIsPressedAsState()
    val reduce = LocalMotion.current.reduce
    val s by animateFloatAsState(if (pressed && !reduce) pressedScale else 1f, pressSpring(), label = "press")
    graphicsLayer { scaleX = s; scaleY = s }
}

/** Remembers which list keys already played their entrance, so scrolling back doesn't replay it. */
class EntranceTracker { val played = HashSet<Any>() }

@Composable
fun rememberEntranceTracker() = remember { EntranceTracker() }

/** Staggered rise-and-fade the first time an item appears on a screen. */
fun Modifier.staggeredEntrance(tracker: EntranceTracker, key: Any, index: Int): Modifier = composed {
    val motion = LocalMotion.current
    val first = remember(key) { !motion.reduce && tracker.played.add(key) }
    val anim = remember(key) { Animatable(if (first) 0f else 1f) }
    val rise = with(LocalDensity.current) { androidx.compose.ui.unit.Dp(22f).toPx() }
    LaunchedEffect(key) {
        if (anim.value < 1f) {
            delay(index.coerceIn(0, 9) * 45L)
            anim.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = 260f))
        }
    }
    graphicsLayer {
        val v = anim.value
        alpha = v.coerceIn(0f, 1f)
        translationY = (1f - v) * rise
        val sc = 0.97f + 0.03f * v
        scaleX = sc; scaleY = sc
    }
}

/** 0..1 phase of a slow loop, or a fixed [still] value when looping motion is off. */
@Composable
fun loopPhase(periodMs: Int, still: Float = 0.35f, delayMs: Int = 0): State<Float> {
    if (!LocalMotion.current.loops) return remember { mutableStateOf(still) }
    val t = rememberInfiniteTransition(label = "loop")
    return t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(periodMs, delayMillis = delayMs, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
}

/** A diagonal light band that sweeps across the content (gold-foil glint). */
fun Modifier.foilGlint(periodMs: Int = 2600, strength: Float = 0.55f, pauseMs: Int = 1400): Modifier = composed {
    val phase by loopPhase(periodMs, still = 0.42f, delayMs = pauseMs)
    graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }.drawWithContent {
        drawContent()
        val w = size.width
        val x = -w * 0.6f + phase * w * 2.2f
        drawRect(
            Brush.linearGradient(
                0f to Color.Transparent, 0.45f to Color.White.copy(alpha = strength), 0.55f to Color.White.copy(alpha = strength),
                1f to Color.Transparent,
                start = Offset(x, 0f), end = Offset(x + w * 0.5f, size.height),
            ),
            blendMode = BlendMode.SrcAtop,
        )
    }
}

/** Window-space bounds of covers on screen, by book id, for the cover-to-reader zoom. */
object CoverBounds {
    private val map = HashMap<String, Rect>()
    operator fun get(id: String): Rect? = map[id]
    operator fun set(id: String, r: Rect) { map[id] = r }
}

fun Modifier.trackCoverBounds(id: String?): Modifier =
    if (id == null) this else onGloballyPositioned { CoverBounds[id] = it.boundsInWindow() }
