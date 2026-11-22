package net.blazarforce.folio.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import net.blazarforce.folio.comic.PageLoader
import net.blazarforce.folio.comic.buildSpreads
import net.blazarforce.folio.comic.singlePages
import net.blazarforce.folio.comic.unitOf
import net.blazarforce.folio.data.Book
import net.blazarforce.folio.data.PageTurn
import net.blazarforce.folio.data.ReadingMode
import net.blazarforce.folio.data.Settings
import net.blazarforce.folio.ui.common.COVER_ASPECT
import net.blazarforce.folio.ui.common.Cover
import net.blazarforce.folio.ui.common.FolioSlider
import net.blazarforce.folio.ui.common.OpenBookMark
import net.blazarforce.folio.ui.theme.Eyebrow
import net.blazarforce.folio.ui.theme.LocalPalette
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    book: Book,
    loader: PageLoader,
    thumbs: PageLoader,
    settings: Settings,
    nextBook: Book?,
    onPageChanged: (Int) -> Unit,
    onBack: () -> Unit,
    onSettings: ((Settings) -> Settings) -> Unit,
    onSetRtl: (Boolean) -> Unit,
    onOpenNext: (Book) -> Unit,
    onUiVisible: (Boolean) -> Unit,
    initialPage: Int = book.lastPage,
    initialUi: Boolean = true,
    autoHideUi: Boolean = true,
    initialSheet: Boolean = false,
) {
    val p = LocalPalette.current
    val rtl = book.effectiveRtl
    val count = loader.pageCount
    var page by remember { mutableIntStateOf(initialPage.coerceIn(0, (count - 1).coerceAtLeast(0))) }
    var atEnd by remember { mutableStateOf(false) }
    var uiVisible by remember { mutableStateOf(initialUi) }
    var showSheet by remember { mutableStateOf(initialSheet) }
    var touched by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val readerBg = if (p.isInk) Color(0xFF0A0908) else Color(0xFFEDE7DB)

    LaunchedEffect(uiVisible) { onUiVisible(uiVisible) }
    LaunchedEffect(Unit) {
        if (autoHideUi) { delay(1800); if (!touched) uiVisible = false }
    }
    LaunchedEffect(page) {
        loader.preload(page, 2)
        onPageChanged(page)
    }
    var lastTick by remember { mutableIntStateOf(page) }
    LaunchedEffect(page) {
        if (page != lastTick && settings.haptics) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        lastTick = page
    }

    // Navigation requests from the overlay are funnelled through this; each mode installs a handler.
    var jumpTo by remember { mutableStateOf<((Int) -> Unit)?>(null) }

    BoxWithConstraints(Modifier.fillMaxSize().background(readerBg)) {
        val wide = maxWidth > maxHeight
        val mode = when {
            settings.readingMode == ReadingMode.WEBTOON -> ReadingMode.WEBTOON
            settings.readingMode == ReadingMode.SPREAD -> ReadingMode.SPREAD
            wide && settings.autoSpreadLandscape -> ReadingMode.SPREAD
            else -> ReadingMode.PAGED
        }
        val toggleUi = { touched = true; uiVisible = !uiVisible }

        if (mode == ReadingMode.WEBTOON) {
            WebtoonReader(
                book, loader, page, nextBook, readerBg,
                onPage = { page = it },
                onEnd = { atEnd = it },
                onToggleUi = toggleUi,
                onOpenNext = onOpenNext, onBack = onBack,
                register = { jumpTo = it },
            )
        } else {
            val units = remember(mode, count, book.coverIndex, book.doublePages) {
                if (mode == ReadingMode.SPREAD) buildSpreads(count, book.coverIndex, book.doublePages) else singlePages(count)
            }
            key(mode) {
                PagedReader(
                    book, loader, units, page, rtl, settings.pageTurn, nextBook, readerBg,
                    onPage = { page = it }, onEnd = { atEnd = it },
                    onToggleUi = toggleUi, onOpenNext = onOpenNext, onBack = onBack,
                    register = { jumpTo = it },
                )
            }
        }

        // Warm "night paper" filter.
        if (settings.warmth > 0.01f) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Color(0xFFFF9E45).copy(alpha = settings.warmth * 0.32f), blendMode = BlendMode.Multiply)
                drawRect(Color(0xFF2A1400).copy(alpha = settings.warmth * 0.10f))
            }
        }

        ReaderOverlay(
            visible = uiVisible && !atEnd,
            book = book, page = page, count = count, rtl = rtl, thumbs = thumbs,
            onBack = onBack,
            onSettings = { touched = true; showSheet = true },
            onToggleRtl = { onSetRtl(!rtl) },
            onJump = { target -> touched = true; jumpTo?.invoke(target) },
        )
    }

    if (showSheet) {
        ReaderSettingsSheet(
            settings = settings, rtl = rtl,
            onDismiss = { showSheet = false },
            onSettings = onSettings, onSetRtl = onSetRtl,
        )
    }
}

