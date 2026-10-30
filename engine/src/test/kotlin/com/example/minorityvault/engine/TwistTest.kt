package com.example.minorityvault.engine

import com.example.minorityvault.engine.Side.NO
import com.example.minorityvault.engine.Side.YES
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

// Five players with 100 coins each; round 1 fine is 10.
class TwistTest {
    @Test
    fun `ExactMinority pays the minority when it has exactly the right size`() {
        val state = startedGame(twist = Twist.ExactMinority(2))
            .playVote(P1 to YES, P2 to YES, P3 to NO, P4 to NO, P5 to NO)

        state.summary.winners shouldBe listOf(P1, P2)
        state.balance(P1) shouldBe 115
    }

    @Test
    fun `ExactMinority of the wrong size pays nobody, but the majority still pays and the pot rolls over`() {
        val state = startedGame(twist = Twist.ExactMinority(1))
            .playVote(P1 to YES, P2 to YES, P3 to NO, P4 to NO, P5 to NO)

        state.summary.minorityPlayers shouldBe listOf(P1, P2)
        state.summary.winners shouldBe emptyList()
        state.summary.fines.keys shouldBe setOf(P3, P4, P5)
        state.balance(P1) shouldBe 100
        state.pot shouldBe 30
    }

    @Test
    fun `ExactMinority must be smaller than half the table`() {
        val four = lobby(players = 4).players
        val five = lobby(players = 5).players

        Twist.ExactMinority(1).isValid(four) shouldBe true
        Twist.ExactMinority(2).isValid(four) shouldBe false
        Twist.ExactMinority(2).isValid(five) shouldBe true
        Twist.ExactMinority(0).isValid(five) shouldBe false
    }

    @Test
    fun `SecondChance shows only totals, then lets each voter flip once`() {
        val window = startedGame(twist = Twist.SecondChance).playVote(P1 to YES, P2 to YES, P3 to NO, P4 to NO)

        window.phase shouldBe Phase.FlipWindow
        window.toPublicView().flipWindowTotals shouldBe VoteTotals(yes = 2, no = 2)

        val flipped = window.apply(VoteFlip(P3))
        flipped.toPublicView().flipWindowTotals shouldBe VoteTotals(yes = 3, no = 1)
        flipped.rejection(VoteFlip(P3)) shouldBe RuleError.AlreadyFlipped
        flipped.rejection(VoteFlip(P5)) shouldBe RuleError.CannotFlipAbstain

        val state = flipped.advance()
        state.summary.minorityPlayers shouldBe listOf(P4)
        state.summary.votes.getValue(P3) shouldBe Vote(VoteChoice.YES, flipped = true)
        state.balance(P4) shouldBe 140
    }

    @Test
    fun `without SecondChance there is no flip window`() {
        val state = startedGame().playVote(P1 to YES)

        state.phase shouldBe Phase.Reveal
        state.rejection(VoteFlip(P1)) shouldBe RuleError.WrongPhase(Phase.Reveal)
    }

    @Test
    fun `the Insider alone sees live totals of locked votes, and stays anonymous`() {
        val state = startedGame(twist = Twist.Insider(P2))
            .advanceTo(Phase.Vote)
            .vote(P1, YES, doubled = true)
            .apply(VoteSelect(P3, NO))

        checkNotNull(state.toPrivateView(P2)).insiderTotals shouldBe VoteTotals(yes = 2, no = 0)
        checkNotNull(state.toPrivateView(P2)).isInsider shouldBe true
        checkNotNull(state.toPrivateView(P1)).insiderTotals shouldBe null
        checkNotNull(state.toPrivateView(P1)).isInsider shouldBe false
        state.toPublicView().twist shouldBe TwistInfo(TwistId.INSIDER)
    }

    @Test
    fun `RobinHood gives the pot to the poorest player after fines`() {
        val state = startedGame(twist = Twist.RobinHood)
            .withBalances(100, 100, 100, 100, 40)
            .playVote(P1 to YES, P2 to YES, P3 to NO, P4 to NO, P5 to NO)

        state.summary.winners shouldBe listOf(P5)
        state.balance(P5) shouldBe 60
        state.balance(P1) shouldBe 100
    }

    @Test
    fun `RobinHood splits the pot between players tied for poorest`() {
        val state = startedGame(twist = Twist.RobinHood)
            .withBalances(100, 100, 100, 50, 50)
            .playVote(P1 to YES, P2 to YES, P3 to NO, P4 to NO, P5 to NO)

        state.summary.winners shouldBe listOf(P4, P5)
        state.balance(P4) shouldBe 55
        state.balance(P5) shouldBe 55
    }

    @Test
    fun `RobinHood pays nobody when there is no minority`() {
        val state = startedGame(twist = Twist.RobinHood).playVote(P1 to YES, P2 to NO)

        state.summary.winners shouldBe emptyList()
        state.pot shouldBe 50
    }

    @Test
    fun `the catalog offers three valid twists with distinct ids, the same for the same seed`() {
        for (players in 4..8) {
            for (seed in 1L..40L) {
                val table = lobby(players).players
                val offers = TwistCatalog.offerTwists(table, SeededRandom(seed), count = 3)

                offers shouldHaveSize 3
                offers.map { it.id }.toSet() shouldHaveSize 3
                offers.all { it.isValid(table) } shouldBe true
                TwistCatalog.offerTwists(table, SeededRandom(seed), count = 3) shouldBe offers
            }
        }
    }

    @Test
    fun `every twist gets offered`() {
        val table = lobby().players
        val offered = (1L..200L).flatMap { TwistCatalog.offerTwists(table, SeededRandom(it), 3) }.map { it.id }

        offered shouldContainAll TwistId.entries
    }

    @Test
    fun `the Keeper can only choose an offered twist, during QUESTION`() {
        val started = lobby().apply(StartGame).withRound { it.copy(twistOptions = listOf(Twist.RobinHood)) }

        started.rejection(ChooseTwist(TwistId.INSIDER)) shouldBe RuleError.TwistNotOffered
        started.apply(ChooseTwist(TwistId.ROBIN_HOOD)).advance()
            .rejection(ChooseTwist(TwistId.ROBIN_HOOD)) shouldBe RuleError.WrongPhase(Phase.Negotiation)
    }

    @Test
    fun `with no choice from the Keeper the first offered twist is used`() {
        val started = lobby().apply(StartGame)

        started.advance().round?.twist shouldBe checkNotNull(started.round).twistOptions.first()
    }
}
