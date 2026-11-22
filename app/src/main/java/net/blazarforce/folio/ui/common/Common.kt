package net.blazarforce.folio.ui.common

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.blazarforce.folio.ui.theme.LocalPalette
import net.blazarforce.folio.ui.theme.Serif
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object CoverCache {
    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }
    fun get(path: String): Bitmap? = cache.get(path)
    fun put(path: String, bmp: Bitmap) { cache.put(path, bmp) }
    fun load(path: String): Bitmap? = get(path) ?: BitmapFactory.decodeFile(path)?.also { put(path, it) }
}

@Composable
fun rememberCover(path: String?): Bitmap? {
    var bmp by remember(path) { mutableStateOf(path?.let { CoverCache.get(it) }) }
    LaunchedEffect(path) {
        if (bmp == null && path != null) bmp = withContext(Dispatchers.IO) { CoverCache.load(path) }
    }
    return bmp
}

val CoverShape = RoundedCornerShape(5.dp)
const val COVER_ASPECT = 0.75f

/**
 * A comic cover with a soft drop shadow, gentle corners, a faint spine highlight
 * and a hairline edge. Falls back to a typographic placeholder.
 */
@Composable
fun Cover(
    path: String?,
    title: String,
    modifier: Modifier = Modifier,
    elevation: Dp = 14.dp,
    dim: Float = 0f,
) {
    val p = LocalPalette.current
    val bmp = rememberCover(path)
    val alpha by animateFloatAsState(if (bmp != null) 1f else 0f, tween(420), label = "cover")
    Box(
        modifier
            .shadow(elevation, CoverShape, ambientColor = p.shadow, spotColor = p.shadow)
            .clip(CoverShape)
            .background(p.raised),
    ) {
        Placeholder(title)
        bmp?.let {
            Image(
                it.asImageBitmap(), contentDescription = title, contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.High,
                modifier = Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha },
            )
        }
        // Spine shading + hairline edge give a printed, bound feel.
        Box(
            Modifier.fillMaxSize().drawWithContent {
                drawContent()
                drawRect(
                    Brush.horizontalGradient(
                        0f to Color.Black.copy(alpha = 0.28f), 0.035f to Color.White.copy(alpha = 0.10f),
                        0.07f to Color.Transparent, 1f to Color.Transparent,
                    ),
                )
                if (dim > 0f) drawRect(Color.Black.copy(alpha = dim))
            }.border(0.6.dp, Color.White.copy(alpha = if (p.isInk) 0.08f else 0.35f), CoverShape),
        )
    }
}

@Composable
private fun Placeholder(title: String) {
    val p = LocalPalette.current
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(p.raised, p.surface)),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            title, color = p.muted, textAlign = TextAlign.Center, maxLines = 4,
            style = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 18.sp),
            modifier = Modifier.padding(12.dp),
        )
    }
}

/** A 2dp reading-progress line in the accent colour over a hairline track. */
@Composable
fun ProgressLine(progress: Float, modifier: Modifier = Modifier, height: Dp = 2.dp) {
    val p = LocalPalette.current
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(600), label = "progress")
    Canvas(modifier.fillMaxWidth().height(height)) {
        val r = size.height / 2
        drawLine(p.hairline, Offset(r, r), Offset(size.width - r, r), strokeWidth = size.height, cap = StrokeCap.Round)
        if (animated > 0f) drawLine(
            p.accent, Offset(r, r), Offset(r + (size.width - 2 * r) * animated, r),
            strokeWidth = size.height, cap = StrokeCap.Round,
        )
    }
}

/** Line-art open book used for the empty state and the brand mark. */
@Composable
fun OpenBookMark(modifier: Modifier, color: Color, strokeWidth: Dp = 1.6.dp) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val s = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val spineTop = Offset(w * 0.5f, h * 0.30f)
        val spineBot = Offset(w * 0.5f, h * 0.86f)
        fun page(dir: Float): Path = Path().apply {
            moveTo(spineTop.x, spineTop.y)
            cubicTo(w * (0.5f + dir * 0.14f), h * 0.20f, w * (0.5f + dir * 0.32f), h * 0.20f, w * (0.5f + dir * 0.44f), h * 0.25f)
            lineTo(w * (0.5f + dir * 0.44f), h * 0.80f)
            cubicTo(w * (0.5f + dir * 0.32f), h * 0.75f, w * (0.5f + dir * 0.14f), h * 0.76f, spineBot.x, spineBot.y)
            close()
        }
        drawPath(page(-1f), color, style = s)
        drawPath(page(1f), color, style = s)
        // Text lines.
        for (k in 0 until 4) {
            val y = h * (0.40f + k * 0.09f)
            drawLine(color.copy(alpha = 0.45f), Offset(w * 0.15f, y + h * 0.01f), Offset(w * 0.42f, y - h * 0.012f), strokeWidth = s.width * 0.7f, cap = StrokeCap.Round)
            drawLine(color.copy(alpha = 0.45f), Offset(w * 0.58f, y - h * 0.012f), Offset(w * 0.85f, y + h * 0.01f), strokeWidth = s.width * 0.7f, cap = StrokeCap.Round)
        }
        // Four-point star above the spine.
        val c = Offset(w * 0.5f, h * 0.11f); val r = w * 0.065f
        val star = Path().apply {
            moveTo(c.x, c.y - r); quadraticTo(c.x, c.y, c.x + r, c.y); quadraticTo(c.x, c.y, c.x, c.y + r)
            quadraticTo(c.x, c.y, c.x - r, c.y); quadraticTo(c.x, c.y, c.x, c.y - r); close()
        }
        drawPath(star, color)
    }
}

fun Size.isWide() = width > height

/** Slim editorial slider: hairline track, accent fill, small round thumb. Mirrors under RTL. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun FolioSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val p = LocalPalette.current
    val rtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
    androidx.compose.material3.Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        modifier = modifier,
        thumb = {
            Box(
                Modifier.size(16.dp).shadow(4.dp, androidx.compose.foundation.shape.CircleShape)
                    .background(p.accent, androidx.compose.foundation.shape.CircleShape),
            )
        },
        track = { state ->
            val span = (state.valueRange.endInclusive - state.valueRange.start).takeIf { it > 0f } ?: 1f
            val frac = ((state.value - state.valueRange.start) / span).coerceIn(0f, 1f)
            Canvas(Modifier.fillMaxWidth().height(2.dp)) {
                val y = size.height / 2
                drawLine(p.hairline.copy(alpha = p.hairline.alpha * 1.6f), Offset(0f, y), Offset(size.width, y), size.height, StrokeCap.Round)
                val end = size.width * frac
                if (rtl) drawLine(p.accent, Offset(size.width, y), Offset(size.width - end, y), size.height, StrokeCap.Round)
                else drawLine(p.accent, Offset(0f, y), Offset(end, y), size.height, StrokeCap.Round)
            }
        },
    )
}