@Composable
private fun key(k: Any, content: @Composable () -> Unit) = androidx.compose.runtime.key(k) { content() }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PagedReader(
    book: Book,
    loader: PageLoader,
    units: List<IntArray>,
    startPage: Int,
    rtl: Boolean,
    turn: PageTurn,
    nextBook: Book?,
    bg: Color,
    onPage: (Int) -> Unit,
    onEnd: (Boolean) -> Unit,
    onToggleUi: () -> Unit,
    onOpenNext: (Book) -> Unit,
    onBack: () -> Unit,
    register: (((Int) -> Unit)?) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val state = rememberPagerState(initialPage = units.unitOf(startPage)) { units.size + 1 }
    val zooms = remember(units) { HashMap<Int, ZoomState>() }
    val currentZoom = zooms.getOrPut(state.currentPage) { ZoomState() }

    LaunchedEffect(state) {
        snapshotFlow { state.currentPage }.distinctUntilChanged().collect { u ->
            zooms.values.forEach { it.reset() }
            if (u < units.size) { onPage(units[u].first()); onEnd(false) } else onEnd(true)
        }
    }
    suspend fun go(unit: Int) {
        val target = unit.coerceIn(0, units.size)
        when {
            turn == PageTurn.NONE || abs(target - state.currentPage) > 2 -> state.scrollToPage(target)
            else -> state.animateScrollToPage(target, animationSpec = tween(420, easing = FastOutSlowInEasing))
        }
    }
    LaunchedEffect(units) {
        register { page -> scope.launch { go(units.unitOf(page)) } }
    }

    HorizontalPager(
        state = state,
        reverseLayout = rtl,
        beyondViewportPageCount = 1,
        userScrollEnabled = !currentZoom.zoomed,
        key = { it },
        modifier = Modifier.fillMaxSize(),
    ) { u ->
        val pageOffset = (state.currentPage - u) + state.currentPageOffsetFraction
        val layer = Modifier.graphicsLayer {
            when (turn) {
                PageTurn.FADE -> {
                    // Mostly cancel the slide (a 12% drift remains) and cross-fade instead.
                    translationX = pageOffset * size.width * 0.88f * (if (rtl) -1f else 1f)
                    alpha = 1f - abs(pageOffset).coerceIn(0f, 1f)
                }
                PageTurn.SLIDE -> {
                    // A whisper of depth on the outgoing page.
                    val f = abs(pageOffset).coerceIn(0f, 1f)
                    alpha = 1f - f * 0.25f
                }
                PageTurn.NONE -> Unit
            }
        }
        if (u >= units.size) {
            EndCard(book, nextBook, onOpenNext, onBack, onAgain = { scope.launch { state.scrollToPage(0) } }, modifier = layer)
            return@HorizontalPager
        }
        val zoom = zooms.getOrPut(u) { ZoomState() }
        Zoomable(
            zoom,
            modifier = layer.fillMaxSize(),
            onTap = { pos, size ->
                val x = pos.x / size.width
                val forward = if (rtl) x < 0.3f else x > 0.7f
                val backward = if (rtl) x > 0.7f else x < 0.3f
                when {
                    forward -> scope.launch { go(state.currentPage + 1) }
                    backward -> scope.launch { go(state.currentPage - 1) }
                    else -> onToggleUi()
                }
            },
        ) {
            val pages = units[u]
            if (pages.size == 1) {
                PageImage(loader, pages[0], Modifier.fillMaxSize(), Alignment.Center)
            } else {
                val ordered = if (rtl) pages.reversed() else pages.toList()
                Box(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxSize()) {
                        PageImage(loader, ordered[0], Modifier.weight(1f).fillMaxHeight(), Alignment.CenterEnd)
                        PageImage(loader, ordered[1], Modifier.weight(1f).fillMaxHeight(), Alignment.CenterStart)
                    }
                    // Soft gutter shadow where the pages meet.
                    Box(
                        Modifier.align(Alignment.Center).width(28.dp).fillMaxHeight().background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.16f), Color.Transparent),
                            ),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
fun PageImage(loader: PageLoader, index: Int, modifier: Modifier, alignment: Alignment) {
    val p = LocalPalette.current
    val bmp by produceState(loader.peek(index), index) {
        if (value == null) value = loader.load(index)
    }
    val alpha by animateFloatAsState(if (bmp != null) 1f else 0f, tween(260), label = "page")
    Box(modifier, contentAlignment = Alignment.Center) {
        if (bmp == null) {
            Text(
                "${index + 1}", color = p.faint.copy(alpha = 0.5f),
                style = MaterialTheme.typography.displayMedium.copy(fontStyle = FontStyle.Italic),
            )
        }
        bmp?.let {
            Image(
                it.asImageBitmap(), contentDescription = "Page ${index + 1}",
                contentScale = ContentScale.Fit, alignment = alignment,
                filterQuality = FilterQuality.High,
                modifier = Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha },
            )
        }
    }
}

@Composable
private fun WebtoonReader(
    book: Book,
    loader: PageLoader,
    startPage: Int,
    nextBook: Book?,
    bg: Color,
    onPage: (Int) -> Unit,
    onEnd: (Boolean) -> Unit,
    onToggleUi: () -> Unit,
    onOpenNext: (Book) -> Unit,
    onBack: () -> Unit,
    register: (((Int) -> Unit)?) -> Unit,
) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = startPage)
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    LaunchedEffect(state) {
        snapshotFlow { state.firstVisibleItemIndex }.distinctUntilChanged().collect { i ->
            if (i < loader.pageCount) { onPage(i); onEnd(false) } else onEnd(true)
        }
    }
    LaunchedEffect(Unit) { register { page -> scope.launch { state.scrollToItem(page) } } }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val h = with(density) { maxHeight.toPx() }
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                detectTapGestures { pos ->
                    val y = pos.y / size.height
                    when {
                        y > 0.75f -> scope.launch { state.animateScrollBy(h * 0.8f, tween(380)) }
                        y < 0.25f -> scope.launch { state.animateScrollBy(-h * 0.8f, tween(380)) }
                        else -> onToggleUi()
                    }
                }
            },
        ) {
            items(loader.pageCount) { i ->
                val aspect by produceState(0.75f, i) {
                    value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { loader.aspect(i) } ?: 0.75f
                }
                PageImage(loader, i, Modifier.fillMaxWidth().aspectRatio(aspect), Alignment.Center)
            }
            item {
                EndCard(book, nextBook, onOpenNext, onBack, onAgain = { scope.launch { state.scrollToItem(0) } },
                    modifier = Modifier.fillMaxWidth().height(maxHeight))
            }
        }
    }
}

