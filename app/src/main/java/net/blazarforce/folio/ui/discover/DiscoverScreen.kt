package net.blazarforce.folio.ui.discover

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.shadow
import kotlinx.coroutines.delay
import net.blazarforce.folio.ui.common.DashEyebrow
import net.blazarforce.folio.ui.common.GhostButton
import net.blazarforce.folio.ui.common.GoldButton
import net.blazarforce.folio.ui.common.LocalMotion
import net.blazarforce.folio.ui.common.NewBadge
import net.blazarforce.folio.ui.common.SkeletonBlock
import net.blazarforce.folio.ui.common.foilCard
import net.blazarforce.folio.ui.common.foilGlint
import net.blazarforce.folio.ui.common.loopPhase
import net.blazarforce.folio.ui.common.pressSpring
import net.blazarforce.folio.ui.common.rememberEntranceTracker
import net.blazarforce.folio.ui.common.softGlow
import net.blazarforce.folio.ui.common.softSpring
import net.blazarforce.folio.ui.common.springPress
import net.blazarforce.folio.ui.common.staggeredEntrance
import net.blazarforce.folio.data.AppRelease
import net.blazarforce.folio.data.CatalogChapter
import net.blazarforce.folio.data.CatalogSeries
import net.blazarforce.folio.data.LibraryState
import net.blazarforce.folio.ui.common.COVER_ASPECT
import net.blazarforce.folio.ui.common.Cover
import net.blazarforce.folio.ui.common.OpenBookMark
import net.blazarforce.folio.ui.common.rememberCover
import net.blazarforce.folio.ui.theme.Eyebrow
import net.blazarforce.folio.ui.theme.LocalPalette
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

