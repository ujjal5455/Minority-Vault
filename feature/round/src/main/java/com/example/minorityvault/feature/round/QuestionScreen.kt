package com.example.minorityvault.feature.round

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.minorityvault.designsystem.VaultPreviewTheme
import com.example.minorityvault.designsystem.component.KeeperBubble
import com.example.minorityvault.designsystem.component.PaperCard
import com.example.minorityvault.designsystem.component.VaultButton
import com.example.minorityvault.designsystem.preview.ScreenPreviews

/** The question and the round's twist, for everyone. [onContinue] is null when the host's timer moves on. */
@Composable
fun QuestionScreen(state: QuestionUiState, onContinue: (() -> Unit)?, modifier: Modifier = Modifier) {
    RoundScaffold(
        header = state.header,
        timer = state.timer,
        modifier = modifier,
        bottomBar = onContinue?.let {
            { VaultButton(stringResource(R.string.continue_label), onClick = it, modifier = Modifier.fillMaxWidth()) }
        },
    ) {
        PaperCard(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.question_title), style = MaterialTheme.typography.labelLarge)
            Text(stringResource(state.header.questionRes), style = MaterialTheme.typography.headlineLarge)
        }
        // Until the Keeper has picked, say nothing rather than guess.
        state.header.twist?.let { KeeperBubble(twistKeeperLine(it)) }
        SectionTitle(stringResource(R.string.lobby_players))
        PlayerGrid(state.players)
    }
}

@ScreenPreviews
@Composable
private fun QuestionScreenPreview() {
    VaultPreviewTheme {
        QuestionScreen(QuestionUiState(PreviewData.header, PreviewData.players), onContinue = {})
    }
}
