package com.example.minorityvault.feature.results

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.minorityvault.designsystem.CountingText
import com.example.minorityvault.designsystem.GlassSurface
import com.example.minorityvault.designsystem.VaultPreviewTheme
import com.example.minorityvault.designsystem.VaultSpacing
import com.example.minorityvault.designsystem.VaultTheme
import com.example.minorityvault.designsystem.component.KeeperBubble
import com.example.minorityvault.designsystem.component.VaultButton
import com.example.minorityvault.designsystem.component.VaultButtonStyle
import com.example.minorityvault.designsystem.glassRim
import com.example.minorityvault.designsystem.preview.ScreenPreviews
import com.example.minorityvault.designsystem.sheen
import com.example.minorityvault.engine.PlayerId
import com.example.minorityvault.engine.PublicGameView
import kotlinx.coroutines.delay
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
    // Rows arrive from last place up, so the winner lands last.
    val reducedMotion = VaultTheme.reducedMotion
    var arrived by remember { mutableIntStateOf(if (reducedMotion) state.ranking.size else 0) }
    LaunchedEffect(state) {
        while (arrived < state.ranking.size) {
            delay(ROW_ARRIVAL_MILLIS)
            arrived++
        }
    }
    Column(
        modifier
            .fillMaxSize()
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
                color = colors.brass,
                modifier = Modifier.semantics { heading() }.sheen(),
            )
            KeeperBubble(verdict(state))
            state.ranking.forEachIndexed { index, player ->
                AnimatedVisibility(
                    visible = state.ranking.size - index <= arrived,
                    enter = fadeIn() + slideInVertically { it / 2 },
                ) {
                    RankingRow(player)
                }
            }
        }
        GlassSurface(Modifier.fillMaxWidth(), shape = BottomBarShape) {
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
    GlassSurface(
        rim = if (player.isWinner) glassRim(colors.brass, WINNER_RIM_STRENGTH) else glassRim(),
        modifier = Modifier.sheen(enabled = player.isWinner),
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
            CountingText(
                value = player.balance,
                format = { pluralStringResource(DesignR.plurals.coins, it, it) },
                style = MaterialTheme.typography.headlineSmall,
                color = colors.brass,
                countFromZero = true,
            )
        }
    }
}

private val RANK_WIDTH = 32.dp
private val BottomBarShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
private const val ROW_ARRIVAL_MILLIS = 450L
private const val WINNER_RIM_STRENGTH = 2f

@ScreenPreviews
@Composable
private fun GameOverScreenPreview() {
    VaultPreviewTheme {
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
