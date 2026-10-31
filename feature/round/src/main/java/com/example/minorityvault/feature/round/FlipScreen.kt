package com.example.minorityvault.feature.round

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.minorityvault.designsystem.VaultPreviewTheme
import com.example.minorityvault.designsystem.VaultTheme
import com.example.minorityvault.designsystem.component.PaperCard
import com.example.minorityvault.designsystem.component.VaultButton
import com.example.minorityvault.designsystem.component.VaultButtonStyle
import com.example.minorityvault.designsystem.preview.ScreenPreviews
import com.example.minorityvault.engine.PlayerId
import com.example.minorityvault.engine.Vote
import com.example.minorityvault.engine.VoteChoice
import com.example.minorityvault.engine.VoteTotals

/** SecondChance: totals only, and one flip per vote. [onFinishTurn] is set only in pass-the-phone play. */
@Composable
fun FlipScreen(
    state: FlipUiState,
    onAction: (RoundAction) -> Unit,
    onFinishTurn: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    RoundScaffold(
        header = state.header,
        title = stringResource(R.string.flip_title, state.players.nameOf(state.me)),
        timer = state.timer,
        modifier = modifier,
        bottomBar = onFinishTurn?.let {
            { VaultButton(stringResource(R.string.done_pass_phone), onClick = it, modifier = Modifier.fillMaxWidth()) }
        },
    ) {
        PaperCard(Modifier.fillMaxWidth()) {
            Text(stringResource(state.header.questionRes), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.flip_totals, state.totals.yes, state.totals.no),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        ErrorBanner(state.error)
        when {
            state.soldTo != null ->
                BodyText(stringResource(R.string.vote_sold, state.players.nameOf(state.soldTo)))

            state.myVote != null -> FlipControls(state.myVote, forPlayer = null, label = null, onAction = onAction)
        }
        state.controlled.forEach { (debtor, vote) ->
            SectionTitle(stringResource(R.string.flip_bought, state.players.nameOf(debtor), voteLabel(vote)))
            FlipControls(vote, forPlayer = debtor, label = state.players.nameOf(debtor), onAction = onAction)
        }
    }
}

@Composable
private fun FlipControls(vote: Vote, forPlayer: PlayerId?, label: String?, onAction: (RoundAction) -> Unit) {
    when {
        vote.choice == VoteChoice.ABSTAIN -> BodyText(stringResource(R.string.flip_abstained))

        vote.flipped -> BodyText(stringResource(R.string.flip_already))

        else -> {
            if (label == null) {
                Text(
                    stringResource(R.string.flip_your_vote, voteLabel(vote)),
                    style = MaterialTheme.typography.titleLarge,
                    color = VaultTheme.colors.onPanel,
                )
            }
            val other = voteLabel(vote.copy(choice = vote.choice.opposite, doubled = false))
            VaultButton(
                stringResource(R.string.flip_to, other),
                onClick = { onAction(RoundAction.Flip(forPlayer)) },
                style = VaultButtonStyle.Danger,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@ScreenPreviews
@Composable
private fun FlipScreenPreview() {
    VaultPreviewTheme {
        FlipScreen(
            FlipUiState(
                header = PreviewData.header,
                me = PreviewData.asha,
                players = PreviewData.players,
                totals = VoteTotals(yes = 2, no = 2),
                myVote = Vote(VoteChoice.YES),
                soldTo = null,
                controlled = emptyMap(),
            ),
            onAction = {},
            onFinishTurn = {},
        )
    }
}
