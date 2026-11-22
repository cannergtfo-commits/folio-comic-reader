package net.blazarforce.folio.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.blazarforce.folio.data.AppTheme
import net.blazarforce.folio.data.Book
import net.blazarforce.folio.data.BookKind
import net.blazarforce.folio.data.ShelfItem
import net.blazarforce.folio.data.buildShelf
import net.blazarforce.folio.ui.common.COVER_ASPECT
import net.blazarforce.folio.ui.common.Cover
import net.blazarforce.folio.ui.common.rememberCover
import net.blazarforce.folio.ui.common.OpenBookMark
import net.blazarforce.folio.ui.common.ProgressLine
import net.blazarforce.folio.ui.theme.Eyebrow
import net.blazarforce.folio.ui.theme.LocalPalette
import java.util.Calendar

enum class ShelfFilter(val label: String) { ALL("All"), READING("Reading"), UNREAD("Unread"), FINISHED("Finished") }

private fun Book.matches(f: ShelfFilter) = when (f) {
    ShelfFilter.ALL -> true
    ShelfFilter.READING -> inProgress
    ShelfFilter.UNREAD -> !started
    ShelfFilter.FINISHED -> finished
}

fun greeting(hour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) = when (hour) {
    in 5..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    else -> "Good evening"
}

@Composable
fun LibraryScreen(
    books: List<Book>,
    theme: AppTheme,
    importing: Boolean,
    onToggleTheme: () -> Unit,
    onImportFiles: () -> Unit,
    onImportFolder: () -> Unit,
    onOpen: (Book) -> Unit,
    onOpenSeries: (String) -> Unit,
    onLongPress: (Book) -> Unit,
    hour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
) {
    val p = LocalPalette.current
    var filter by rememberSaveable { mutableStateOf(ShelfFilter.ALL) }
    val continueBook = remember(books) { books.filter { it.inProgress }.maxByOrNull { it.lastReadAt } }
    val shelf = remember(books, filter) {
        buildShelf(books.filter { it.matches(filter) })
    }

    Box(Modifier.fillMaxSize().background(p.canvas)) {
        if (books.isEmpty()) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                BrandBar(theme, importing, onToggleTheme, onImportFiles, onImportFolder)
                EmptyShelf(onImportFiles, onImportFolder)
            }
            return@Box
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(148.dp),
            contentPadding = PaddingValues(horizontal = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "brand") {
                Column(Modifier.statusBarsPadding()) {
                    BrandBar(theme, importing, onToggleTheme, onImportFiles, onImportFolder, inset = false)
                    Spacer(Modifier.height(18.dp))
                    Text("${greeting(hour)}.", style = MaterialTheme.typography.displayMedium, color = p.text)
                    Spacer(Modifier.height(6.dp))
                    val reading = books.count { it.inProgress }
                    Text(
                        buildString {
                            append(if (books.size == 1) "One book" else "${books.size} books")
                            append(" on the shelf")
                            if (reading > 0) append(" · $reading in progress")
                        },
                        style = MaterialTheme.typography.bodyMedium, color = p.muted,
                    )
                }
            }
            if (continueBook != null) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "hero") {
                    ContinueCard(continueBook) { onOpen(continueBook) }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }, key = "filters") {
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("The Shelf", style = MaterialTheme.typography.headlineSmall, color = p.text)
                        Spacer(Modifier.weight(1f))
                        FilterTabs(filter) { filter = it }
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.fillMaxWidth().height(0.7.dp).background(p.hairline))
                }
            }
            if (shelf.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "none") {
                    Text(
                        "Nothing here yet.", style = MaterialTheme.typography.titleMedium.copy(fontStyle = FontStyle.Italic),
                        color = p.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    )
                }
            }
            items(shelf, key = { it.key }) { item ->
                when (item) {
                    is ShelfItem.Single -> BookCard(item.book, { onOpen(item.book) }, { onLongPress(item.book) })
                    is ShelfItem.Series -> SeriesCard(item) { onOpenSeries(item.name) }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }, key = "foot") {
                Column {
                    Spacer(Modifier.height(24.dp))
                    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                }
            }
        }
    }
}

