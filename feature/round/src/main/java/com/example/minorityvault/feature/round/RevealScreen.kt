package com.example.minorityvault.feature.round

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.minorityvault.designsystem.MinorityVaultTheme
import com.example.minorityvault.designsystem.VaultSpacing
import com.example.minorityvault.designsystem.VaultTheme
import com.example.minorityvault.designsystem.component.Stamp
import com.example.minorityvault.designsystem.component.VaultButton
import com.example.minorityvault.designsystem.component.VaultDial
import com.example.minorityvault.designsystem.preview.ScreenPreviews
import com.example.minorityvault.engine.Side
import com.example.minorityvault.engine.Vote
import com.example.minorityvault.engine.VoteChoice
import com.example.minorityvault.engine.VoteTotals

/** The public reveal and settlement. [onContinue] is null when the host moves everyone on. */
@Composable
fun RevealScreen(state: RevealUiState, onContinue: (() -> Unit)?, modifier: Modifier = Modifier) {
    val colors = VaultTheme.colors
    RoundScaffold(
        header = state.header,
        title = stringResource(R.string.reveal_title),
        modifier = modifier,
        bottomBar = onContinue?.let {
            { VaultButton(stringResource(R.string.continue_label), onClick = it, modifier = Modifier.fillMaxWidth()) }
        },
    ) {
        VaultDial(
            yes = state.totals.yes,
            no = state.totals.no,
            yesLabel = stringResource(R.string.side_yes),
            noLabel = stringResource(R.string.side_no),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            state.minoritySide?.let { stringResource(R.string.reveal_minority, sideLabel(it)) }
                ?: stringResource(R.string.reveal_no_minority),
            style = MaterialTheme.typography.headlineMedium,
            color = colors.brass,
        )
        PayoutLine(state)
        if (state.potLeft > 0) BodyText(stringResource(R.string.reveal_rollover, state.potLeft))

        SectionTitle(stringResource(R.string.reveal_votes))
        VoteList(state)
        if (state.fines.values.any { it > 0 }) {
            SectionTitle(stringResource(R.string.reveal_fines))
            state.fines.filterValues { it > 0 }.forEach { (id, amount) ->
                BodyText(stringResource(R.string.reveal_fine_line, state.players.nameOf(id), amount))
            }
        }
        state.newDebtors.forEach {
            Text(
                stringResource(R.string.reveal_new_debtor, state.players.nameOf(it)),
                style = MaterialTheme.typography.titleMedium,
                color = colors.stampRed,
            )
        }
        state.recoveredDebtors.forEach {
            BodyText(stringResource(R.string.reveal_recovered, state.players.nameOf(it)))
        }
        SectionTitle(stringResource(R.string.reveal_balances))
        PlayerGrid(state.players)
    }
}

/** Who voted what. Minority votes in brass; promise breakers get the stamp. */
@Composable
private fun VoteList(state: RevealUiState) {
    val colors = VaultTheme.colors
    state.players.forEach { player ->
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BodyText(player.name, Modifier.weight(1f))
            if (player.id in state.promiseBreakers) {
                // The stamp is drawn tilted, so leave room for its corners.
                Stamp(stringResource(R.string.reveal_broken), Modifier.padding(horizontal = VaultSpacing.lg))
            }
            Text(
                voteLabel(state.votes[player.id] ?: Vote.ABSTAIN),
                style = MaterialTheme.typography.headlineSmall,
                color = if (player.id in state.minorityPlayers) colors.brass else colors.onPanelMuted,
            )
        }
    }
}

@Composable
private fun PayoutLine(state: RevealUiState) {
    val names = state.winners.joinToString(", ") { state.players.nameOf(it) }
    val text = when {
        state.winners.size == 1 -> stringResource(R.string.reveal_winner_single, names, state.share)
        state.winners.isNotEmpty() -> stringResource(R.string.reveal_winners, names, state.share)
        state.minoritySide != null -> stringResource(R.string.reveal_no_winners_twist)
        else -> null
    }
    if (text != null) {
        Text(text, style = MaterialTheme.typography.titleLarge, color = VaultTheme.colors.onPanel)
    }
}

@ScreenPreviews
@Composable
private fun RevealScreenPreview() {
    val p = PreviewData
    MinorityVaultTheme(reducedMotion = true) {
        RevealScreen(
            RevealUiState(
                header = p.header,
                players = p.players,
                votes = mapOf(
                    p.asha to Vote(VoteChoice.YES),
                    p.ben to Vote(VoteChoice.NO),
                    p.chandrika to Vote(VoteChoice.YES),
                    p.dev to Vote(VoteChoice.NO, doubled = true),
                    p.ezra to Vote.ABSTAIN,
                ),
                totals = VoteTotals(yes = 2, no = 3),
                minoritySide = Side.YES,
                minorityPlayers = listOf(p.asha, p.chandrika),
                winners = listOf(p.asha, p.chandrika),
                share = 22,
                potLeft = 1,
                fines = mapOf(p.ben to 10, p.dev to 10, p.ezra to 10),
                promiseBreakers = listOf(p.ben),
                newDebtors = emptyList(),
                recoveredDebtors = listOf(p.chandrika),
            ),
            onContinue = {},
        )
    }
}
