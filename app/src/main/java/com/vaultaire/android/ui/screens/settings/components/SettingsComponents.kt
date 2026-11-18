package com.vaultaire.android.ui.screens.settings.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vaultaire.android.designsystem.AuroraBackground
import com.vaultaire.android.designsystem.AuroraChip
import com.vaultaire.android.designsystem.AuroraDimens
import com.vaultaire.android.designsystem.AuroraGlassCard
import com.vaultaire.android.designsystem.AuroraIconButton
import com.vaultaire.android.designsystem.AuroraShapes
import com.vaultaire.android.designsystem.AuroraSwitch
import com.vaultaire.android.designsystem.AuroraTheme
import com.vaultaire.android.designsystem.AuroraTopBar
import com.vaultaire.android.designsystem.AuroraType

/**
 * Shared settings surfaces.
 *
 * Rows are 64 dp with 20 dp horizontal page padding, values render in mono,
 * and every tappable row exposes a Button role for accessibility.
 */

@Composable
fun SettingsPage(
    title: String,
    onBack: (() -> Unit)?,
    eyebrow: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = AuroraTheme.colors
    AuroraBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            AuroraTopBar(
                title = title,
                eyebrow = eyebrow,
                leading = onBack?.let {
                    {
                        AuroraIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            onClick = it,
                        )
                    }
                },
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = AuroraDimens.screenPadding)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(AuroraDimens.cardGap),
            ) {
                content()
                Spacer(Modifier.height(16.dp))
                Text(
                    "All settings stay on this device. Nothing here is synced or analyzed.",
                    style = AuroraType.bodyS,
                    color = c.ink400,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = AuroraTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (title != null) {
            Text(
                title.uppercase(),
                style = AuroraType.overline,
                color = c.ink500,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        AuroraGlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(vertical = 4.dp), content = content)
        }
    }
}

@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    badge: String? = null,
    icon: ImageVector? = null,
    tint: Color? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = AuroraTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.semantics { role = Role.Button } else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(AuroraShapes.sm)
                    .background(tint ?: c.ink050),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = c.ink700, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = AuroraType.bodyM, color = c.ink900)
                if (badge != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        badge,
                        style = AuroraType.monoS,
                        color = c.ink700,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(c.ink050)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            if (subtitle != null) {
                Text(subtitle, style = AuroraType.bodyS, color = c.ink500)
            }
        }
        if (value != null) {
            Text(value, style = AuroraType.monoM, color = c.ink500)
            Spacer(Modifier.width(6.dp))
        }
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = c.ink300,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    SettingsRow(
        title = title,
        subtitle = subtitle,
        onClick = if (enabled) ({ onCheckedChange(!checked) }) else null,
        trailing = { AuroraToggle(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange) },
    )
}

/**
 * Aurora toggle switch. Delegates to [AuroraSwitch] for centralized styling and 48dp touch target compliance.
 */
@Composable
fun AuroraToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    AuroraSwitch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
    )
}

@Composable
fun SegmentedOptions(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEachIndexed { index, label ->
            AuroraChip(
                label = label,
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun HonestyCard(text: String, modifier: Modifier = Modifier) {
    val c = AuroraTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AuroraShapes.md)
            .background(c.tintButter)
            .border(1.dp, c.tintButterDeep.copy(alpha = 0.35f), AuroraShapes.md)
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            Icons.Filled.Info,
            contentDescription = null,
            tint = c.ink700,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(text, style = AuroraType.bodyM, color = c.ink800)
    }
}

@Composable
fun InfoCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    val c = AuroraTheme.colors
    AuroraGlassCard(modifier.fillMaxWidth(), tint = com.vaultaire.android.designsystem.AuroraTint.None) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = AuroraType.titleM, color = c.ink900)
            Text(body, style = AuroraType.bodyM, color = c.ink600)
        }
    }
}

@Composable
fun SettingsValueRow(label: String, value: String) {
    val c = AuroraTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = AuroraType.bodyM, color = c.ink700, modifier = Modifier.weight(1f))
        Text(value, style = AuroraType.monoM, color = c.ink500)
    }
}

@Composable
fun SettingsPageHeader(title: String, body: String) {
    val c = AuroraTheme.colors
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp)) {
        Text(title, style = AuroraType.headlineL, color = c.ink900)
        Spacer(Modifier.height(4.dp))
        Text(body, style = AuroraType.bodyM, color = c.ink500)
    }
}

/**
 * Version row with the 3-taps-in-1s diagnostics gesture from screens.md §10.
 * The toast carries build, vault size and sync state only.
 */
@Composable
fun DiagnosticsVersionRow() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var taps by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var lastTap by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0L) }
    SettingsRow(
        title = "Version",
        value = "1.0.0 (1)",
        onClick = {
            val now = System.currentTimeMillis()
            taps = if (now - lastTap <= 1000) taps + 1 else 1
            lastTap = now
            if (taps >= 3) {
                taps = 0
                android.widget.Toast.makeText(
                    context,
                    "build=1.0.0-debug - vault=2.1 GB - sync=idle",
                    android.widget.Toast.LENGTH_LONG,
                ).show()
            }
        },
    )
}