/** Online library: series and chapters from the published catalog, one tap onto the shelf. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    state: LibraryState,
    shelfIds: Set<String>,
    update: AppRelease?,
    onRefresh: () -> Unit,
    onGet: (CatalogChapter) -> Unit,
    onGetAll: (CatalogSeries) -> Unit,
    onRead: (CatalogChapter) -> Unit,
    onUpdate: (AppRelease) -> Unit,
    now: Long = System.currentTimeMillis(),
) {
    val p = LocalPalette.current
    val cat = state.catalog
    val pull = rememberPullToRefreshState()
    val entrance = rememberEntranceTracker()
    fun isNew(ch: CatalogChapter) = ch.id !in state.seen && ch.bookId !in shelfIds

    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = onRefresh,
        state = pull,
        modifier = Modifier.fillMaxSize().background(p.canvas).softGlow(p.glow, androidx.compose.ui.geometry.Offset(0.9f, 0.04f), 0.8f),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pull, isRefreshing = state.refreshing,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding(),
                containerColor = p.raised, color = p.accent,
            )
        },
    ) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 22.dp)) {
            item(key = "head") {
                Column(Modifier.statusBarsPadding()) {
                    Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                        OpenBookMark(Modifier.size(24.dp), p.accent, 1.3.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("FOLIO", style = Eyebrow.copy(letterSpacing = Eyebrow.letterSpacing * 1.6f), color = p.text)
                    }
                    Spacer(Modifier.height(20.dp))
                    DashEyebrow("THE ONLINE LIBRARY")
                    Spacer(Modifier.height(10.dp))
                    Text("Discover", style = MaterialTheme.typography.displayLarge, color = p.text)
                    Spacer(Modifier.height(6.dp))
                    Text(statusLine(state, now), style = MaterialTheme.typography.bodyMedium, color = p.muted)
                    Spacer(Modifier.height(22.dp))
                }
            }
            if (update != null) item(key = "update") { UpdateCard(update) { onUpdate(update) }; Spacer(Modifier.height(22.dp)) }
            if (cat == null) {
                item(key = "empty") { EmptyLibrary(state, onRefresh) }
            } else {
                cat.series.forEach { s ->
                    item(key = "s:" + s.id) {
                        Box(Modifier.staggeredEntrance(entrance, "s:" + s.id, 0)) { SeriesHero(
                            s, state.covers, newCount = s.chapters.count(::isNew),
                            missing = s.chapters.count { it.bookId !in shelfIds },
                            busy = s.chapters.any { it.id in state.downloads },
                            onGetAll = { onGetAll(s) },
                        ) }
                        Spacer(Modifier.height(10.dp))
                    }
                    itemsIndexed(s.chapters, key = { _, it -> "c:" + it.id }) { i, ch ->
                        Box(Modifier.staggeredEntrance(entrance, "c:" + ch.id, i + 1)) { ChapterRow(
                            ch, state.covers[ch.cover], isNew(ch), ch.bookId in shelfIds, state.downloads[ch.id],
                            onGet = { onGet(ch) }, onRead = { onRead(ch) },
                        ) }
                    }
                    item(key = "gap:" + s.id) { Spacer(Modifier.height(30.dp)) }
                }
                item(key = "note") {
                    Text(
                        "Pull down to check for new chapters. Downloads go to your shelf and stay readable offline.",
                        style = MaterialTheme.typography.bodySmall, color = p.faint, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    )
                }
            }
            item(key = "foot") {
                Column {
                    Spacer(Modifier.height(104.dp))
                    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                }
            }
        }
    }
}

private fun statusLine(s: LibraryState, now: Long): String = when {
    s.refreshing && s.catalog == null -> "Opening the library…"
    s.refreshing -> "Checking for new chapters…"
    s.error != null && s.catalog != null -> "Offline · showing the library from ${ago(s.fetchedAt, now)}"
    s.error != null -> "You're offline."
    s.fetchedAt > 0 -> "New chapters arrive here as they're published · updated ${ago(s.fetchedAt, now)}"
    else -> "New chapters arrive here as they're published."
}

private fun ago(t: Long, now: Long): String {
    if (t <= 0) return "earlier"
    val m = ((now - t) / 60_000).coerceAtLeast(0)
    return when {
        m < 1 -> "just now"
        m < 60 -> "$m min ago"
        m < 24 * 60 -> "${m / 60} h ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(t)
    }
}

private fun prettyDate(iso: String?): String? = iso?.let {
    runCatching {
        val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(it.take(10))!!
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(d)
    }.getOrNull()
}

private fun mb(bytes: Long) = if (bytes <= 0) null else "%.1f MB".format(bytes / 1_048_576.0)

@Composable
private fun SeriesHero(
    s: CatalogSeries,
    covers: Map<String, String>,
    newCount: Int,
    missing: Int,
    busy: Boolean,
    onGetAll: () -> Unit,
) {
    val p = LocalPalette.current
    val coverPath = covers[s.cover] ?: covers[s.chapters.first().cover]
    val shape = RoundedCornerShape(26.dp)
    Box(Modifier.fillMaxWidth().foilCard(shape, 24.dp)) {
        rememberCover(coverPath)?.let { bmp ->
            Image(
                bmp.asImageBitmap(), null, contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize().blur(34.dp).graphicsLayer { alpha = if (p.isInk) 0.34f else 0.22f },
            )
        }
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(p.surface, p.surface.copy(alpha = 0.9f), p.surface.copy(alpha = 0.5f)))))
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Cover(coverPath, s.title, Modifier.width(112.dp).aspectRatio(COVER_ASPECT), elevation = 26.dp)
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${s.chapters.size} CHAPTERS", style = Eyebrow, color = p.accent, maxLines = 1)
                        if (newCount > 0) { Spacer(Modifier.width(8.dp)); NewBadge("$newCount NEW") }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(s.title, style = MaterialTheme.typography.headlineSmall, color = p.text, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    s.author?.let {
                        Text(it, style = MaterialTheme.typography.titleMedium.copy(fontStyle = FontStyle.Italic), color = p.muted)
                    }
                    Spacer(Modifier.height(14.dp))
                    if (missing > 0) {
                        GoldButton(
                            if (missing == s.chapters.size) "Get all $missing" else "Get the other $missing", onGetAll,
                            icon = Icons.Outlined.Download, enabled = !busy, height = 40.dp,
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CheckCircle, null, tint = p.accent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("All on your shelf", style = MaterialTheme.typography.labelMedium, color = p.muted)
                        }
                    }
                }
            }
            s.description?.let {
                Spacer(Modifier.height(14.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = p.muted, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun NewPill(text: String) {
    val p = LocalPalette.current
    Box(
        Modifier.background(p.accent, RoundedCornerShape(50)).padding(horizontal = 7.dp, vertical = 2.dp),
    ) { Text(text, style = Eyebrow, color = if (p.isInk) Color(0xFF1A1408) else Color.White, maxLines = 1, softWrap = false) }
}

@Composable
private fun ChapterRow(
    ch: CatalogChapter,
    coverPath: String?,
    isNew: Boolean,
    onShelf: Boolean,
    progress: Float?,
    onGet: () -> Unit,
    onRead: () -> Unit,
) {
    val p = LocalPalette.current
    val src = remember { MutableInteractionSource() }
    Column {
        Row(
            Modifier.fillMaxWidth().springPress(src, 0.975f)
                .clickable(src, null, enabled = progress == null) { if (onShelf) onRead() else onGet() }.padding(vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Cover(coverPath, ch.title, Modifier.width(56.dp).aspectRatio(COVER_ASPECT), elevation = 12.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("CHAPTER ${ch.numberLabel}", style = Eyebrow, color = p.accent)
                    if (isNew) { Spacer(Modifier.width(8.dp)); NewBadge() }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    cleanTitle(ch), style = MaterialTheme.typography.titleMedium, color = p.text,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    listOfNotNull(if (ch.pages > 0) "${ch.pages} pages" else null, mb(ch.size), prettyDate(ch.published)).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = p.muted, maxLines = 1,
                )
            }
            Spacer(Modifier.width(12.dp))
            ChapterAction(onShelf, progress, onGet, onRead)
        }
        Box(Modifier.fillMaxWidth().padding(start = 72.dp).height(0.7.dp).background(Brush.horizontalGradient(listOf(p.hairline, p.hairline, Color.Transparent))))
    }
}

/** "Chapter 3: The Last Purebloods" → "The Last Purebloods" (the eyebrow already says Chapter 3). */
private fun cleanTitle(ch: CatalogChapter): String =
    ch.title.replace(Regex("^(chapter|ch\\.?|issue|no\\.?)\\s*[0-9.]+\\s*[:\\-–—]\\s*", RegexOption.IGNORE_CASE), "")
        .ifBlank { ch.title }

