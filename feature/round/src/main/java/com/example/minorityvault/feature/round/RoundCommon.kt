package com.example.minorityvault.feature.round

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.minorityvault.designsystem.VaultSpacing
import com.example.minorityvault.designsystem.VaultTheme
import com.example.minorityvault.designsystem.component.PlayerChip
import com.example.minorityvault.designsystem.component.TimerBar
import com.example.minorityvault.designsystem.component.TimerText
import com.example.minorityvault.engine.PlayerId
import com.example.minorityvault.engine.RuleError

/** Every round screen: header, optional timer, scrolling content, and a fixed action bar. */
@Composable
internal fun RoundScaffold(
    header: RoundHeaderUi,
    modifier: Modifier = Modifier,
    title: String? = null,
    timer: TimerUi? = null,
    bottomBar: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = VaultTheme.colors
    Column(
        modifier
            .fillMaxSize()
            .background(colors.ink)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(VaultSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(VaultSpacing.md),
        ) {
            RoundHeader(header)
            if (timer != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TimerBar(timer.fraction, timer.secondsLeft, Modifier.weight(1f))
                    Spacer(Modifier.padding(start = VaultSpacing.md))
                    TimerText(timer.secondsLeft)
                }
            }
            if (title != null) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onPanel,
                    modifier = Modifier.semantics { heading() },
                )
            }
            content()
        }
        if (bottomBar != null) {
            Surface(color = colors.panel, contentColor = colors.onPanel) {
                Column(
                    Modifier.fillMaxWidth().padding(VaultSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(VaultSpacing.sm),
                    content = bottomBar,
                )
            }
        }
    }
}

@Composable
private fun RoundHeader(header: RoundHeaderUi) {
    val colors = VaultTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(VaultSpacing.xs)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                stringResource(R.string.round_of, header.round, header.totalRounds),
                style = MaterialTheme.typography.labelLarge,
                color = colors.onPanelMuted,
            )
            Text(
                stringResource(R.string.round_pot, header.pot),
                style = MaterialTheme.typography.titleMedium,
                color = colors.brass,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(VaultSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            header.twist?.let {
                Text(
                    twistTitle(it),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.brass,
                    modifier = Modifier
                        .border(1.dp, colors.brass, MaterialTheme.shapes.extraSmall)
                        .padding(horizontal = VaultSpacing.sm, vertical = VaultSpacing.xxs),
                )
            }
            Text(
                if (header.isAllIn) {
                    stringResource(R.string.round_fine_all_in)
                } else {
                    stringResource(R.string.round_fine, header.baseFine)
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (header.isAllIn) colors.stampRed else colors.onPanelMuted,
            )
        }
    }
}

@Composable
internal fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = VaultTheme.colors.onPanelMuted,
        modifier = modifier.padding(top = VaultSpacing.sm).semantics { heading() },
    )
}

@Composable
internal fun BodyText(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = VaultTheme.colors.onPanel, modifier = modifier)
}

/** Refusals from the engine, in plain words. Announced to screen readers. */
@Composable
internal fun ErrorBanner(error: RuleError?) {
    if (error == null) return
    val colors = VaultTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .border(1.dp, colors.stampRed, MaterialTheme.shapes.small)
            .padding(VaultSpacing.md)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Text(ruleErrorText(error), style = MaterialTheme.typography.bodyMedium, color = colors.stampRed)
    }
}

/** Players two to a row. [showLocks] adds VOTE lock dots. */
@Composable
internal fun PlayerGrid(players: List<PlayerUi>, highlight: PlayerId? = null, showLocks: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(VaultSpacing.sm)) {
        players.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(VaultSpacing.sm)) {
                row.forEach { player ->
                    PlayerChip(
                        name = player.name,
                        balance = player.balance,
                        brokenPromises = player.brokenPromises,
                        isDebtor = player.isDebtor,
                        hasLocked = if (showLocks) player.hasLocked else null,
                        highlighted = player.id == highlight,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
