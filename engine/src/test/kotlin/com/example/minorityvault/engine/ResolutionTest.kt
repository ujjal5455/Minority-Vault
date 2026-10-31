package com.example.minorityvault.engine

import com.example.minorityvault.engine.Side.NO
import com.example.minorityvault.engine.Side.YES
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

// Five players with 100 coins each; round 1 fine is 10.
class ResolutionTest {
    @Test
    fun `the smaller side splits the majority's fines`() {
        val state = startedGame().playVote(P1 to YES, P2 to YES, P3 to NO, P4 to NO, P5 to NO)

        state.phase shouldBe Phase.Reveal
        state.summary.minoritySide shouldBe YES
        state.summary.minorityPlayers shouldBe listOf(P1, P2)
        state.summary.fines shouldBe mapOf(P3 to 10, P4 to 10, P5 to 10)
        state.balances() shouldBe listOf(115, 115, 90, 90, 90)
        state.pot shouldBe 0
    }

    @Test
    fun `a double vote counts twice and can change which side is smaller`() {
        val state = startedGame().playVote(P1 to YES, P2 to YES, P3 to NO, P4 to NO, doubled = setOf(P1))

        state.summary.totals shouldBe VoteTotals(yes = 3, no = 2)
        state.summary.minorityPlayers shouldBe listOf(P3, P4)
        state.summary.fines.keys shouldBe setOf(P1, P2, P5)
        state.balance(P3) shouldBe 115
        checkNotNull(state.player(P1)).itemCount(Item.DOUBLE_VOTE) shouldBe 0
    }

    @Test
    fun `abstainers pay the fine but never win`() {
        val state = startedGame().playVote(P1 to YES, P2 to NO, P3 to NO)

        state.summary.minorityPlayers shouldBe listOf(P1)
        state.summary.fines.keys shouldBe setOf(P2, P3, P4, P5)
        state.balance(P1) shouldBe 140
        state.balance(P4) shouldBe 90
    }

    @Test
    fun `a tie means no minority, everyone pays and the pot rolls over`() {
        val state = startedGame().playVote(P1 to YES, P2 to YES, P3 to NO, P4 to NO)

        state.summary.minoritySide shouldBe null
        state.summary.winners shouldBe emptyList()
        state.summary.fines.keys shouldBe setOf(P1, P2, P3, P4, P5)
        state.pot shouldBe 50
        state.balances() shouldBe List(5) { 90 }
    }

    @Test
    fun `an empty side means no minority`() {
        val state = startedGame().playVote(P1 to YES, P2 to YES, P3 to YES, P4 to YES, P5 to YES)

        state.summary.minoritySide shouldBe null
        state.pot shouldBe 50
    }

    @Test
    fun `when everyone abstains, everyone pays and the pot rolls over`() {
        val state = startedGame().playVote()

        state.summary.totals shouldBe VoteTotals(yes = 0, no = 0)
        state.summary.minoritySide shouldBe null
        state.pot shouldBe 50
    }

    @Test
    fun `the remainder of an uneven split stays in the pot`() {
        val state = startedGame().copy(pot = 1).playVote(P1 to YES, P2 to YES, P3 to NO, P4 to NO, P5 to NO)

        state.summary.winnings shouldBe mapOf(P1 to 15, P2 to 15)
        state.pot shouldBe 1
    }

    @Test
    fun `a rolled-over pot is paid out in a later round`() {
        val state = startedGame().playVote(P1 to YES, P2 to YES, P3 to NO, P4 to NO)
            .advanceTo(Phase.Question)
            .playVote(P1 to YES, P2 to YES, P3 to NO, P4 to NO, P5 to NO)

        state.summary.winnings shouldBe mapOf(P1 to 40, P2 to 40)
        state.pot shouldBe 0
    }

    @Test
    fun `a fine is capped at the payer's balance and leaving them at 0 makes them a Debtor`() {
        val voting = startedGame().withBalances(100, 100, 4, 100, 100).advanceTo(Phase.Vote)
            .vote(P1, YES).vote(P2, YES).vote(P3, NO).vote(P4, NO).vote(P5, NO)

        val events = voting.events(AdvancePhase(Phase.Vote))
        val state = voting.advance()

        state.summary.fines.getValue(P3) shouldBe 4
        state.balance(P3) shouldBe 0
        checkNotNull(state.player(P3)).isDebtor shouldBe true
        state.summary.newDebtors shouldBe listOf(P3)
        events shouldContain PublicEvent.BecameDebtor(P3)
    }

    @Test
    fun `a Debtor in the majority pays nothing and stays a Debtor`() {
        val state = startedGame().asDebtor(P5).playVote(P1 to YES, P2 to YES, P3 to NO, P4 to NO, P5 to NO)

        state.summary.fines.getValue(P5) shouldBe 0
        checkNotNull(state.player(P5)).isDebtor shouldBe true
        state.summary.newDebtors shouldBe emptyList()
    }

    @Test
    fun `a Debtor who wins coins stops being a Debtor`() {
        val state = startedGame().asDebtor(P1).playVote(P1 to YES, P2 to YES, P3 to NO, P4 to NO, P5 to NO)

        state.balance(P1) shouldBe 15
        checkNotNull(state.player(P1)).isDebtor shouldBe false
        state.summary.recoveredDebtors shouldBe listOf(P1)
    }

    @Test
    fun `the final round fines half of each payer's balance, rounded down`() {
        val state = startedGame(config = GameConfig(rounds = 1))
            .withBalances(100, 100, 75, 100, 1)
            .playVote(P1 to YES, P2 to YES, P3 to NO, P4 to NO, P5 to NO)

        state.summary.fines shouldBe mapOf(P3 to 37, P4 to 50, P5 to 0)
        state.summary.winnings shouldBe mapOf(P1 to 43, P2 to 43)
        state.pot shouldBe 1
    }

    @Test
    fun `the reveal sequence tells the round in order`() {
        val state = startedGame().playVote(P1 to YES, P2 to YES, P3 to NO, P4 to NO, P5 to NO)

        state.summary.reveal.steps.map { it::class } shouldBe listOf(
            RevealStep.VotesShown::class,
            RevealStep.TotalsShown::class,
            RevealStep.MinorityFound::class,
            RevealStep.TwistResolved::class,
            RevealStep.FinesPaid::class,
            RevealStep.PotSplit::class,
            RevealStep.Settled::class,
        )
        state.summary.reveal.steps.last() shouldBe RevealStep.Settled(
            balances = mapOf(P1 to 115, P2 to 115, P3 to 90, P4 to 90, P5 to 90),
            pot = 0,
            previous = mapOf(P1 to 100, P2 to 100, P3 to 100, P4 to 100, P5 to 100),
        )
    }

    @Test
    fun `coins are conserved through a round`() {
        val before = startedGame()
        val after = before.playVote(P1 to YES, P2 to NO, P3 to NO, doubled = setOf(P2))

        after.totalCoins shouldBe before.totalCoins
    }
}
