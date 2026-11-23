package net.blazarforce.folio.ui.library

import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.remember
import net.blazarforce.folio.ui.common.DashEyebrow
import net.blazarforce.folio.ui.common.GoldButton
import net.blazarforce.folio.ui.common.GoldRule
import net.blazarforce.folio.ui.common.LocalMotion
import net.blazarforce.folio.ui.common.rememberEntranceTracker
import net.blazarforce.folio.ui.common.softGlow
import net.blazarforce.folio.ui.common.springPress
import net.blazarforce.folio.ui.common.staggeredEntrance

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
    val list = rememberLazyListState()
    val entrance = rememberEntranceTracker()
    val reduce = LocalMotion.current.reduce
    LazyColumn(Modifier.fillMaxSize().background(p.canvas), state = list, contentPadding = PaddingValues(bottom = 24.dp)) {
        item(key = "header") {
            Box(Modifier.fillMaxWidth().clipToBounds()) {
                // Parallax backdrop: the blurred cover drifts at half speed and dims as you scroll.
                rememberCover(series.books.first().coverPath)?.let { bmp ->
                    Image(
                        bmp.asImageBitmap(), null, contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize().graphicsLayer {
                            val off = if (list.firstVisibleItemIndex == 0) list.firstVisibleItemScrollOffset.toFloat() else size.height
                            translationY = if (reduce) 0f else off * 0.5f
                            alpha = (if (p.isInk) 0.5f else 0.34f) * (1f - (off / size.height).coerceIn(0f, 1f))
                            scaleX = 1.15f; scaleY = 1.15f
                        }.blur(36.dp),
                    )
                }
                Box(
                    Modifier.matchParentSize().background(
                        Brush.verticalGradient(listOf(p.canvas.copy(alpha = 0.35f), p.canvas.copy(alpha = 0.78f), p.canvas)),
                    ).softGlow(p.glow, androidx.compose.ui.geometry.Offset(0.3f, 0.55f), 0.6f),
                )
                Column(
                    Modifier.statusBarsPadding().padding(horizontal = 12.dp).graphicsLayer {
                        val off = if (list.firstVisibleItemIndex == 0) list.firstVisibleItemScrollOffset.toFloat() else 0f
                        if (!reduce) translationY = off * 0.18f
                    },
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = p.text)
                    }
                    Row(Modifier.padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 30.dp), verticalAlignment = Alignment.Bottom) {
                        Cover(next.coverPath, next.title, Modifier.width(140.dp).aspectRatio(COVER_ASPECT), elevation = 30.dp, heroKey = next.id)
                        Spacer(Modifier.width(20.dp))
                        Column(Modifier.weight(1f)) {
                            DashEyebrow("SERIES")
                            Spacer(Modifier.height(8.dp))
                            Text(series.name, style = MaterialTheme.typography.displayMedium, color = p.text, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(6.dp))
                            val pages = series.books.sumOf { it.pageCount }
                            Text(
                                "${series.books.size} ${(series.books.first().numberLabel ?: "issue").lowercase()}s · $pages pages",
                                style = MaterialTheme.typography.bodySmall, color = p.muted,
                            )
                            Spacer(Modifier.height(10.dp))
                            ProgressLine(series.progress, Modifier.width(120.dp))
                            Spacer(Modifier.height(14.dp))
                            GoldButton(
                                (if (next.started && !next.finished) "Continue · " else "Begin · ") + next.shortLabel,
                                { onOpen(next) }, height = 42.dp,
                            )
                        }
                    }
                }
            }
        }
        item(key = "rule") {
            Column(Modifier.padding(horizontal = 22.dp, vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Contents", style = MaterialTheme.typography.headlineSmall, color = p.text)
                    Spacer(Modifier.weight(1f))
                    Text("${series.readCount} of ${series.books.size} read", style = MaterialTheme.typography.labelMedium, color = p.muted)
                }
                Spacer(Modifier.height(10.dp))
                GoldRule()
            }
        }
        itemsIndexed(series.books, key = { _, b -> b.id }) { i, book ->
            Box(Modifier.staggeredEntrance(entrance, book.id, i)) {
                ChapterRow(book, book.id == next.id, { onOpen(book) }, { onLongPress(book) })
            }
        }
        item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChapterRow(book: Book, isNext: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val p = LocalPalette.current
    val src = remember { MutableInteractionSource() }
    Column(
        Modifier.springPress(src, 0.97f).combinedClickable(interactionSource = src, indication = null, onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 22.dp),
    ) {
        Row(
            Modifier.padding(vertical = 8.dp).then(
                if (isNext) Modifier.clip(RoundedCornerShape(16.dp)).background(p.accentSoft.copy(alpha = 0.5f)).border(0.7.dp, p.goldEdge, RoundedCornerShape(16.dp)) else Modifier,
            ).padding(horizontal = if (isNext) 10.dp else 0.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Cover(book.coverPath, book.title, Modifier.width(62.dp).aspectRatio(COVER_ASPECT), elevation = 10.dp, heroKey = book.id)
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