@Composable
private fun EndCard(
    book: Book,
    next: Book?,
    onOpenNext: (Book) -> Unit,
    onBack: () -> Unit,
    onAgain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalPalette.current
    Column(
        modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        OpenBookMark(Modifier.size(44.dp), p.accent, 1.2.dp)
        Spacer(Modifier.height(18.dp))
        Text("Fin.", style = MaterialTheme.typography.displayLarge.copy(fontStyle = FontStyle.Italic), color = p.text)
        Spacer(Modifier.height(6.dp))
        Text(
            "You've reached the end of ${book.shortLabel.lowercase().let { if (book.number != null) it else book.title }}",
            style = MaterialTheme.typography.bodyMedium, color = p.muted, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(30.dp))
        if (next != null) {
            Text("UP NEXT", style = Eyebrow, color = p.accent)
            Spacer(Modifier.height(12.dp))
            Cover(next.coverPath, next.title, Modifier.width(118.dp).aspectRatio(COVER_ASPECT).clickable { onOpenNext(next) }, elevation = 20.dp)
            Spacer(Modifier.height(14.dp))
            Text(next.title, style = MaterialTheme.typography.titleLarge, color = p.text, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = { onOpenNext(next) },
                colors = ButtonDefaults.buttonColors(containerColor = p.accent, contentColor = if (p.isInk) Color(0xFF1A1408) else Color.White),
            ) { Text("Continue to ${next.shortLabel}", style = MaterialTheme.typography.labelLarge) }
        } else {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = p.accent, contentColor = if (p.isInk) Color(0xFF1A1408) else Color.White),
            ) { Text("Back to the shelf", style = MaterialTheme.typography.labelLarge) }
        }
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = onAgain) { Text("Read again", color = p.muted, style = MaterialTheme.typography.labelLarge) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReaderOverlay(
    visible: Boolean,
    book: Book,
    page: Int,
    count: Int,
    rtl: Boolean,
    thumbs: PageLoader,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onToggleRtl: () -> Unit,
    onJump: (Int) -> Unit,
) {
    val p = LocalPalette.current
    val scrim = p.canvas
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible, Modifier.align(Alignment.TopCenter),
            enter = fadeIn(tween(240)) + slideInVertically(tween(280)) { -it / 4 },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(240)) { -it / 4 },
        ) {
            Box(
                Modifier.fillMaxWidth().background(
                    Brush.verticalGradient(listOf(scrim.copy(alpha = 0.94f), scrim.copy(alpha = 0.70f), Color.Transparent)),
                ),
            ) {
                Row(
                    Modifier.windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility)
                        .padding(start = 6.dp, end = 10.dp, top = 6.dp, bottom = 28.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = p.text) }
                    Column(Modifier.weight(1f).padding(start = 4.dp)) {
                        Text((book.series ?: "Folio").uppercase(), style = Eyebrow, color = p.accent, maxLines = 1)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (book.series != null) book.shortLabel else book.title,
                            style = MaterialTheme.typography.titleLarge, color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        if (rtl) "RTL" else "LTR", style = Eyebrow, color = if (rtl) p.accent else p.muted,
                        modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onToggleRtl)
                            .border(0.7.dp, if (rtl) p.accent.copy(alpha = 0.6f) else p.hairline, RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = onSettings) { Icon(Icons.Outlined.Tune, "Reading settings", tint = p.text) }
                }
            }
        }

        AnimatedVisibility(
            visible, Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(tween(240)) + slideInVertically(tween(280)) { it / 4 },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(240)) { it / 4 },
        ) {
            BottomBar(page, count, rtl, thumbs, scrim, onJump)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun BottomBar(page: Int, count: Int, rtl: Boolean, thumbs: PageLoader, scrim: Color, onJump: (Int) -> Unit) {
    val p = LocalPalette.current
    var dragging by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(page.toFloat()) }
    LaunchedEffect(page) { if (!dragging) sliderValue = page.toFloat() }
    val shown = if (dragging) sliderValue.roundToInt() else page
    val strip = rememberLazyListState()
    LaunchedEffect(shown) {
        val target = (shown - 2).coerceAtLeast(0)
        if (dragging) strip.scrollToItem(target) else strip.animateScrollToItem(target)
    }
    val dir = if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    Box(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().background(
                Brush.verticalGradient(listOf(Color.Transparent, scrim.copy(alpha = 0.78f), scrim.copy(alpha = 0.96f))),
            ).windowInsetsPadding(WindowInsets.navigationBarsIgnoringVisibility)
                .padding(top = 40.dp, bottom = 10.dp),
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides dir) {
                LazyRow(
                    state = strip,
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxWidth().height(86.dp),
                ) {
                    items(count) { i ->
                        val selected = i == shown
                        val s by animateFloatAsState(if (selected) 1f else 0.86f, tween(220), label = "thumb")
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier.height(66.dp).aspectRatio(0.75f)
                                    .graphicsLayer { scaleX = s; scaleY = s; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f) }
                                    .shadow(if (selected) 10.dp else 2.dp, RoundedCornerShape(3.dp))
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(p.raised)
                                    .then(if (selected) Modifier.border(1.2.dp, p.accent, RoundedCornerShape(3.dp)) else Modifier)
                                    .clickable { onJump(i) },
                            ) {
                                Thumb(thumbs, i, Modifier.fillMaxSize())
                                if (!selected) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${i + 1}", style = MaterialTheme.typography.labelSmall,
                                color = if (selected) p.accent else p.faint,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.widthIn(min = 64.dp)) {
                            Text("${shown + 1}", style = MaterialTheme.typography.headlineSmall, color = p.text)
                            Text(" / $count", style = MaterialTheme.typography.bodySmall, color = p.muted, modifier = Modifier.padding(bottom = 3.dp))
                        }
                    }
                    FolioSlider(
                        value = sliderValue,
                        onValueChange = { dragging = true; sliderValue = it },
                        onValueChangeFinished = { dragging = false; onJump(sliderValue.roundToInt()) },
                        valueRange = 0f..(count - 1).coerceAtLeast(1).toFloat(),
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    )
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            "${if (count <= 1) 100 else (shown * 100 / (count - 1))}%",
                            style = MaterialTheme.typography.labelMedium, color = p.muted,
                            textAlign = TextAlign.End, modifier = Modifier.width(40.dp),
                        )
                    }
                }
            }
        }
        // Floating preview while scrubbing.
        AnimatedVisibility(
            dragging, Modifier.align(Alignment.TopCenter).padding(bottom = 8.dp),
            enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.92f), exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.92f),
        ) {
            Column(
                Modifier.graphicsLayer { translationY = -size.height * 0.95f }
                    .shadow(24.dp, RoundedCornerShape(10.dp)).clip(RoundedCornerShape(10.dp))
                    .background(p.raised).padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Thumb(thumbs, shown, Modifier.height(170.dp).aspectRatio(0.75f).clip(RoundedCornerShape(4.dp)))
                Spacer(Modifier.height(6.dp))
                Text("Page ${shown + 1}", style = MaterialTheme.typography.labelMedium, color = p.text)
            }
        }
    }
}

@Composable
private fun Thumb(thumbs: PageLoader, index: Int, modifier: Modifier) {
    val bmp by produceState(thumbs.peek(index), index) { if (value == null) value = thumbs.load(index) }
    Box(modifier) {
        bmp?.let {
            Image(it.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(), filterQuality = FilterQuality.Medium)
        }
    }
}
