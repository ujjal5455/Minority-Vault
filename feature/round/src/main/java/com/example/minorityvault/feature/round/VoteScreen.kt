package com.example.minorityvault.feature.round

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.example.minorityvault.designsystem.MinorityVaultTheme
import com.example.minorityvault.designsystem.VaultSpacing
import com.example.minorityvault.designsystem.VaultTheme
import com.example.minorityvault.designsystem.component.Lever
import com.example.minorityvault.designsystem.component.PaperCard
import com.example.minorityvault.designsystem.component.VaultButton
import com.example.minorityvault.designsystem.component.VaultButtonStyle
import com.example.minorityvault.designsystem.preview.ScreenPreviews
import com.example.minorityvault.engine.Ballot
import com.example.minorityvault.engine.PlayerId
import com.example.minorityvault.engine.Side
import com.example.minorityvault.engine.VoteTotals

/** The secret vote. [onFinishTurn] is set only in pass-the-phone play. */
@Composable
fun VoteScreen(
    state: VoteUiState,
    onAction: (RoundAction) -> Unit,
    onFinishTurn: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    RoundScaffold(
        header = state.header,
        title = stringResource(R.string.vote_title, state.players.nameOf(state.me)),
        timer = state.timer,
        modifier = modifier,
        bottomBar = onFinishTurn?.let {
            {
                if (state.ballot?.locked == false) {
                    Text(
                        stringResource(R.string.vote_unlocked_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = VaultTheme.colors.onPanelMuted,
                    )
                }
                VaultButton(stringResource(R.string.done_pass_phone), onClick = it, modifier = Modifier.fillMaxWidth())
            }
        },
    ) {
        PaperCard(Modifier.fillMaxWidth()) {
            Text(stringResource(state.header.questionRes), style = MaterialTheme.typography.headlineSmall)
        }
        ErrorBanner(state.error)
        state.insiderTotals?.let { totals ->
            Text(
                stringResource(R.string.vote_insider, totals.yes, totals.no),
                style = MaterialTheme.typography.titleMedium,
                color = VaultTheme.colors.brass,
            )
        }
        if (state.ballot == null) {
            BodyText(stringResource(R.string.vote_sold, state.soldTo?.let(state.players::nameOf).orEmpty()))
        } else {
            BallotControls(state.ballot, forPlayer = null, onAction = onAction)
            if (!state.ballot.locked && (state.doubleVoteAvailable || state.doubleArmed)) {
                DoubleVoteToggle(state.doubleArmed) { onAction(RoundAction.ArmDoubleVote(it)) }
            }
            if (state.ballot.doubled) {
                Text(
                    stringResource(R.string.vote_double_used),
                    style = MaterialTheme.typography.titleMedium,
                    color = VaultTheme.colors.brass,
                )
            }
        }
        state.controlled.forEach { (debtor, ballot) ->
            SectionTitle(stringResource(R.string.vote_bought, state.players.nameOf(debtor)))
            BallotControls(ballot, forPlayer = debtor, onAction = onAction, debtorName = state.players.nameOf(debtor))
        }
        SectionTitle(stringResource(R.string.vote_lock_status))
        PlayerGrid(state.players, highlight = state.me, showLocks = true)
    }
}

@Composable
private fun BallotControls(
    ballot: Ballot,
    forPlayer: PlayerId?,
    onAction: (RoundAction) -> Unit,
    debtorName: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(VaultSpacing.sm)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Side.entries.forEach { side ->
                Lever(
                    label = sideLabel(side),
                    pulled = ballot.selection == side,
                    enabled = !ballot.locked,
                    onPull = { onAction(RoundAction.SelectVote(side, forPlayer)) },
                )
            }
        }
        if (ballot.locked) {
            BodyText(stringResource(R.string.vote_locked))
            VaultButton(
                stringResource(R.string.vote_unlock),
                onClick = { onAction(RoundAction.Unlock(forPlayer)) },
                style = VaultButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            VaultButton(
                if (debtorName == null) {
                    stringResource(R.string.vote_lock)
                } else {
                    stringResource(R.string.vote_lock_bought, debtorName)
                },
                onClick = { onAction(RoundAction.Lock(forPlayer)) },
                enabled = ballot.selection != null,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DoubleVoteToggle(armed: Boolean, onToggle: (Boolean) -> Unit) {
    val colors = VaultTheme.colors
    Surface(color = colors.panel, contentColor = colors.onPanel, shape = MaterialTheme.shapes.medium) {
        Row(
            Modifier
                .fillMaxWidth()
                .toggleable(value = armed, role = Role.Switch, onValueChange = onToggle)
                .padding(VaultSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.vote_double), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.vote_double_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onPanelMuted,
                )
            }
            Switch(
                checked = armed,
                onCheckedChange = null,
                modifier = Modifier.padding(start = VaultSpacing.sm),
                colors = SwitchDefaults.colors(
                    checkedTrackColor = colors.brass,
                    checkedThumbColor = colors.ink,
                    uncheckedTrackColor = colors.ink,
                    uncheckedThumbColor = colors.steel,
                    uncheckedBorderColor = colors.steel,
                ),
            )
        }
    }
}

@ScreenPreviews
@Composable
private fun VoteScreenPreview() {
    MinorityVaultTheme(reducedMotion = true) {
        VoteScreen(
            VoteUiState(
                header = PreviewData.header,
                me = PreviewData.asha,
                players = PreviewData.players,
                ballot = Ballot(selection = Side.NO),
                soldTo = null,
                doubleVoteAvailable = true,
                doubleArmed = true,
                controlled = mapOf(PreviewData.chandrika to Ballot()),
                insiderTotals = VoteTotals(yes = 1, no = 2),
            ),
            onAction = {},
            onFinishTurn = {},
        )
    }
}
