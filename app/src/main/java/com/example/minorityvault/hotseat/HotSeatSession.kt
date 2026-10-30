package com.example.minorityvault.hotseat

import com.example.minorityvault.engine.AdvancePhase
import com.example.minorityvault.engine.ChooseTwist
import com.example.minorityvault.engine.GameConfig
import com.example.minorityvault.engine.GameEngine
import com.example.minorityvault.engine.GameState
import com.example.minorityvault.engine.Intent
import com.example.minorityvault.engine.Phase
import com.example.minorityvault.engine.Player
import com.example.minorityvault.engine.PlayerId
import com.example.minorityvault.engine.PlayerSeat
import com.example.minorityvault.engine.QuestionId
import com.example.minorityvault.engine.ReduceResult
import com.example.minorityvault.engine.RuleError
import com.example.minorityvault.engine.StartGame
import com.example.minorityvault.feature.round.RoundAction
import com.example.minorityvault.feature.round.toIntents

/**
 * One phone passed around the table. Public phases (question, reveal, game over) are shown to everyone.
 * Private phases (negotiation, vote, flip window) go player by player, with a cover screen between turns
 * so nobody sees someone else's secrets. The engine stays the only source of rules.
 */
data class HotSeatSession(
    val game: GameState,
    /** Seat index of the player holding the phone in a private phase. */
    val turn: Int = 0,
    /** True while the "pass the phone" cover hides the current player's screen. */
    val coverUp: Boolean = true,
    /** The current player has chosen to use their Double Vote when they lock. */
    val doubleArmed: Boolean = false,
    val error: RuleError? = null,
    /** Everyone has had a negotiation turn; the table decides whether to go round again or vote. */
    val negotiationPassDone: Boolean = false,
) {
    val currentPlayer: Player get() = game.players[turn]

    val step: HotSeatStep
        get() = when (game.phase) {
            Phase.Lobby, Phase.Question -> HotSeatStep.Question

            Phase.Negotiation, Phase.Vote, Phase.FlipWindow -> when {
                game.phase == Phase.Negotiation && negotiationPassDone -> HotSeatStep.NegotiationPassDone
                coverUp -> HotSeatStep.Cover(currentPlayer.id, game.phase)
                else -> HotSeatStep.Turn(currentPlayer.id, game.phase)
            }

            Phase.Reveal, Phase.Settlement -> HotSeatStep.Reveal

            Phase.GameOver -> HotSeatStep.GameOver
        }

    fun showTurn(): HotSeatSession = copy(coverUp = false)

    /** Applies a round action for the current player. All its intents apply, or none do. */
    fun act(action: RoundAction): HotSeatSession {
        if (action is RoundAction.ArmDoubleVote) return copy(doubleArmed = action.armed, error = null)
        val ownVoteChanged =
            (action is RoundAction.Lock || action is RoundAction.Unlock) && action.forPlayerOrNull == null
        return when (val result = game.applyAll(action.toIntents(currentPlayer.id, doubleArmed))) {
            is ReduceResult.Applied -> copy(
                game = result.state,
                error = null,
                doubleArmed =
                doubleArmed && !ownVoteChanged,
            )

            is ReduceResult.Rejected -> copy(error = result.error)
        }
    }

    /** The current player hands the phone on. After the last player, the phase moves on. */
    fun finishTurn(): HotSeatSession {
        val reset = copy(coverUp = true, doubleArmed = false, error = null)
        if (turn + 1 < game.players.size) return reset.copy(turn = turn + 1)
        return when (game.phase) {
            Phase.Negotiation -> reset.copy(negotiationPassDone = true)
            Phase.Vote, Phase.FlipWindow -> reset.copy(game = game.advanced(), turn = 0)
            else -> reset
        }
    }

    fun anotherNegotiationPass(): HotSeatSession = copy(negotiationPassDone = false, turn = 0, coverUp = true)

    fun startVote(): HotSeatSession =
        copy(game = game.advanced(), negotiationPassDone = false, turn = 0, coverUp = true, error = null)

    /** Moves on from a public screen: QUESTION to the first negotiation turn, or past the reveal. */
    fun continuePublic(): HotSeatSession = when (game.phase) {
        Phase.Question -> copy(game = game.advanced(), turn = 0, coverUp = true)
        Phase.Reveal -> copy(game = game.advanced().let { if (it.phase == Phase.Settlement) it.advanced() else it })
        Phase.Settlement -> copy(game = game.advanced())
        else -> this
    }.let { it.copy(game = it.game.withTwistAnnounced()) }

    companion object {
        fun start(names: List<String>, seed: Long, questions: List<QuestionId>, config: GameConfig = GameConfig()) =
            HotSeatSession(
                GameState.newLobby(
                    config,
                    names.mapIndexed { i, name -> PlayerSeat(PlayerId("p${i + 1}"), name.trim()) },
                    questions,
                    seed,
                ).applyOrThrow(StartGame).withTwistAnnounced(),
            )
    }
}

sealed interface HotSeatStep {
    data object Question : HotSeatStep

    data class Cover(val player: PlayerId, val phase: Phase) : HotSeatStep

    data class Turn(val player: PlayerId, val phase: Phase) : HotSeatStep

    data object NegotiationPassDone : HotSeatStep

    data object Reveal : HotSeatStep

    data object GameOver : HotSeatStep
}

private val RoundAction.forPlayerOrNull: PlayerId?
    get() = when (this) {
        is RoundAction.Lock -> forPlayer
        is RoundAction.Unlock -> forPlayer
        else -> null
    }

/**
 * Pass-the-phone play has no Vault Keeper yet, so as soon as a round starts it takes the engine's fallback
 * twist (the first option) and the question screen can announce it.
 */
private fun GameState.withTwistAnnounced(): GameState {
    val first = round?.twistOptions?.firstOrNull()
    return if (phase == Phase.Question && round?.twist == null && first != null) {
        applyOrThrow(ChooseTwist(first.id))
    } else {
        this
    }
}

/** Applies [intents] in order, stopping at the first refusal. */
private fun GameState.applyAll(intents: List<Intent>): ReduceResult {
    var state = this
    for (intent in intents) {
        when (val result = GameEngine.reduce(state, intent)) {
            is ReduceResult.Applied -> state = result.state
            is ReduceResult.Rejected -> return result
        }
    }
    return ReduceResult.Applied(state)
}

/** The host always advances from the phase it is in, so a rejection here is a bug. */
private fun GameState.advanced(): GameState = applyOrThrow(AdvancePhase(phase))

private fun GameState.applyOrThrow(intent: Intent): GameState = when (val result = GameEngine.reduce(this, intent)) {
    is ReduceResult.Applied -> result.state
    is ReduceResult.Rejected -> error("Hot seat sent an illegal $intent in $phase: ${result.error}")
}
