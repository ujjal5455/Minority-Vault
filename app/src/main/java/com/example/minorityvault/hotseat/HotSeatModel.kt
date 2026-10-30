package com.example.minorityvault.hotseat

import com.example.minorityvault.engine.ContractStatus
import com.example.minorityvault.engine.GameConfig
import com.example.minorityvault.engine.Phase
import com.example.minorityvault.engine.QuestionId
import com.example.minorityvault.engine.toPrivateView
import com.example.minorityvault.engine.toPublicView
import com.example.minorityvault.feature.results.GameOverUiState
import com.example.minorityvault.feature.results.gameOverUiState
import com.example.minorityvault.feature.round.FlipUiState
import com.example.minorityvault.feature.round.NegotiationUiState
import com.example.minorityvault.feature.round.QuestionUiState
import com.example.minorityvault.feature.round.RevealUiState
import com.example.minorityvault.feature.round.RoundAction
import com.example.minorityvault.feature.round.VoteUiState
import com.example.minorityvault.feature.round.flipUiState
import com.example.minorityvault.feature.round.negotiationUiState
import com.example.minorityvault.feature.round.questionUiState
import com.example.minorityvault.feature.round.revealUiState
import com.example.minorityvault.feature.round.voteUiState

/** Everything the hot-seat screen shows: the setup names, then a session once the game starts. */
data class HotSeatModel(
    val names: List<String> = List(GameConfig.DEFAULT_MIN_PLAYERS) { "" },
    val session: HotSeatSession? = null,
) {
    /** Every seat named, and no two names the same. */
    val canStart: Boolean
        get() = names.all { it.isNotBlank() } && names.map { it.trim().lowercase() }.toSet().size == names.size
}

sealed interface HotSeatEvent {
    data class SetName(val index: Int, val name: String) : HotSeatEvent

    data object AddPlayer : HotSeatEvent

    data class RemovePlayer(val index: Int) : HotSeatEvent

    data object Start : HotSeatEvent

    data object ShowTurn : HotSeatEvent

    data object Continue : HotSeatEvent

    data class Round(val action: RoundAction) : HotSeatEvent

    data object FinishTurn : HotSeatEvent

    data object AnotherNegotiationPass : HotSeatEvent

    data object StartVote : HotSeatEvent

    data object PlayAgain : HotSeatEvent

    data object NewPlayers : HotSeatEvent
}

/** Pure: the next model for [event]. [nextSeed] is only called when a game starts. */
fun HotSeatModel.on(event: HotSeatEvent, nextSeed: () -> Long, questions: List<QuestionId>): HotSeatModel {
    val session = session
    return when (event) {
        is HotSeatEvent.SetName -> copy(
            names = names.toMutableList().also {
                it[event.index] = event.name.take(MAX_NAME)
            },
        )

        HotSeatEvent.AddPlayer ->
            if (names.size < GameConfig.DEFAULT_MAX_PLAYERS) copy(names = names + "") else this

        is HotSeatEvent.RemovePlayer ->
            if (names.size >
                GameConfig.DEFAULT_MIN_PLAYERS
            ) {
                copy(names = names.filterIndexed { i, _ -> i != event.index })
            } else {
                this
            }

        HotSeatEvent.Start, HotSeatEvent.PlayAgain ->
            if (canStart) copy(session = HotSeatSession.start(names, nextSeed(), questions)) else this

        HotSeatEvent.NewPlayers -> copy(session = null)

        else -> if (session == null) this else copy(session = session.on(event))
    }
}

private fun HotSeatSession.on(event: HotSeatEvent): HotSeatSession = when (event) {
    HotSeatEvent.ShowTurn -> showTurn()
    HotSeatEvent.Continue -> continuePublic()
    is HotSeatEvent.Round -> act(event.action)
    HotSeatEvent.FinishTurn -> finishTurn()
    HotSeatEvent.AnotherNegotiationPass -> anotherNegotiationPass()
    HotSeatEvent.StartVote -> startVote()
    else -> this
}

/** What to draw. Private states are built only for the player holding the phone. */
sealed interface HotSeatUiState {
    data class Setup(val names: List<String>, val canStart: Boolean, val canAdd: Boolean, val canRemove: Boolean) :
        HotSeatUiState

    data class PassPhone(val name: String, val phase: Phase) : HotSeatUiState

    data class NegotiationPassDone(val pendingContracts: Int) : HotSeatUiState

    data class Question(val state: QuestionUiState) : HotSeatUiState

    data class Negotiation(val state: NegotiationUiState) : HotSeatUiState

    data class Vote(val state: VoteUiState) : HotSeatUiState

    data class Flip(val state: FlipUiState) : HotSeatUiState

    data class Reveal(val state: RevealUiState) : HotSeatUiState

    data class GameOver(val state: GameOverUiState) : HotSeatUiState
}

fun HotSeatModel.toUiState(): HotSeatUiState {
    val session = session ?: return HotSeatUiState.Setup(
        names = names,
        canStart = canStart,
        canAdd = names.size < GameConfig.DEFAULT_MAX_PLAYERS,
        canRemove = names.size > GameConfig.DEFAULT_MIN_PLAYERS,
    )
    val game = session.game
    val view = game.toPublicView()
    return when (val step = session.step) {
        HotSeatStep.Question -> HotSeatUiState.Question(questionUiState(view))

        is HotSeatStep.Cover -> HotSeatUiState.PassPhone(session.currentPlayer.name, step.phase)

        HotSeatStep.NegotiationPassDone -> HotSeatUiState.NegotiationPassDone(
            game.round?.contracts.orEmpty().count { it.status == ContractStatus.PENDING },
        )

        is HotSeatStep.Turn -> {
            val me = checkNotNull(game.toPrivateView(step.player))
            when (step.phase) {
                Phase.Negotiation -> HotSeatUiState.Negotiation(negotiationUiState(view, me, error = session.error))
                Phase.Vote -> HotSeatUiState.Vote(voteUiState(view, me, session.doubleArmed, error = session.error))
                else -> HotSeatUiState.Flip(flipUiState(view, me, error = session.error))
            }
        }

        HotSeatStep.Reveal -> HotSeatUiState.Reveal(checkNotNull(revealUiState(view)))

        HotSeatStep.GameOver -> HotSeatUiState.GameOver(gameOverUiState(view))
    }
}

private const val MAX_NAME = 20
