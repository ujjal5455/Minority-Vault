package com.example.minorityvault.engine.sim

import com.example.minorityvault.engine.AdvancePhase
import com.example.minorityvault.engine.ChooseTwist
import com.example.minorityvault.engine.GameConfig
import com.example.minorityvault.engine.GameEngine
import com.example.minorityvault.engine.GameState
import com.example.minorityvault.engine.Intent
import com.example.minorityvault.engine.Phase
import com.example.minorityvault.engine.PlayerId
import com.example.minorityvault.engine.PlayerIntent
import com.example.minorityvault.engine.PlayerSeat
import com.example.minorityvault.engine.PrivatePlayerView
import com.example.minorityvault.engine.PublicGameView
import com.example.minorityvault.engine.QuestionId
import com.example.minorityvault.engine.ReduceResult
import com.example.minorityvault.engine.SeededRandom
import com.example.minorityvault.engine.StartGame
import com.example.minorityvault.engine.toPrivateView
import com.example.minorityvault.engine.toPublicView

/** Decides one player's intents for the current phase, from only what that player may see. */
fun interface PlayerPolicy {
    fun decide(view: PublicGameView, me: PrivatePlayerView, random: SeededRandom): List<PlayerIntent>
}

data class SimulationResult(val finalState: GameState, val applied: Int, val rejected: Int)

/**
 * Plays a whole match: each phase, every player acts [ACTION_PASSES] times (so later proposals can be answered),
 * then the host advances. Policies get their own RNG stream, derived from [seed], so the game's RNG is untouched.
 * [observe] sees every state after an applied intent.
 */
fun simulateGame(
    config: GameConfig,
    seats: List<PlayerSeat>,
    policies: Map<PlayerId, PlayerPolicy>,
    seed: Long,
    questionIds: List<QuestionId> = placeholderQuestions(config.rounds),
    observe: (GameState) -> Unit = {},
): SimulationResult {
    val random = SeededRandom(seed xor POLICY_SEED_SALT)
    var state = GameState.newLobby(config, seats, questionIds, seed)
    var applied = 0
    var rejected = 0
    fun submit(intent: Intent) {
        when (val result = GameEngine.reduce(state, intent)) {
            is ReduceResult.Applied -> {
                state = result.state
                applied++
                observe(state)
            }

            is ReduceResult.Rejected -> rejected++
        }
    }

    submit(StartGame)
    check(state.phase == Phase.Question) { "the game did not start" }
    val maxPhases = config.rounds * PHASES_PER_ROUND
    var phases = 0
    while (state.phase != Phase.GameOver) {
        check(phases++ < maxPhases) { "the game did not end after $maxPhases phases" }
        hostChoice(state, random)?.let(::submit)
        repeat(ACTION_PASSES) {
            for (seat in seats) {
                val me = checkNotNull(state.toPrivateView(seat.id))
                policies.getValue(seat.id).decide(state.toPublicView(), me, random).forEach(::submit)
            }
        }
        submit(AdvancePhase(state.phase))
    }
    return SimulationResult(state, applied, rejected)
}

fun placeholderQuestions(count: Int): List<QuestionId> = (1..count).map { QuestionId("sim-$it") }

/** Like the Keeper: usually picks a twist, sometimes leaves the engine's fallback in place. */
private fun hostChoice(state: GameState, random: SeededRandom): Intent? {
    val options = state.round?.twistOptions.orEmpty()
    return if (state.phase == Phase.Question && options.isNotEmpty() && random.chance(CHOOSE_TWIST_CHANCE)) {
        ChooseTwist(random.pick(options).id)
    } else {
        null
    }
}

private const val POLICY_SEED_SALT = 0x5EED_CAFEL
private const val PHASES_PER_ROUND = 7
private const val ACTION_PASSES = 2
private const val CHOOSE_TWIST_CHANCE = 80
