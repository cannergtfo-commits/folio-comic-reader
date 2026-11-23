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
    fun isNew(ch: CatalogChapter) = ch.id !in state.seen && ch.bookId !in shelfIds

    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = onRefresh,
        state = pull,
        modifier = Modifier.fillMaxSize().background(p.canvas),
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
                    Spacer(Modifier.height(18.dp))
                    Text("Discover", style = MaterialTheme.typography.displayMedium, color = p.text)
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
                        SeriesHero(
                            s, state.covers, newCount = s.chapters.count(::isNew),
                            missing = s.chapters.count { it.bookId !in shelfIds },
                            busy = s.chapters.any { it.id in state.downloads },
                            onGetAll = { onGetAll(s) },
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    items(s.chapters, key = { "c:" + it.id }) { ch ->
                        ChapterRow(
                            ch, state.covers[ch.cover], isNew(ch), ch.bookId in shelfIds, state.downloads[ch.id],
                            onGet = { onGet(ch) }, onRead = { onRead(ch) },
                        )
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
    val shape = RoundedCornerShape(22.dp)
    Box(Modifier.fillMaxWidth().clip(shape).background(p.surface)) {
        rememberCover(coverPath)?.let { bmp ->
            Image(
                bmp.asImageBitmap(), null, contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize().blur(34.dp).graphicsLayer { alpha = if (p.isInk) 0.34f else 0.22f },
            )
        }
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(p.surface, p.surface.copy(alpha = 0.9f), p.surface.copy(alpha = 0.5f)))))
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Cover(coverPath, s.title, Modifier.width(108.dp).aspectRatio(COVER_ASPECT), elevation = 20.dp)
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("SERIES · ${s.chapters.size} CHAPTERS", style = Eyebrow, color = p.accent)
                        if (newCount > 0) { Spacer(Modifier.width(8.dp)); NewPill("$newCount NEW") }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(s.title, style = MaterialTheme.typography.headlineSmall, color = p.text, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    s.author?.let {
                        Text(it, style = MaterialTheme.typography.titleMedium.copy(fontStyle = FontStyle.Italic), color = p.muted)
                    }
                    Spacer(Modifier.height(14.dp))
                    if (missing > 0) {
                        Button(
                            onClick = onGetAll, enabled = !busy,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = p.accent, contentColor = if (p.isInk) Color(0xFF1A1408) else Color.White,
                                disabledContainerColor = p.accent.copy(alpha = 0.4f),
                            ),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                            modifier = Modifier.height(38.dp),
                        ) {
                            Icon(Icons.Outlined.Download, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (missing == s.chapters.size) "Get all $missing" else "Get the other $missing", style = MaterialTheme.typography.labelLarge)
                        }
                    } else {
                        Text("All chapters are on your shelf", style = MaterialTheme.typography.labelMedium, color = p.muted)
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
    ) { Text(text, style = Eyebrow, color = if (p.isInk) Color(0xFF1A1408) else Color.White) }
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
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(enabled = progress == null) { if (onShelf) onRead() else onGet() }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Cover(coverPath, ch.title, Modifier.width(54.dp).aspectRatio(COVER_ASPECT), elevation = 8.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("CHAPTER ${ch.numberLabel}", style = Eyebrow, color = p.accent)
                    if (isNew) { Spacer(Modifier.width(8.dp)); NewPill("NEW") }
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
            AnimatedContent(
                targetState = when { progress != null -> 2; onShelf -> 1; else -> 0 },
                transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "action",
            ) { mode ->
                when (mode) {
                    2 -> Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        val pr = progress ?: -1f
                        if (pr >= 0f) CircularProgressIndicator(progress = { pr }, color = p.accent, trackColor = p.hairline, strokeWidth = 2.dp, modifier = Modifier.size(30.dp))
                        else CircularProgressIndicator(color = p.accent, trackColor = p.hairline, strokeWidth = 2.dp, modifier = Modifier.size(30.dp))
                        if (pr >= 0f) Text("${(pr * 100).roundToInt()}", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp), color = p.muted)
                    }
                    1 -> OutlinedButton(
                        onClick = onRead, border = BorderStroke(0.8.dp, p.hairline),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = p.text),
                        contentPadding = PaddingValues(horizontal = 16.dp), modifier = Modifier.height(34.dp),
                    ) { Text("Read", style = MaterialTheme.typography.labelLarge) }
                    else -> Button(
                        onClick = onGet,
                        colors = ButtonDefaults.buttonColors(containerColor = p.accentSoft, contentColor = p.accent),
                        contentPadding = PaddingValues(horizontal = 14.dp), modifier = Modifier.height(34.dp),
                    ) {
                        Icon(Icons.Outlined.Download, null, Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Get", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().padding(start = 70.dp).height(0.7.dp).background(p.hairline))
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
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(0.8.dp, p.accent.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .background(p.accentSoft).clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.SystemUpdate, null, tint = p.accent)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Folio ${app.version} is out", style = MaterialTheme.typography.titleMedium, color = p.text)
            Text(app.notes ?: "Tap to download the new version.", style = MaterialTheme.typography.bodySmall, color = p.muted, maxLines = 2)
        }
    }
}

@Composable
private fun EmptyLibrary(state: LibraryState, onRetry: () -> Unit) {
    val p = LocalPalette.current
    Column(
        Modifier.fillMaxWidth().padding(top = 60.dp, start = 14.dp, end = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.refreshing) {
            CircularProgressIndicator(color = p.accent, trackColor = p.hairline, strokeWidth = 2.dp, modifier = Modifier.size(34.dp))
        } else {
            Icon(Icons.Outlined.CloudOff, null, tint = p.muted, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(16.dp))
            Text("The library is out of reach.", style = MaterialTheme.typography.titleLarge, color = p.text, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                "Connect to the internet once and Folio keeps a copy of the catalog for offline browsing. Your shelf works offline either way.",
                style = MaterialTheme.typography.bodyMedium, color = p.muted, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 320.dp),
            )
            Spacer(Modifier.height(20.dp))
            OutlinedButton(onClick = onRetry, border = BorderStroke(0.8.dp, p.hairline), colors = ButtonDefaults.outlinedButtonColors(contentColor = p.text)) {
                Text("Try again", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** Floating two-tab switch between the shelf and the online library. */
@Composable
fun TabSwitch(discover: Boolean, newCount: Int, onShelf: () -> Unit, onDiscover: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(p.raised.copy(alpha = 0.96f))
            .border(0.7.dp, p.hairline, RoundedCornerShape(50))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TabPill("Shelf", !discover, 0, onShelf)
        TabPill("Discover", discover, newCount, onDiscover)
    }
}

@Composable
private fun TabPill(label: String, on: Boolean, badge: Int, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier.clip(RoundedCornerShape(50))
            .background(if (on) p.accent else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (on) (if (p.isInk) Color(0xFF1A1408) else Color.White) else p.muted)
        if (badge > 0) {
            Spacer(Modifier.width(7.dp))
            Box(
                Modifier.size(18.dp).background(if (on) Color(0x331A1408) else p.accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (badge > 9) "9+" else "$badge",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                    color = if (on) (if (p.isInk) Color(0xFF1A1408) else Color.White) else (if (p.isInk) Color(0xFF1A1408) else Color.White),
                )
            }
        }
    }
}
