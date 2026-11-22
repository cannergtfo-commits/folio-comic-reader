package net.blazarforce.folio.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.blazarforce.folio.data.Book
import net.blazarforce.folio.data.BookKind
import net.blazarforce.folio.data.ShelfItem
import net.blazarforce.folio.ui.common.COVER_ASPECT
import net.blazarforce.folio.ui.common.Cover
import net.blazarforce.folio.ui.common.ProgressLine
import net.blazarforce.folio.ui.common.rememberCover
import net.blazarforce.folio.ui.theme.Eyebrow
import net.blazarforce.folio.ui.theme.LocalPalette

@Composable
fun SeriesScreen(
    series: ShelfItem.Series,
    onBack: () -> Unit,
    onOpen: (Book) -> Unit,
    onLongPress: (Book) -> Unit,
) {
    val p = LocalPalette.current
    val next = series.next
    LazyColumn(Modifier.fillMaxSize().background(p.canvas), contentPadding = PaddingValues(bottom = 24.dp)) {
        item(key = "header") {
            Box(Modifier.fillMaxWidth()) {
                rememberCover(series.books.first().coverPath)?.let { bmp ->
                    Image(
                        bmp.asImageBitmap(), null, contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize().blur(40.dp).graphicsLayer { alpha = if (p.isInk) 0.35f else 0.25f },
                    )
                }
                Box(
                    Modifier.matchParentSize().background(
                        Brush.verticalGradient(listOf(p.canvas.copy(alpha = 0.55f), p.canvas.copy(alpha = 0.85f), p.canvas)),
                    ),
                )
                Column(Modifier.statusBarsPadding().padding(horizontal = 12.dp)) {
                    IconButton(onClick = onBack, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = p.text)
                    }
                    Row(Modifier.padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 28.dp), verticalAlignment = Alignment.Bottom) {
                        Cover(next.coverPath, next.title, Modifier.width(132.dp).aspectRatio(COVER_ASPECT), elevation = 24.dp)
                        Spacer(Modifier.width(20.dp))
                        Column(Modifier.weight(1f)) {
                            Text("SERIES", style = Eyebrow, color = p.accent)
                            Spacer(Modifier.height(6.dp))
                            Text(series.name, style = MaterialTheme.typography.displayMedium, color = p.text, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(6.dp))
                            val pages = series.books.sumOf { it.pageCount }
                            Text(
                                "${series.books.size} ${(series.books.first().numberLabel ?: "issue").lowercase()}s · $pages pages",
                                style = MaterialTheme.typography.bodySmall, color = p.muted,
                            )
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = { onOpen(next) },
                                colors = ButtonDefaults.buttonColors(containerColor = p.accent, contentColor = if (p.isInk) Color(0xFF1A1408) else Color.White),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                                modifier = Modifier.height(40.dp),
                            ) {
                                Text(
                                    (if (next.started && !next.finished) "Continue · " else "Begin · ") + next.shortLabel,
                                    style = MaterialTheme.typography.labelLarge, maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
        item(key = "rule") {
            Row(Modifier.padding(horizontal = 22.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Contents", style = MaterialTheme.typography.headlineSmall, color = p.text)
                Spacer(Modifier.weight(1f))
                Text("${series.readCount} of ${series.books.size} read", style = MaterialTheme.typography.labelMedium, color = p.muted)
            }
        }
        items(series.books, key = { it.id }) { book ->
            ChapterRow(book, book.id == next.id, { onOpen(book) }, { onLongPress(book) })
        }
        item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChapterRow(book: Book, isNext: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val p = LocalPalette.current
    Column(Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(horizontal = 22.dp)) {
        Box(Modifier.fillMaxWidth().height(0.7.dp).background(p.hairline))
        Row(Modifier.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Cover(book.coverPath, book.title, Modifier.width(62.dp).aspectRatio(COVER_ASPECT), elevation = 8.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(book.shortLabel.uppercase(), style = Eyebrow, color = if (isNext) p.accent else p.muted)
                    if (book.kind != BookKind.CBZ) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            book.kind.name, style = Eyebrow, color = p.faint,
                            modifier = Modifier.border(0.6.dp, p.hairline, RoundedCornerShape(3.dp)).padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(book.title, style = MaterialTheme.typography.titleLarge, color = p.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    when {
                        book.finished -> {
                            Icon(Icons.Outlined.CheckCircle, null, tint = p.accent, modifier = Modifier.height(14.dp))
                            Text("Finished · ${book.pageCount} pages", style = MaterialTheme.typography.bodySmall, color = p.muted)
                        }
                        book.started -> {
                            ProgressLine(book.progress, Modifier.width(90.dp))
                            Text("Page ${book.lastPage + 1} of ${book.pageCount}", style = MaterialTheme.typography.bodySmall, color = p.muted)
                        }
                        else -> Text(
                            "Unread · ${book.pageCount} pages",
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Normal), color = p.muted,
                        )
                    }
                }
            }
        }
    }
}
