package net.blazarforce.folio.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import net.blazarforce.folio.data.Book
import net.blazarforce.folio.ui.common.COVER_ASPECT
import net.blazarforce.folio.ui.common.Cover
import net.blazarforce.folio.ui.theme.Eyebrow
import net.blazarforce.folio.ui.theme.LocalPalette
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSheet(
    book: Book,
    onDismiss: () -> Unit,
    onRead: () -> Unit,
    onFromStart: () -> Unit,
    onToggleFinished: () -> Unit,
    onRemove: () -> Unit,
) {
    val p = LocalPalette.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = p.surface,
        scrimColor = Color.Black.copy(alpha = 0.5f),
    ) {
        Column(Modifier.padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Cover(book.coverPath, book.title, Modifier.width(76.dp).aspectRatio(COVER_ASPECT), elevation = 10.dp)
                Spacer(Modifier.width(18.dp))
                Column {
                    Text((book.series ?: "Standalone").uppercase(), style = Eyebrow, color = p.accent)
                    Spacer(Modifier.height(4.dp))
                    Text(book.title, style = MaterialTheme.typography.headlineSmall, color = p.text)
                    Spacer(Modifier.height(6.dp))
                    val size = if (book.sizeBytes > 0) " · %.1f MB".format(book.sizeBytes / 1_048_576.0) else ""
                    Text("${book.kind.name} · ${book.pageCount} pages$size", style = MaterialTheme.typography.bodySmall, color = p.muted)
                    Text(
                        "Added " + DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(book.addedAt)) +
                            if (book.effectiveRtl) " · Right-to-left" else "",
                        style = MaterialTheme.typography.bodySmall, color = p.faint,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            SheetAction(Icons.AutoMirrored.Outlined.MenuBook, if (book.inProgress) "Continue reading" else "Read", onRead)
            if (book.started) SheetAction(Icons.Outlined.RestartAlt, "Read from the beginning", onFromStart)
            SheetAction(Icons.Outlined.DoneAll, if (book.finished) "Mark as unread" else "Mark as finished", onToggleFinished)
            SheetAction(Icons.Outlined.DeleteOutline, "Remove from library", onRemove, destructive = true)
        }
    }
}

@Composable
private fun SheetAction(icon: ImageVector, label: String, onClick: () -> Unit, destructive: Boolean = false) {
    val p = LocalPalette.current
    val color = if (destructive) Color(0xFFD08770) else p.text
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (destructive) color else p.accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}
