package com.vaultaire.android.ui.screens.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaultaire.android.designsystem.AuroraBackground
import com.vaultaire.android.designsystem.AuroraButton
import com.vaultaire.android.designsystem.AuroraButtonVariant
import com.vaultaire.android.designsystem.AuroraGlassCard
import com.vaultaire.android.designsystem.AuroraIconButton
import com.vaultaire.android.designsystem.AuroraTheme
import com.vaultaire.android.designsystem.AuroraTopBar
import com.vaultaire.android.designsystem.AuroraType

/**
 * Temporary scaffold used until a flow screen is implemented against its spec.
 *
 * It deliberately renders the route id so QA can confirm every canonical route
 * resolves, and it offers the screen's primary path so flows stay walkable.
 */
@Composable
fun SpecPlaceholderScreen(
    title: String,
    route: String,
    purpose: String,
    onBack: (() -> Unit)? = null,
    primaryLabel: String? = null,
    onPrimary: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    body: @Composable ColumnScope.() -> Unit = {},
) {
    val c = AuroraTheme.colors
    AuroraBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            AuroraTopBar(
                title = title,
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
                    .padding(horizontal = AuroraTheme.dimens.screenPadding),
                verticalArrangement = Arrangement.spacedBy(AuroraTheme.dimens.cardGap),
            ) {
                Spacer(Modifier.height(8.dp))
                Text(route, style = AuroraType.monoM, color = c.ink500)
                Text(title, style = AuroraType.headlineL, color = c.ink900)
                Text(purpose, style = AuroraType.bodyM, color = c.ink600)
                AuroraGlassCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(AuroraTheme.dimens.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Screen not implemented yet", style = AuroraType.titleM, color = c.ink900)
                        Text(
                            "This destination resolves but still uses the flow scaffold. " +
                                "Implement it against plan/design-system/flows and plan/tests/screens.",
                            style = AuroraType.bodyS,
                            color = c.ink500,
                        )
                    }
                }
                body()
                Spacer(Modifier.height(8.dp))
                if (primaryLabel != null && onPrimary != null) {
                    AuroraButton(
                        text = primaryLabel,
                        onClick = onPrimary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (secondaryLabel != null && onSecondary != null) {
                    AuroraButton(
                        text = secondaryLabel,
                        onClick = onSecondary,
                        variant = AuroraButtonVariant.Ghost,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
