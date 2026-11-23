package net.blazarforce.folio.ui.reader

import androidx.compose.runtime.remember
import net.blazarforce.folio.ui.common.springPress

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BrightnessLow
import androidx.compose.material.icons.outlined.CropPortrait
import androidx.compose.material.icons.outlined.ViewDay
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import net.blazarforce.folio.data.PageTurn
import net.blazarforce.folio.data.ReadingMode
import net.blazarforce.folio.data.Settings
import net.blazarforce.folio.ui.common.FolioSlider
import net.blazarforce.folio.ui.theme.Eyebrow
import net.blazarforce.folio.ui.theme.LocalPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    settings: Settings,
    rtl: Boolean,
    onDismiss: () -> Unit,
    onSettings: ((Settings) -> Settings) -> Unit,
    onSetRtl: (Boolean) -> Unit,
) {
    val p = LocalPalette.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = p.surface,
        scrimColor = Color.Black.copy(alpha = 0.35f),
    ) {
        ReaderSettingsContent(settings, rtl, onSettings, onSetRtl)
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ReaderSettingsContent(
    settings: Settings,
    rtl: Boolean,
    onSettings: ((Settings) -> Settings) -> Unit,
    onSetRtl: (Boolean) -> Unit,
) {
    val p = LocalPalette.current
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 16.dp),
    ) {
        Text("Reading", style = MaterialTheme.typography.displaySmall, color = p.text)
        Text("Make the page yours.", style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), color = p.muted)
        Spacer(Modifier.height(18.dp))

        Section("LAYOUT")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile(Icons.Outlined.CropPortrait, "Single", settings.readingMode == ReadingMode.PAGED) { onSettings { it.copy(readingMode = ReadingMode.PAGED) } }
            Tile(Icons.Outlined.AutoStories, "Spread", settings.readingMode == ReadingMode.SPREAD) { onSettings { it.copy(readingMode = ReadingMode.SPREAD) } }
            Tile(Icons.Outlined.ViewDay, "Scroll", settings.readingMode == ReadingMode.WEBTOON) { onSettings { it.copy(readingMode = ReadingMode.WEBTOON) } }
        }
        Spacer(Modifier.height(4.dp))
        SwitchRow("Two pages in landscape", settings.autoSpreadLandscape) { v -> onSettings { it.copy(autoSpreadLandscape = v) } }

        Section("DIRECTION")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile(Icons.AutoMirrored.Outlined.ArrowForward, "Left to right", !rtl) { onSetRtl(false) }
            Tile(Icons.AutoMirrored.Outlined.ArrowBack, "Right to left", rtl) { onSetRtl(true) }
        }

        Section("PAGE TURN")
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PageTurn.entries.forEach { t ->
                Pill(
                    when (t) {
                        PageTurn.SLIDE -> "Slide"; PageTurn.BOOK -> "Page flip"; PageTurn.DEPTH -> "Depth"
                        PageTurn.FADE -> "Fade"; PageTurn.NONE -> "Instant"
                    },
                    settings.pageTurn == t,
                ) { onSettings { it.copy(pageTurn = t) } }
            }
        }

        Section("LIGHT")
        val auto = settings.brightness < 0f
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.BrightnessLow, null, tint = p.muted, modifier = Modifier.size(20.dp))
            FolioSlider(
                value = if (auto) 0.6f else settings.brightness,
                onValueChange = { v -> onSettings { it.copy(brightness = v.coerceIn(0.03f, 1f)) } },
                valueRange = 0.03f..1f,
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
            )
            Pill("Auto", auto) { onSettings { it.copy(brightness = if (auto) 0.6f else -1f) } }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.WbTwilight, null, tint = p.muted, modifier = Modifier.size(20.dp))
            FolioSlider(
                value = settings.warmth,
                onValueChange = { v -> onSettings { it.copy(warmth = v) } },
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
            )
            Text("Warmth", style = MaterialTheme.typography.labelMedium, color = p.muted, modifier = Modifier.width(56.dp))
        }

        Section("FEEL")
        SwitchRow("Haptic tick on page turn", settings.haptics) { v -> onSettings { it.copy(haptics = v) } }
    }
}

@Composable
private fun sliderColors() = LocalPalette.current.let { p ->
    SliderDefaults.colors(thumbColor = p.accent, activeTrackColor = p.accent, inactiveTrackColor = p.hairline)
}

@Composable
private fun Section(label: String) {
    val p = LocalPalette.current
    Spacer(Modifier.height(22.dp))
    net.blazarforce.folio.ui.common.DashEyebrow(label)
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun RowScope.Tile(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    val border by animateColorAsState(if (selected) p.accent else p.hairline, label = "tile")
    val bg by animateColorAsState(if (selected) p.accentSoft else p.raised.copy(alpha = 0.35f), label = "tilebg")
    val src = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Column(
        Modifier.weight(1f).springPress(src, 0.94f).clip(RoundedCornerShape(16.dp)).background(bg)
            .border(if (selected) 1.dp else 0.7.dp, border, RoundedCornerShape(16.dp))
            .clickable(src, null, onClick = onClick).padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = if (selected) p.accent else p.muted, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = if (selected) p.text else p.muted)
    }
}

@Composable
private fun Pill(label: String, selected: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    val src = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val fg by animateColorAsState(if (selected) p.onAccent else p.muted, label = "pill")
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = fg,
        modifier = Modifier.springPress(src, 0.92f).clip(RoundedCornerShape(50))
            .then(if (selected) Modifier.background(p.goldBrush) else Modifier.background(Color.Transparent))
            .border(0.8.dp, if (selected) Color.Transparent else p.hairline, RoundedCornerShape(50))
            .clickable(src, null, onClick = onClick).padding(horizontal = 15.dp, vertical = 9.dp),
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = p.text, modifier = Modifier.weight(1f))
        Switch(
            checked, onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = p.canvas, checkedTrackColor = p.accent,
                uncheckedThumbColor = p.muted, uncheckedTrackColor = p.raised, uncheckedBorderColor = p.hairline,
            ),
        )
    }
}
