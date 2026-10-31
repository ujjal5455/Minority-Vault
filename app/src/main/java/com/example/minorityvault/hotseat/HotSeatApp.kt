package com.example.minorityvault.hotseat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.minorityvault.R
import com.example.minorityvault.designsystem.VaultTheme
import com.example.minorityvault.feature.results.GameOverScreen
import com.example.minorityvault.feature.round.FlipScreen
import com.example.minorityvault.feature.round.NegotiationScreen
import com.example.minorityvault.feature.round.QuestionScreen
import com.example.minorityvault.feature.round.RevealScreen
import com.example.minorityvault.feature.round.RoundAction
import com.example.minorityvault.feature.round.VoteScreen
import com.example.minorityvault.heavyPanelSlide
import com.example.minorityvault.instant
import com.example.minorityvault.quickFade

@Composable
fun HotSeatRoute(onExit: () -> Unit, viewModel: HotSeatViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HotSeatApp(state, viewModel::onEvent, onExit)
}

/** Picks the screen for the current phase and turn. Back from setup leaves; back mid-game asks first. */
@Composable
fun HotSeatApp(
    state: HotSeatUiState,
    onEvent: (HotSeatEvent) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmQuit by rememberSaveable { mutableStateOf(false) }
    BackHandler { if (state is HotSeatUiState.Setup) onExit() else confirmQuit = true }
    if (confirmQuit) {
        QuitDialog(
            onQuit = {
                confirmQuit = false
                onEvent(HotSeatEvent.NewPlayers)
            },
            onDismiss = { confirmQuit = false },
        )
    }
    val reducedMotion = VaultTheme.reducedMotion
    AnimatedContent(
        targetState = state,
        modifier = modifier,
        contentKey = { it::class },
        transitionSpec = {
            when {
                reducedMotion -> instant()

                // The vault door does its own closing animation.
                targetState is HotSeatUiState.PassPhone -> quickFade()

                else -> heavyPanelSlide()
            }
        },
        label = "hot seat screen",
    ) { screen -> HotSeatScreen(screen, onEvent) }
}

@Composable
private fun HotSeatScreen(screen: HotSeatUiState, onEvent: (HotSeatEvent) -> Unit) {
    val onAction = { action: RoundAction -> onEvent(HotSeatEvent.Round(action)) }
    val onFinishTurn = { onEvent(HotSeatEvent.FinishTurn) }
    val onContinue = { onEvent(HotSeatEvent.Continue) }
    when (screen) {
        is HotSeatUiState.Setup -> HotSeatSetupScreen(screen, onEvent)

        is HotSeatUiState.PassPhone -> PassPhoneScreen(screen, onShow = { onEvent(HotSeatEvent.ShowTurn) })

        is HotSeatUiState.NegotiationPassDone -> NegotiationPassDoneScreen(screen, onEvent)

        is HotSeatUiState.Question -> QuestionScreen(screen.state, onContinue)

        is HotSeatUiState.Negotiation -> NegotiationScreen(screen.state, onAction, onFinishTurn)

        is HotSeatUiState.Vote -> VoteScreen(screen.state, onAction, onFinishTurn)

        is HotSeatUiState.Flip -> FlipScreen(screen.state, onAction, onFinishTurn)

        is HotSeatUiState.Reveal -> RevealScreen(screen.state, onContinue)

        is HotSeatUiState.GameOver -> GameOverScreen(
            screen.state,
            onPlayAgain = { onEvent(HotSeatEvent.PlayAgain) },
            onNewPlayers = { onEvent(HotSeatEvent.NewPlayers) },
        )
    }
}

@Composable
private fun QuitDialog(onQuit: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.quit_title)) },
        text = { Text(stringResource(R.string.quit_body)) },
        confirmButton = { TextButton(onClick = onQuit) { Text(stringResource(R.string.quit_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.quit_cancel)) } },
    )
}