@Composable
private fun UpdateCard(app: AppRelease, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().foilCard(RoundedCornerShape(18.dp), 12.dp).background(p.accentSoft)
            .foilGlint(periodMs = 3200, strength = 0.12f, pauseMs = 2400).clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).background(p.goldBrush, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.SystemUpdate, null, tint = p.onAccent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Folio ${app.version} is out", style = MaterialTheme.typography.titleMedium, color = p.text)
            Text(app.notes ?: "Tap to download the new version.", style = MaterialTheme.typography.bodySmall, color = p.muted, maxLines = 2)
        }
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = p.accent, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun EmptyLibrary(state: LibraryState, onRetry: () -> Unit) {
    val p = LocalPalette.current
    Column(
        Modifier.fillMaxWidth().padding(top = if (state.refreshing) 0.dp else 40.dp, start = if (state.refreshing) 0.dp else 14.dp, end = if (state.refreshing) 0.dp else 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.refreshing) {
            LoadingSkeleton()
        } else {
            Box(Modifier.size(120.dp).softGlow(p.glow, androidx.compose.ui.geometry.Offset(0.5f, 0.5f), 0.5f), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.CloudOff, null, tint = p.accent, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("The library is out of reach.", style = MaterialTheme.typography.titleLarge, color = p.text, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                "Connect to the internet once and Folio keeps a copy of the catalog for offline browsing. Your shelf works offline either way.",
                style = MaterialTheme.typography.bodyMedium, color = p.muted, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 320.dp),
            )
            Spacer(Modifier.height(20.dp))
            GhostButton("Try again", onRetry, icon = Icons.Outlined.Refresh)
        }
    }
}

