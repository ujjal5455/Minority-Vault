package com.example.minorityvault.feature.round

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import com.example.minorityvault.designsystem.CountingText
import com.example.minorityvault.designsystem.VaultPreviewTheme
import com.example.minorityvault.designsystem.VaultSpacing
import com.example.minorityvault.designsystem.VaultTheme
import com.example.minorityvault.designsystem.component.CoinBurst
import com.example.minorityvault.designsystem.component.Stamp
import com.example.minorityvault.designsystem.component.VaultButton
import com.example.minorityvault.designsystem.component.VaultDial
import com.example.minorityvault.designsystem.preview.ScreenPreviews
import com.example.minorityvault.engine.Side
import com.example.minorityvault.engine.Vote
import com.example.minorityvault.engine.VoteChoice
import com.example.minorityvault.engine.VoteTotals
import kotlinx.coroutines.delay

/**
 * The public reveal, told in order: the dial spins and settles, the result lands, each vote flips over one
 * at a time, coins fly to the winners, stamps slam on broken promises, and balances count to their new
 * values. Reduced motion shows everything at once. [onContinue] is null when the host moves everyone on.
 */
@Composable
fun RevealScreen(state: RevealUiState, onContinue: (() -> Unit)?, modifier: Modifier = Modifier) {
    val colors = VaultTheme.colors
    val reducedMotion = VaultTheme.reducedMotion
    val stages = RevealStages(voteCount = state.players.size)
    var dialSettled by remember(state.header.round) { mutableStateOf(reducedMotion) }
    var stage by remember(state.header.round) { mutableIntStateOf(if (reducedMotion) stages.last else 0) }
    LaunchedEffect(dialSettled) {
        if (!dialSettled) return@LaunchedEffect
        while (stage < stages.last) {
            stage++
            delay(stages.pauseAfter(stage))
        }
    }
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
            onSettled = { dialSettled = true },
        )
        AnimatedVisibility(stage >= stages.result, enter = fadeIn() + scaleIn(initialScale = RESULT_START_SCALE)) {
            Text(
                state.minoritySide?.let { stringResource(R.string.reveal_minority, sideLabel(it)) }
                    ?: stringResource(R.string.reveal_no_minority),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.brass,
            )
        }
        SectionTitle(stringResource(R.string.reveal_votes))
        VoteList(state, stage, stages)
        AnimatedVisibility(stage >= stages.payout, enter = fadeIn() + expandVertically()) {
            Payout(state)
        }
        AnimatedVisibility(stage >= stages.details, enter = fadeIn() + expandVertically()) {
            Details(state)
        }
        SectionTitle(stringResource(R.string.reveal_balances))
        val shownPlayers = if (stage >= stages.balances) {
            state.players
        } else {
            state.players.map { it.copy(balance = state.balancesBefore[it.id] ?: it.balance) }
        }
        PlayerGrid(shownPlayers)
    }
}

/** The order of the reveal, and how long each beat holds before the next. */
private class RevealStages(private val voteCount: Int) {
    val result = 1
    fun vote(index: Int) = FIRST_VOTE + index
    val payout get() = FIRST_VOTE + voteCount
    val details get() = payout + 1
    val balances get() = payout + 2
    val last get() = balances

    fun pauseAfter(stage: Int): Long = when {
        stage == result -> RESULT_HOLD_MILLIS
        stage < payout -> VOTE_HOLD_MILLIS
        stage == payout -> PAYOUT_HOLD_MILLIS
        else -> DETAILS_HOLD_MILLIS
    }

    private companion object {
        const val FIRST_VOTE = 2
    }
}

