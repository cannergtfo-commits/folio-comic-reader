package net.blazarforce.folio.ui.reader

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.launch

@Stable
class ZoomState {
    var scale by mutableFloatStateOf(1f)
    var offset by mutableStateOf(Offset.Zero)
    val zoomed: Boolean get() = scale > 1.01f

    internal fun clamp(o: Offset, size: IntSize, s: Float = scale): Offset {
        val maxX = (s - 1f) * size.width / 2f
        val maxY = (s - 1f) * size.height / 2f
        return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
    }

    suspend fun animateTo(targetScale: Float, focus: Offset?, size: IntSize) {
        val startScale = scale
        val startOffset = offset
        val center = Offset(size.width / 2f, size.height / 2f)
        val endOffset = if (focus == null || targetScale <= 1f) Offset.Zero
        else clamp((center - focus) * (targetScale - 1f), size, targetScale)
        animate(0f, 1f, animationSpec = tween(320, easing = FastOutSlowInEasing)) { t, _ ->
            scale = startScale + (targetScale - startScale) * t
            offset = startOffset + (endOffset - startOffset) * t
        }
    }

    fun reset() { scale = 1f; offset = Offset.Zero }
}

/**
 * Pinch / double-tap zoom container that leaves single-finger swipes to the parent pager
 * while unzoomed, and pans freely once zoomed.
 */
@Composable
fun Zoomable(
    state: ZoomState,
    modifier: Modifier = Modifier,
    onTap: (Offset, IntSize) -> Unit,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    Box(
        modifier
            .pointerInput(state) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.count { it.pressed }
                        if (pressed >= 2 || state.zoomed) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val centroid = event.calculateCentroid(useCurrent = true)
                            val old = state.scale
                            val new = (old * zoom).coerceIn(0.85f, 5f)
                            val c = Offset(size.width / 2f, size.height / 2f)
                            val focus = if (centroid == Offset.Unspecified) c else centroid
                            val o = focus - c - (focus - c - state.offset) * (new / old) + pan
                            state.scale = new
                            state.offset = state.clamp(o, size, maxOf(new, 1f))
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                    if (state.scale < 1.03f && state.scale != 1f) {
                        scope.launch { state.animateTo(1f, null, size) }
                    }
                }
            }
            .pointerInput(state) {
                detectTapGestures(
                    onDoubleTap = { pos ->
                        scope.launch {
                            if (state.zoomed) state.animateTo(1f, null, size) else state.animateTo(2.5f, pos, size)
                        }
                    },
                    onTap = { pos -> onTap(pos, size) },
                )
            }
            .graphicsLayer {
                scaleX = state.scale
                scaleY = state.scale
                translationX = state.offset.x
                translationY = state.offset.y
            },
    ) { content() }
}