/** Floating two-tab switch with a spring-driven gold indicator. */
@Composable
fun TabSwitch(discover: Boolean, newCount: Int, onShelf: () -> Unit, onDiscover: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val tabW = 124.dp
    val x by animateDpAsState(if (discover) tabW else 0.dp, softSpring(), label = "tab")
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .shadow(24.dp, shape, ambientColor = p.shadow, spotColor = p.shadow)
            .clip(shape)
            .background(p.raised.copy(alpha = 0.97f))
            .border(0.8.dp, p.goldEdge, shape)
            .padding(5.dp),
    ) {
        Box(
            Modifier.offset(x = x).width(tabW).height(42.dp)
                .shadow(10.dp, shape, ambientColor = p.accent, spotColor = p.accent)
                .clip(shape).background(p.goldBrush).foilGlint(periodMs = 2600, strength = 0.3f, pauseMs = 4200),
        )
        Row {
            TabPill("Shelf", !discover, 0, tabW, onShelf)
            TabPill("Discover", discover, newCount, tabW, onDiscover)
        }
    }
}

@Composable
private fun TabPill(label: String, on: Boolean, badge: Int, width: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    val p = LocalPalette.current
    val src = remember { MutableInteractionSource() }
    val fg by animateColorAsState(if (on) p.onAccent else p.muted, label = "fg")
    Row(
        Modifier.width(width).height(42.dp).springPress(src, 0.93f).clip(RoundedCornerShape(50))
            .clickable(src, null, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = fg)
        if (badge > 0) {
            Spacer(Modifier.width(7.dp))
            Box(
                Modifier.size(18.dp).background(if (on) p.onAccent.copy(alpha = 0.18f) else p.accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (badge > 9) "9+" else "$badge",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                    color = p.onAccent,
                )
            }
        }
    }
}

/** Get → animated gold progress ring → check with a burst → Read. */
@Composable
private fun ChapterAction(onShelf: Boolean, progress: Float?, onGet: () -> Unit, onRead: () -> Unit) {
    val p = LocalPalette.current
    val reduce = LocalMotion.current.reduce
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    var wasLoading by remember { mutableStateOf(false) }
    var celebrate by remember { mutableStateOf(false) }
    LaunchedEffect(progress, onShelf) {
        if (progress != null) wasLoading = true
        else if (wasLoading && onShelf) {
            wasLoading = false
            celebrate = true
            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            delay(if (reduce) 400 else 1400)
            celebrate = false
        } else wasLoading = false
    }
    val mode = when { celebrate -> 3; progress != null -> 2; onShelf -> 1; else -> 0 }
    AnimatedContent(
        targetState = mode,
        transitionSpec = {
            (fadeIn(tween(220)) + scaleIn(initialScale = 0.7f, animationSpec = pressSpring())) togetherWith
                (fadeOut(tween(160)) + scaleOut(targetScale = 0.8f))
        },
        contentAlignment = Alignment.Center,
        label = "action",
    ) { m ->
        when (m) {
            3 -> DoneBurst()
            2 -> ProgressRing(progress ?: -1f)
            1 -> GhostButton("Read", onRead, height = 36.dp)
            else -> {
                val src = remember { MutableInteractionSource() }
                Row(
                    Modifier.springPress(src, 0.9f).height(36.dp).clip(RoundedCornerShape(50))
                        .background(p.accentSoft).border(0.8.dp, p.goldEdge, RoundedCornerShape(50))
                        .clickable(src, null, onClick = onGet).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Download, null, tint = p.accent, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Get", style = MaterialTheme.typography.labelLarge, color = p.accent)
                }
            }
        }
    }
}

