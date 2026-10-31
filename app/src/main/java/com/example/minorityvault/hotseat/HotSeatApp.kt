package com.example.minorityvault.hotseat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import com.example.minorityvault.designsystem.VaultBackdrop
import com.example.minorityvault.designsystem.VaultTheme
import com.example.minorityvault.feature.results.GameOverScreen
import com.example.minorityvault.feature.round.FlipScreen
import com.example.minorityvault.feature.round.NegotiationScreen
import com.example.minorityvault.feature.round.QuestionScreen
import com.example.minorityvault.feature.round.RevealScreen
import com.example.minorityvault.feature.round.RoundAction
import com.example.minorityvault.feature.round.VoteScreen

@Composable
fun HotSeatRoute(viewModel: HotSeatViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HotSeatApp(state, viewModel::onEvent)
}

/** Picks the screen for the current phase and turn. */
@Composable
fun HotSeatApp(state: HotSeatUiState, onEvent: (HotSeatEvent) -> Unit, modifier: Modifier = Modifier) {
    var confirmQuit by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = state !is HotSeatUiState.Setup) { confirmQuit = true }
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
    VaultBackdrop(modifier) {
        AnimatedContent(
            targetState = state,
            contentKey = { it::class },
            transitionSpec = {
                when {
                    reducedMotion -> fadeIn(snap()) togetherWith fadeOut(snap())

                    // The vault door does its own closing animation.
                    targetState is HotSeatUiState.PassPhone -> quickFade()

                    else -> heavyPanelSlide()
                }
            },
            label = "hot seat screen",
        ) { screen -> HotSeatScreen(screen, onEvent) }
    }
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

private fun quickFade(): ContentTransform =
    fadeIn(tween(QUICK_FADE_MILLIS)) togetherWith fadeOut(tween(QUICK_FADE_MILLIS))

/** The new screen rises into place like a heavy panel; the old one lifts away. */
private fun heavyPanelSlide(): ContentTransform {
    val rise = slideInVertically(tween(PANEL_MILLIS, easing = HeavyEasing)) { it / PANEL_RISE } + fadeIn()
    val lift = slideOutVertically(tween(PANEL_MILLIS, easing = HeavyEasing)) { -it / PANEL_LIFT } + fadeOut()
    return rise togetherWith lift
}

private val HeavyEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private const val PANEL_MILLIS = 480
private const val QUICK_FADE_MILLIS = 150
private const val PANEL_RISE = 3
private const val PANEL_LIFT = 6

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