/** Who voted what, each vote flipping over in turn. Minority votes in brass; stamps slam on promise breakers. */
@Composable
private fun VoteList(state: RevealUiState, stage: Int, stages: RevealStages) {
    val colors = VaultTheme.colors
    state.players.forEachIndexed { index, player ->
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BodyText(player.name, Modifier.weight(1f))
            if (player.id in state.promiseBreakers && stage >= stages.details) {
                // The stamp is drawn tilted, so leave room for its corners.
                Stamp(stringResource(R.string.reveal_broken), Modifier.padding(horizontal = VaultSpacing.lg))
            }
            FlipReveal(revealed = stage >= stages.vote(index)) { shown ->
                Text(
                    if (shown) voteLabel(state.votes[player.id] ?: Vote.ABSTAIN) else HIDDEN_VOTE,
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (shown && player.id in state.minorityPlayers) colors.brass else colors.onPanelMuted,
                )
            }
        }
    }
}

/** Flips over top to bottom like a split-flap sign, showing [content] (false) then (true). */
@Composable
private fun FlipReveal(revealed: Boolean, content: @Composable (shown: Boolean) -> Unit) {
    val angle by animateFloatAsState(
        targetValue = if (revealed) HALF_TURN else 0f,
        animationSpec = if (VaultTheme.reducedMotion) snap() else tween(FLIP_MILLIS),
        label = "vote flip",
    )
    val showingBack = angle > QUARTER_TURN
    Box(
        Modifier.graphicsLayer {
            rotationX = if (showingBack) angle - HALF_TURN else angle
            cameraDistance = CAMERA_DISTANCE * density
        },
    ) {
        content(showingBack)
    }
}

@Composable
private fun Payout(state: RevealUiState) {
    val names = state.winners.joinToString(", ") { state.players.nameOf(it) }
    Column(verticalArrangement = Arrangement.spacedBy(VaultSpacing.xs)) {
        Box {
            when {
                state.winners.isNotEmpty() -> CountingText(
                    value = state.share,
                    format = {
                        if (state.winners.size == 1) {
                            stringResource(R.string.reveal_winner_single, names, it)
                        } else {
                            stringResource(R.string.reveal_winners, names, it)
                        }
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = VaultTheme.colors.onPanel,
                    countFromZero = true,
                )

                state.minoritySide != null -> BodyText(stringResource(R.string.reveal_no_winners_twist))
            }
            CoinBurst(trigger = state.winners.isNotEmpty() && state.share > 0, modifier = Modifier.matchParentSize())
        }
        if (state.potLeft > 0) BodyText(stringResource(R.string.reveal_rollover, state.potLeft))
    }
}

@Composable
private fun Details(state: RevealUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(VaultSpacing.xs)) {
        if (state.fines.values.any { it > 0 }) {
            SectionTitle(stringResource(R.string.reveal_fines))
            state.fines.filterValues { it > 0 }.forEach { (id, amount) ->
                BodyText(stringResource(R.string.reveal_fine_line, state.players.nameOf(id), amount))
            }
        }
        state.newDebtors.forEach { DebtorLine(stringResource(R.string.reveal_new_debtor, state.players.nameOf(it))) }
        state.recoveredDebtors.forEach { BodyText(stringResource(R.string.reveal_recovered, state.players.nameOf(it))) }
    }
}

@Composable
private fun DebtorLine(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = VaultTheme.colors.stampRed)
}

private const val HIDDEN_VOTE = "?"
private const val RESULT_START_SCALE = 0.7f
private const val FLIP_MILLIS = 420
private const val HALF_TURN = 180f
private const val QUARTER_TURN = 90f
private const val CAMERA_DISTANCE = 16f
private const val RESULT_HOLD_MILLIS = 700L
private const val VOTE_HOLD_MILLIS = 380L
private const val PAYOUT_HOLD_MILLIS = 1300L
private const val DETAILS_HOLD_MILLIS = 900L

@ScreenPreviews
@Composable
private fun RevealScreenPreview() {
    val p = PreviewData
    VaultPreviewTheme {
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
                balancesBefore = p.players.associate { it.id to it.balance } + (p.asha to 93),
            ),
            onContinue = {},
        )
    }
}