@Composable
private fun ProgressRing(progress: Float) {
    val p = LocalPalette.current
    val shown by animateFloatAsState(progress.coerceAtLeast(0f), tween(280), label = "ring")
    val spin by loopPhase(1100, still = 0.1f)
    Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.size(34.dp)) {
            val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.6.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            drawArc(p.hairline, 0f, 360f, false, style = stroke)
            val sweep = if (progress < 0f) 70f else 360f * shown
            val start = if (progress < 0f) spin * 360f - 90f else -90f
            drawArc(
                Brush.sweepGradient(listOf(p.goldLo, p.accent, p.goldHi, p.goldLo)),
                start, sweep, false, style = stroke,
            )
            // Bright bead at the leading edge.
            val a = Math.toRadians((start + sweep).toDouble())
            val r = size.minDimension / 2
            drawCircle(p.goldHi, 2.4.dp.toPx(), center + androidx.compose.ui.geometry.Offset((r * kotlin.math.cos(a)).toFloat(), (r * kotlin.math.sin(a)).toFloat()))
        }
        if (progress >= 0f) Text("${(shown * 100).roundToInt()}", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp), color = p.text)
        else Icon(Icons.Outlined.Download, null, tint = p.accent, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun DoneBurst() {
    val p = LocalPalette.current
    val reduce = LocalMotion.current.reduce
    val burst = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { burst.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
    Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.size(42.dp)) {
            val b = burst.value
            drawCircle(p.accent.copy(alpha = (1f - b) * 0.6f), size.minDimension / 2 * (0.6f + b * 0.9f), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx() * (1f - b) + 0.5f))
            // Eight sparks flying outwards.
            for (k in 0 until 8) {
                val ang = Math.toRadians(k * 45.0 + 22.5)
                val d0 = size.minDimension / 2 * (0.55f + b * 0.55f)
                val d1 = d0 + 5.dp.toPx() * (1f - b)
                val c = center
                drawLine(
                    p.goldHi.copy(alpha = (1f - b).coerceIn(0f, 1f)),
                    c + androidx.compose.ui.geometry.Offset((d0 * kotlin.math.cos(ang)).toFloat(), (d0 * kotlin.math.sin(ang)).toFloat()),
                    c + androidx.compose.ui.geometry.Offset((d1 * kotlin.math.cos(ang)).toFloat(), (d1 * kotlin.math.sin(ang)).toFloat()),
                    strokeWidth = 1.6.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
            }
        }
        Box(
            Modifier.size(32.dp).shadow(10.dp, CircleShape, ambientColor = p.accent, spotColor = p.accent).background(p.goldBrush, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.Check, "Added", tint = p.onAccent, modifier = Modifier.size(18.dp)) }
    }
}

@Composable
private fun LoadingSkeleton() {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().foilCard(RoundedCornerShape(26.dp), 10.dp).padding(18.dp)) {
            SkeletonBlock(Modifier.width(112.dp).aspectRatio(COVER_ASPECT), RoundedCornerShape(6.dp))
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                SkeletonBlock(Modifier.width(90.dp).height(10.dp))
                Spacer(Modifier.height(14.dp))
                SkeletonBlock(Modifier.fillMaxWidth(0.9f).height(22.dp))
                Spacer(Modifier.height(10.dp))
                SkeletonBlock(Modifier.fillMaxWidth(0.5f).height(14.dp))
                Spacer(Modifier.height(22.dp))
                SkeletonBlock(Modifier.width(130.dp).height(40.dp), RoundedCornerShape(50))
            }
        }
        repeat(4) {
            Row(Modifier.padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                SkeletonBlock(Modifier.width(56.dp).aspectRatio(COVER_ASPECT), RoundedCornerShape(5.dp))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    SkeletonBlock(Modifier.width(70.dp).height(9.dp))
                    Spacer(Modifier.height(8.dp))
                    SkeletonBlock(Modifier.fillMaxWidth(0.7f).height(16.dp))
                    Spacer(Modifier.height(8.dp))
                    SkeletonBlock(Modifier.fillMaxWidth(0.45f).height(10.dp))
                }
                SkeletonBlock(Modifier.width(64.dp).height(34.dp), RoundedCornerShape(50))
            }
        }
    }
}

/** All Get/progress/done/Read states side by side (screenshot tests and design review). */
@Composable
internal fun DownloadStatesPreview() {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        ChapterAction(false, null, {}, {})
        ProgressRing(-1f)
        ProgressRing(0.42f)
        ProgressRing(0.9f)
        DoneBurst()
        ChapterAction(true, null, {}, {})
    }
}
