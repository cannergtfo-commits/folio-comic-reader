package net.blazarforce.folio.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NoAccounts
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import net.blazarforce.folio.ui.common.DashEyebrow
import net.blazarforce.folio.ui.common.GhostButton
import net.blazarforce.folio.ui.common.GoldButton
import net.blazarforce.folio.ui.common.GoldRule
import net.blazarforce.folio.ui.common.OpenBookMark
import net.blazarforce.folio.ui.common.foilCard
import net.blazarforce.folio.ui.common.softGlow

/** Plain-English privacy summary shown in the About & Privacy dialog. Kept in sync with the hosted policy. */
val PRIVACY_POINTS: List<Pair<ImageVector, String>> = listOf(
    Icons.Outlined.NoAccounts to "No account. There is nothing to sign up for.",
    Icons.Outlined.Block to "No ads, no analytics, no trackers.",
    Icons.Outlined.PhoneAndroid to "Your library and reading progress live on your device, never on our servers.",
    Icons.Outlined.Lock to "The online comic catalog and chapters are fetched over HTTPS.",
)

/** Editorial About & Privacy dialog opened from the Library's info button. */
@Composable
fun AboutPrivacyDialog(versionName: String, onPrivacyPolicy: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        AboutPrivacyContent(versionName, onPrivacyPolicy, onDismiss, Modifier.padding(horizontal = 22.dp))
    }
}

@Composable
fun AboutPrivacyContent(
    versionName: String,
    onPrivacyPolicy: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = net.blazarforce.folio.ui.theme.LocalPalette.current
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .foilCard(shape)
            .background(p.surface, shape)
            .softGlow(p.glow, androidx.compose.ui.geometry.Offset(0.5f, 0f), 0.7f)
            .border(0.8.dp, p.goldEdge, shape)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 28.dp)
            .testTag("about-privacy"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(64.dp).background(p.accentSoft, CircleShape), contentAlignment = Alignment.Center) {
            OpenBookMark(Modifier.size(34.dp), p.accent, 1.4.dp)
        }
        Spacer(Modifier.height(16.dp))
        DashEyebrow("ABOUT & PRIVACY")
        Spacer(Modifier.height(10.dp))
        Text("Blazar Reader", style = MaterialTheme.typography.displaySmall, color = p.text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            "Version $versionName",
            style = MaterialTheme.typography.titleMedium.copy(fontStyle = FontStyle.Italic),
            color = p.muted,
        )
        Spacer(Modifier.height(18.dp))
        GoldRule()
        Spacer(Modifier.height(18.dp))
        Text(
            "An editorial comic reader. Your shelf is yours alone.",
            style = MaterialTheme.typography.bodyMedium, color = p.text, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PRIVACY_POINTS.forEach { (icon, text) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = p.accent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(text, style = MaterialTheme.typography.bodyMedium, color = p.muted)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        GoldButton(
            "Privacy Policy", onPrivacyPolicy, Modifier.fillMaxWidth(),
            icon = Icons.AutoMirrored.Outlined.OpenInNew, height = 48.dp, glint = false,
        )
        Spacer(Modifier.height(10.dp))
        GhostButton("Close", onClose, Modifier.fillMaxWidth(), height = 46.dp)
    }
}