@Composable
private fun BrandBar(
    theme: AppTheme,
    importing: Boolean,
    onToggleTheme: () -> Unit,
    onImportFiles: () -> Unit,
    onImportFolder: () -> Unit,
    inset: Boolean = true,
) {
    val p = LocalPalette.current
    var menu by remember { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = if (inset) 22.dp else 0.dp)) {
        Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
            OpenBookMark(Modifier.size(24.dp), p.accent, 1.3.dp)
            Spacer(Modifier.width(10.dp))
            Text("FOLIO", style = Eyebrow.copy(letterSpacing = Eyebrow.letterSpacing * 1.6f), color = p.text)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onToggleTheme) {
                Icon(
                    if (theme == AppTheme.INK) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                    contentDescription = "Switch theme", tint = p.muted,
                )
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = "Import", tint = p.text)
                }
                DropdownMenu(menu, onDismissRequest = { menu = false }, containerColor = p.raised) {
                    DropdownMenuItem(
                        text = { Text("Comic files…") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Outlined.NoteAdd, null, tint = p.accent) },
                        onClick = { menu = false; onImportFiles() },
                    )
                    DropdownMenuItem(
                        text = { Text("A folder…") },
                        leadingIcon = { Icon(Icons.Outlined.FolderOpen, null, tint = p.accent) },
                        onClick = { menu = false; onImportFolder() },
                    )
                }
            }
        }
        AnimatedVisibility(importing, enter = fadeIn(), exit = fadeOut()) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(1.5.dp),
                color = p.accent, trackColor = p.hairline,
            )
        }
    }
}

@Composable
private fun FilterTabs(selected: ShelfFilter, onSelect: (ShelfFilter) -> Unit) {
    val p = LocalPalette.current
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        ShelfFilter.entries.forEach { f ->
            val on = f == selected
            val color by animateColorAsState(if (on) p.text else p.faint, label = "tab")
            val bar by animateDpAsState(if (on) 14.dp else 0.dp, label = "bar")
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(remember { MutableInteractionSource() }, null) { onSelect(f) },
            ) {
                Text(f.label, style = MaterialTheme.typography.labelMedium, color = color)
                Spacer(Modifier.height(4.dp))
                Box(Modifier.width(bar).height(1.5.dp).background(p.accent, CircleShape))
            }
        }
    }
}

