package com.example.minorityvault.feature.results

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.minorityvault.designsystem.MinorityVaultTheme
import com.example.minorityvault.designsystem.VaultSpacing
import com.example.minorityvault.designsystem.VaultTheme
import com.example.minorityvault.designsystem.component.KeeperBubble
import com.example.minorityvault.designsystem.component.VaultButton
import com.example.minorityvault.designsystem.component.VaultButtonStyle
import com.example.minorityvault.designsystem.preview.ScreenPreviews
import com.example.minorityvault.engine.PlayerId
import com.example.minorityvault.engine.PublicGameView
import com.example.minorityvault.designsystem.R as DesignR

data class RankedPlayer(
    val rank: Int,
    val id: PlayerId,
    val name: String,
    val balance: Int,
    val isDebtor: Boolean,
    val brokenPromises: Int,
    val isWinner: Boolean,
)

data class GameOverUiState(val ranking: List<RankedPlayer>) {
    val winners: List<RankedPlayer> get() = ranking.filter { it.isWinner }
}

/** Richest first; players on the same balance share a rank (1, 1, 3). */
fun gameOverUiState(view: PublicGameView): GameOverUiState {
    val winners = view.winners.orEmpty().toSet()
    val sorted = view.players.sortedByDescending { it.balance }
    return GameOverUiState(
        sorted.map { player ->
            RankedPlayer(
                rank = 1 + sorted.count { it.balance > player.balance },
                id = player.id,
                name = player.name,
                balance = player.balance,
                isDebtor = player.isDebtor,
                brokenPromises = player.brokenPromises,
                isWinner = player.id in winners,
            )
        },
    )
}

@Composable
fun GameOverScreen(
    state: GameOverUiState,
    onPlayAgain: () -> Unit,
    onNewPlayers: () -> Unit,
    modifier: Modifier = Modifier,
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
            Text(
                stringResource(R.string.game_over_title),
                style = MaterialTheme.typography.displaySmall,
                color = colors.onPanel,
                modifier = Modifier.semantics { heading() },
            )
            KeeperBubble(verdict(state))
            state.ranking.forEach { RankingRow(it) }
        }
        Surface(color = colors.panel) {
            Column(
                Modifier.fillMaxWidth().padding(VaultSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(VaultSpacing.sm),
            ) {
                VaultButton(stringResource(R.string.game_over_play_again), onPlayAgain, Modifier.fillMaxWidth())
                VaultButton(
                    stringResource(R.string.game_over_new_players),
                    onNewPlayers,
                    Modifier.fillMaxWidth(),
                    style = VaultButtonStyle.Secondary,
                )
            }
        }
    }
}

@Composable
private fun verdict(state: GameOverUiState): String {
    val names = state.winners.joinToString(", ") { it.name }
    return when (state.winners.size) {
        0 -> stringResource(R.string.game_over_nobody)
        1 -> stringResource(R.string.game_over_single_winner, names)
        else -> stringResource(R.string.game_over_shared_winners, names)
    }
}

@Composable
private fun RankingRow(player: RankedPlayer) {
    val colors = VaultTheme.colors
    Surface(
        color = colors.panel,
        contentColor = colors.onPanel,
        shape = MaterialTheme.shapes.medium,
        border = if (player.isWinner) BorderStroke(2.dp, colors.brass) else null,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(VaultSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(VaultSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.game_over_rank, player.rank),
                style = MaterialTheme.typography.displaySmall,
                color = if (player.isWinner) colors.brass else colors.onPanelMuted,
                modifier = Modifier.widthIn(min = RANK_WIDTH),
            )
            Column(Modifier.weight(1f)) {
                Text(player.name, style = MaterialTheme.typography.titleLarge)
                val notes = listOfNotNull(
                    if (player.isWinner) stringResource(R.string.game_over_winner) else null,
                    if (player.isDebtor) stringResource(R.string.game_over_debtor) else null,
                    if (player.brokenPromises > 0) {
                        pluralStringResource(
                            DesignR.plurals.broken_promises,
                            player.brokenPromises,
                            player.brokenPromises,
                        )
                    } else {
                        null
                    },
                )
                if (notes.isNotEmpty()) {
                    Text(
                        notes.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (player.isDebtor || player.brokenPromises > 0) colors.stampRed else colors.brass,
                    )
                }
            }
            Text(
                pluralStringResource(DesignR.plurals.coins, player.balance, player.balance),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.brass,
            )
        }
    }
}

private val RANK_WIDTH = 32.dp

@ScreenPreviews
@Composable
private fun GameOverScreenPreview() {
    MinorityVaultTheme {
        GameOverScreen(
            GameOverUiState(
                listOf(
                    RankedPlayer(1, PlayerId("p1"), "Ezra", 212, isDebtor = false, brokenPromises = 0, isWinner = true),
                    RankedPlayer(
                        2,
                        PlayerId("p2"),
                        "Asha",
                        140,
                        isDebtor = false,
                        brokenPromises = 1,
                        isWinner = false,
                    ),
                    RankedPlayer(3, PlayerId("p3"), "Ben", 98, isDebtor = false, brokenPromises = 4, isWinner = false),
                    RankedPlayer(
                        4,
                        PlayerId("p4"),
                        "Chandrika",
                        0,
                        isDebtor = true,
                        brokenPromises = 0,
                        isWinner = false,
                    ),
                ),
            ),
            onPlayAgain = {},
            onNewPlayers = {},
        )
    }
}
