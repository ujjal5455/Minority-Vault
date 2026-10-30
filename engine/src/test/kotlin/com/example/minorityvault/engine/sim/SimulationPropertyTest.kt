package com.example.minorityvault.engine.sim

import com.example.minorityvault.engine.ContractTerms
import com.example.minorityvault.engine.GameConfig
import com.example.minorityvault.engine.GameState
import com.example.minorityvault.engine.Phase
import com.example.minorityvault.engine.PromiseVerdict
import com.example.minorityvault.engine.RoundSummary
import com.example.minorityvault.engine.TwistId
import com.example.minorityvault.engine.isRevealed
import com.example.minorityvault.engine.seats
import com.example.minorityvault.engine.toPrivateView
import com.example.minorityvault.engine.toPublicView
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SimulationPropertyTest {
    // Low balances and short matches make Debtors, vote sales and all-in rounds common.
    private val configs = listOf(
        GameConfig(),
        GameConfig(startingBalance = 30, rounds = 5),
        GameConfig(startingBalance = 12, rounds = 3, contractFee = 3),
    )

    private fun play(game: Int, observe: (GameState) -> Unit = {}): SimulationResult {
        val seats = seats(4 + game % 5)
        return simulateGame(
            config = configs[game % configs.size],
            seats = seats,
            policies = seats.associate { it.id to RandomPolicy() },
            seed = game.toLong(),
            observe = observe,
        )
    }

    @Test
    fun `1000 seeded random games keep every invariant`() {
        val histories = mutableListOf<RoundSummary>()
        repeat(GAMES) { game ->
            var initialCoins = -1
            val result = play(game) { state ->
                if (initialCoins < 0) initialCoins = state.totalCoins
                checkInvariants(state, initialCoins)
            }
            val final = result.finalState
            final.phase shouldBe Phase.GameOver
            final.history.size shouldBe final.config.rounds
            final.winners.all { id -> final.player(id)?.isDebtor == false } shouldBe true
            histories += final.history
        }
        checkCoverage(histories)
    }

    @Test
    fun `the same seed replays the same game`() {
        for (game in 0 until 20) {
            play(game) shouldBe play(game)
        }
    }

    private fun checkInvariants(state: GameState, initialCoins: Int) {
        check(state.totalCoins == initialCoins) { "coins not conserved: ${state.totalCoins} != $initialCoins" }
        check(state.pot >= 0) { "negative pot" }
        check(state.players.all { it.balance >= 0 }) { "negative balance" }
        if (state.phase.isRevealed) {
            check(state.players.all { it.isDebtor == (it.balance == 0) }) { "debtor status out of date" }
        }
        val public = state.toPublicView()
        check((public.reveal != null) == state.phase.isRevealed) { "reveal shown in ${state.phase}" }
        check((public.flipWindowTotals != null) == (state.phase == Phase.FlipWindow)) {
            "totals shown in ${state.phase}"
        }
        for (player in state.players) {
            val private = checkNotNull(state.toPrivateView(player.id))
            check(private.ballot == null || state.phase == Phase.Vote) { "ballot shown in ${state.phase}" }
            check(private.insiderTotals == null || private.isInsider) { "totals leaked to a non-Insider" }
            check(private.contracts.all { player.id in it.terms.parties }) { "someone else's contract leaked" }
            check(private.promisesReceived.all { it.from != player.id }) { "own promise listed as received" }
        }
    }

    /** Proves the random games actually reach the interesting rules, so the invariants mean something. */
    private fun checkCoverage(histories: List<RoundSummary>) {
        val verdicts = histories.flatMap { it.promises }.map { it.verdict }
        val contracts = histories.flatMap { it.contracts }.map { it.terms::class }

        histories.map { it.twist.id }.toSet() shouldContainAll TwistId.entries
        verdicts.toSet() shouldContainAll PromiseVerdict.entries
        contracts.toSet() shouldContainAll listOf(
            ContractTerms.ShareWinnings::class,
            ContractTerms.FixedPayment::class,
            ContractTerms.VoteSale::class,
        )
        histories.any { it.minoritySide == null } shouldBe true
        histories.any { it.newDebtors.isNotEmpty() } shouldBe true
        histories.any { it.recoveredDebtors.isNotEmpty() } shouldBe true
        histories.any { summary -> summary.votes.values.any { it.flipped } } shouldBe true
        histories.any { summary -> summary.votes.values.any { it.doubled } } shouldBe true
        histories.any { summary -> summary.transfers.any { it.paid < it.owed } } shouldBe true
    }

    private companion object {
        const val GAMES = 1000
    }
}