@Composable
private fun ContinueCard(book: Book, onClick: () -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(20.dp)
    Box(
        Modifier.fillMaxWidth().clip(shape).background(p.surface).clickable(onClick = onClick),
    ) {
        // Blurred cover wash on the right, fading into the card.
        rememberCover(book.coverPath)?.let { bmp ->
            Image(
                bmp.asImageBitmap(), null, contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize().blur(30.dp).graphicsLayer { alpha = if (p.isInk) 0.30f else 0.22f },
            )
        }
        Box(
            Modifier.matchParentSize().background(
                Brush.horizontalGradient(listOf(p.surface, p.surface.copy(alpha = 0.92f), p.surface.copy(alpha = 0.55f))),
            ),
        )
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Cover(book.coverPath, book.title, Modifier.width(98.dp).aspectRatio(COVER_ASPECT), elevation = 18.dp)
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text("CONTINUE READING", style = Eyebrow, color = p.accent)
                Spacer(Modifier.height(8.dp))
                Text(
                    book.series ?: book.title, style = MaterialTheme.typography.headlineSmall, color = p.text,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                if (book.series != null) {
                    Text(
                        book.shortLabel, style = MaterialTheme.typography.titleMedium.copy(fontStyle = FontStyle.Italic),
                        color = p.muted,
                    )
                }
                Spacer(Modifier.height(14.dp))
                ProgressLine(book.progress)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Page ${book.lastPage + 1} of ${book.pageCount}",
                        style = MaterialTheme.typography.bodySmall, color = p.muted,
                    )
                    Spacer(Modifier.weight(1f))
                    Text("${(book.progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = p.accent)
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = p.accent, contentColor = if (p.isInk) Color(0xFF1A1408) else Color.White),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    modifier = Modifier.height(38.dp),
                ) { Text("Resume", style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookCard(book: Book, onClick: () -> Unit, onLongClick: () -> Unit) {
    val p = LocalPalette.current
    Column(
        Modifier.combinedClickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null,
            onClick = onClick, onLongClick = onLongClick,
        ),
    ) {
        Cover(book.coverPath, book.title, Modifier.fillMaxWidth().aspectRatio(COVER_ASPECT))
        Spacer(Modifier.height(12.dp))
        if (book.started) {
            ProgressLine(book.progress)
            Spacer(Modifier.height(8.dp))
        }
        Text(
            book.title, style = MaterialTheme.typography.titleMedium, color = p.text,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!book.started) {
                Box(Modifier.size(5.dp).background(p.accent, CircleShape))
                Spacer(Modifier.width(6.dp))
            }
            Text(metaLine(book), style = MaterialTheme.typography.bodySmall, color = p.muted, maxLines = 1)
        }
    }
}

fun metaLine(book: Book): String = buildString {
    append("${book.pageCount} pages")
    if (book.kind != BookKind.CBZ) append(" · ${book.kind.name.lowercase().replaceFirstChar { it.uppercase() }}")
    when {
        book.finished -> append(" · Read")
        book.inProgress -> append(" · ${(book.progress * 100).toInt()}%")
    }
}

@Composable
private fun SeriesCard(series: ShelfItem.Series, onClick: () -> Unit) {
    val p = LocalPalette.current
    Column(Modifier.clickable(remember { MutableInteractionSource() }, null, onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(COVER_ASPECT)) {
            val stack = series.books.take(3)
            // Back issues peek out behind the front cover, like a fanned stack.
            stack.drop(1).reversed().forEachIndexed { i, b ->
                val depth = stack.size - 1 - i
                Cover(
                    b.coverPath, b.title,
                    Modifier.fillMaxSize().padding(start = (depth * 7).dp, bottom = (depth * 7).dp)
                        .offset(x = (depth * 4).dp, y = (-depth * 3).dp)
                        .graphicsLayer { rotationZ = depth * 2.2f; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 1f) },
                    elevation = 6.dp, dim = 0.18f * depth,
                )
            }
            Cover(
                stack.first().coverPath, stack.first().title,
                Modifier.fillMaxSize().padding(end = ((stack.size - 1) * 7).dp, top = ((stack.size - 1) * 7).dp),
            )
            Box(
                Modifier.align(Alignment.BottomStart).padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text("${series.books.size}", style = MaterialTheme.typography.labelMedium, color = Color(0xFFEFE8D8))
            }
        }
        Spacer(Modifier.height(12.dp))
        if (series.books.any { it.started }) {
            ProgressLine(series.progress)
            Spacer(Modifier.height(8.dp))
        }
        Text(series.name, style = MaterialTheme.typography.titleMedium, color = p.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(3.dp))
        val label = (series.books.first().numberLabel ?: "Issue").lowercase()
        Text(
            "${series.books.size} ${label}s · ${series.readCount} read",
            style = MaterialTheme.typography.bodySmall, color = p.muted,
        )
    }
}

@Composable
private fun EmptyShelf(onImportFiles: () -> Unit, onImportFolder: () -> Unit) {
    val p = LocalPalette.current
    Column(
        Modifier.fillMaxSize().padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(170.dp).drawWithContent {
                    drawCircle(Brush.radialGradient(listOf(p.accentSoft, Color.Transparent)))
                },
            )
            OpenBookMark(Modifier.size(112.dp), p.accent, 1.5.dp)
        }
        Spacer(Modifier.height(18.dp))
        Text("A quiet shelf.", style = MaterialTheme.typography.displaySmall, color = p.text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            "Bring in your comics — CBZ, PDF, or a folder of page images. Everything stays on this device.",
            style = MaterialTheme.typography.bodyMedium, color = p.muted, textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp),
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onImportFiles,
            colors = ButtonDefaults.buttonColors(containerColor = p.accent, contentColor = if (p.isInk) Color(0xFF1A1408) else Color.White),
            modifier = Modifier.height(48.dp).widthIn(min = 220.dp),
        ) {
            Icon(Icons.AutoMirrored.Outlined.NoteAdd, null, Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text("Import comics", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onImportFolder,
            border = androidx.compose.foundation.BorderStroke(0.8.dp, p.hairline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = p.text),
            modifier = Modifier.height(48.dp).widthIn(min = 220.dp),
        ) {
            Icon(Icons.Outlined.FolderOpen, null, Modifier.size(18.dp), tint = p.muted)
            Spacer(Modifier.width(10.dp))
            Text("Choose a folder", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(80.dp))
    }
}
