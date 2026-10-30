package com.example.minorityvault.engine

import com.example.minorityvault.engine.ContractTerms.FixedPayment
import com.example.minorityvault.engine.PaymentCondition.ALWAYS
import com.example.minorityvault.engine.PromiseTarget.ToPlayer
import com.example.minorityvault.engine.Side.NO
import com.example.minorityvault.engine.Side.YES
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * CLAUDE.md §3 rule 3. The strongest check here is differential: two states that differ only in another
 * player's secrets must look identical to everyone else.
 */
class ViewLeakTest {
    private fun GameState.viewsFor(viewer: PlayerId) = toPublicView() to toPrivateView(viewer)

    @Test
    fun `another player's vote choice and Double Vote are invisible before REVEAL`() {
        val base = startedGame().advanceTo(Phase.Vote).vote(P1, YES)
        val votesYes = base.apply(VoteSelect(P2, YES))
        val votesNoDoubled = base.apply(VoteSelect(P2, NO)).apply(UseItem(P2, Item.DOUBLE_VOTE))

        for (viewer in listOf(P1, P3, P4, P5)) {
            votesYes.viewsFor(viewer) shouldBe votesNoDoubled.viewsFor(viewer)
        }
    }

    @Test
    fun `locked votes show a lock dot but not the side`() {
        val lockedYes = startedGame().advanceTo(Phase.Vote).vote(P2, YES)
        val lockedNo = startedGame().advanceTo(Phase.Vote).vote(P2, NO)

        lockedYes.toPublicView().players.single { it.id == P2 }.hasLocked shouldBe true
        lockedYes.viewsFor(P1) shouldBe lockedNo.viewsFor(P1)
    }

    @Test
    fun `private promises and pending contracts are invisible to third parties`() {
        val base = startedGame().advanceTo(Phase.Negotiation)
        val secretive = base
            .apply(MakePromise(P2, ToPlayer(P3), YES))
            .apply(ContractPropose(P2, FixedPayment(P2, P3, 10, ALWAYS)))

        for (viewer in listOf(P1, P4, P5)) {
            base.viewsFor(viewer) shouldBe secretive.viewsFor(viewer)
        }
        val recipient = checkNotNull(secretive.toPrivateView(P3))
        recipient.promisesReceived shouldBe listOf(Promise(P2, ToPlayer(P3), YES))
        recipient.contracts.single().proposer shouldBe P2
    }

    @Test
    fun `votes appear only from REVEAL onwards`() {
        val game = startedGame()
        val hidden = listOf(game, game.advance(), game.advanceTo(Phase.Vote).vote(P1, YES))
        hidden.forEach { it.toPublicView().reveal shouldBe null }

        val revealed = game.playVote(P1 to YES, P2 to NO, P3 to NO)
        val votes = revealed.toPublicView().reveal.shouldNotBeNull().steps.first()
        votes shouldBe RevealStep.VotesShown(revealed.summary.votes)
    }

    @Test
    fun `a sold vote is visible to the buyer, not the debtor`() {
        var state = startedGame().asDebtor(P5).advanceTo(Phase.Negotiation)
            .apply(ContractPropose(P1, ContractTerms.VoteSale(debtor = P5, buyer = P1, price = 3)))
        state = state.apply(ContractRespond(P5, state.contracts.last().id, accept = true))
            .advanceTo(Phase.Vote)
            .apply(VoteSelect(P1, NO, forPlayer = P5))

        val debtor = checkNotNull(state.toPrivateView(P5))
        debtor.ballot shouldBe null
        debtor.voteSoldTo shouldBe P1
        checkNotNull(state.toPrivateView(P1)).controlledBallots shouldBe mapOf(P5 to Ballot(selection = NO))
        state.toPrivateView(P2)?.controlledBallots shouldBe emptyMap()
    }

    @Test
    fun `contract payments and promise verdicts stay private after REVEAL`() {
        var state = startedGame().advanceTo(Phase.Negotiation)
            .apply(MakePromise(P2, ToPlayer(P3), YES))
            .apply(ContractPropose(P2, FixedPayment(P2, P3, 10, ALWAYS)))
        state = state.apply(ContractRespond(P3, state.contracts.last().id, accept = true))
            .playVote(P1 to YES, P2 to NO, P3 to NO)

        checkNotNull(state.toPrivateView(P1)).transfers shouldBe emptyList()
        checkNotNull(state.toPrivateView(P1)).judgedPromises shouldBe emptyList()
        checkNotNull(state.toPrivateView(P3)).transfers.single().paid shouldBe 10
        checkNotNull(state.toPrivateView(P3)).judgedPromises.single().verdict shouldBe PromiseVerdict.BROKEN
        // The stamp is public; the promise behind it is not.
        state.toPublicView().players.single { it.id == P2 }.brokenPromises shouldBe 1
    }

    @Test
    fun `everything is revealed at GAME_OVER`() {
        val state = startedGame(config = GameConfig(rounds = 1)).advanceTo(Phase.Negotiation)
            .apply(MakePromise(P2, ToPlayer(P3), YES))
            .playVote(P2 to NO)
            .advanceTo(Phase.GameOver)

        val history = state.toPublicView().finalHistory.shouldNotBeNull()
        history.single().promises.single().promise shouldBe Promise(P2, ToPlayer(P3), YES)
    }

    @Test
    fun `unknown players get no private view`() {
        startedGame().toPrivateView(PlayerId("ghost")) shouldBe null
    }
}
