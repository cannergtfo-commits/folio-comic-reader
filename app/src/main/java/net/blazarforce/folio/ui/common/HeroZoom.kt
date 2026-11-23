package net.blazarforce.folio.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.lerp
import net.blazarforce.folio.data.Book
import kotlin.math.min
import kotlin.math.roundToInt

/** A cover-to-reader zoom in flight. [returning] plays it backwards after the reader closes. */
data class HeroLaunch(val book: Book, val from: Rect, val fromStart: Boolean, val returning: Boolean = false)

private val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/**
 * Lifts the tapped cover off the shelf and grows it to fill the screen over the reader's backdrop,
 * then calls [onArrived] (launch the reader). Played in reverse it settles the cover back into place.
 */
@Composable
fun HeroZoom(hero: HeroLaunch, backdrop: Color, onArrived: () -> Unit, onReturned: () -> Unit) {
    val bmp = rememberCover(hero.book.coverPath)
    val t = remember(hero) { Animatable(if (hero.returning) 1f else 0f) }
    LaunchedEffect(hero) {
        if (hero.returning) { t.animateTo(0f, tween(380, easing = Emphasized)); onReturned() }
        else { t.animateTo(1f, tween(430, easing = Emphasized)); onArrived() }
    }
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = with(density) { maxWidth.toPx() }
        val h = with(density) { maxHeight.toPx() }
        val aspect = bmp?.let { it.width.toFloat() / it.height } ?: (hero.from.width / hero.from.height)
        val tw = min(w, h * aspect); val th = tw / aspect
        val to = Rect((w - tw) / 2, (h - th) / 2, (w + tw) / 2, (h + th) / 2)
        val v = t.value
        val r = Rect(
            lerp(hero.from.left, to.left, v), lerp(hero.from.top, to.top, v),
            lerp(hero.from.right, to.right, v), lerp(hero.from.bottom, to.bottom, v),
        )
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = v }.background(backdrop))
        if (bmp != null) {
            Image(
                bmp.asImageBitmap(), hero.book.title, contentScale = ContentScale.Crop,
                modifier = Modifier
                    .offset { IntOffset(r.left.roundToInt(), r.top.roundToInt()) }
                    .size(with(density) { r.width.toDp() }, with(density) { r.height.toDp() })
                    .shadow(lerp(18f, 0f, v).coerceAtLeast(0f).let { with(density) { it.toDp() } } * 3, CoverShape)
                    .graphicsLayer {
                        // A slight tilt that straightens out, like lifting a book off the shelf.
                        rotationY = (1f - v) * v * 18f
                        cameraDistance = 14f * density.density
                    },
            )
        }
    }
}
