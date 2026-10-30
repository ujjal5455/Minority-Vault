package com.example.minorityvault.engine

import com.example.minorityvault.engine.PromiseTarget.Everyone
import com.example.minorityvault.engine.PromiseTarget.ToPlayer
import com.example.minorityvault.engine.PromiseVerdict.BROKEN
import com.example.minorityvault.engine.PromiseVerdict.KEPT
import com.example.minorityvault.engine.PromiseVerdict.VOID
import com.example.minorityvault.engine.Side.NO
import com.example.minorityvault.engine.Side.YES
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PromiseTest {
    private val negotiating = startedGame().advanceTo(Phase.Negotiation)

    private fun GameState.promise(from: PlayerId, to: PromiseTarget, side: Side) = apply(MakePromise(from, to, side))

    private fun RoundSummary.verdictOf(from: PlayerId) = promises.single { it.promise.from == from }.verdict

    @Test
    fun `promises are judged against the final vote, and abstaining breaks them`() {
        val promised = negotiating
            .promise(P1, ToPlayer(P2), YES)
            .promise(P3, ToPlayer(P2), NO)
            .promise(P4, Everyone, YES)
            .advanceTo(Phase.Vote)
            .vote(P1, YES).vote(P2, NO).vote(P3, YES).vote(P5, NO)

        val events = promised.events(AdvancePhase(Phase.Vote))
        val state = promised.advance()

        state.summary.verdictOf(P1) shouldBe KEPT
        state.summary.verdictOf(P3) shouldBe BROKEN
        state.summary.verdictOf(P4) shouldBe BROKEN
        state.players.map { it.brokenPromises } shouldBe listOf(0, 0, 1, 1, 0)
        events shouldContainAll listOf(PublicEvent.PromiseBroken(P3), PublicEvent.PromiseBroken(P4))
    }

    @Test
    fun `only public promises are announced`() {
        negotiating.events(MakePromise(P1, Everyone, YES)) shouldBe listOf(PublicEvent.PublicPromiseMade(P1, YES))
        negotiating.events(MakePromise(P1, ToPlayer(P2), YES)) shouldBe emptyList()
    }

    @Test
    fun `invalid promises are rejected`() {
        negotiating.rejection(MakePromise(P1, ToPlayer(P1), YES)) shouldBe RuleError.PromiseToSelf
        negotiating.rejection(MakePromise(P1, ToPlayer(PlayerId("ghost")), YES)) shouldBe
            RuleError.UnknownPlayer(PlayerId("ghost"))
        startedGame().rejection(MakePromise(P1, Everyone, YES)) shouldBe RuleError.WrongPhase(Phase.Question)
    }

    @Test
    fun `a new promise to the same recipient replaces the old one`() {
        val state = negotiating.promise(P1, ToPlayer(P2), YES).promise(P1, ToPlayer(P2), NO)

        checkNotNull(state.round).promises shouldBe listOf(Promise(P1, ToPlayer(P2), NO))
    }

    @Test
    fun `double-dealing breaks one of the two promises`() {
        val state = negotiating
            .promise(P1, ToPlayer(P2), YES)
            .promise(P1, ToPlayer(P3), NO)
            .playVote(P1 to YES)

        state.summary.promises.map { it.verdict } shouldBe listOf(KEPT, BROKEN)
        checkNotNull(state.player(P1)).brokenPromises shouldBe 1
    }

    @Test
    fun `a promise is void when the promiser's vote was sold`() {
        var state = negotiating.asDebtor(P5)
            .promise(P5, ToPlayer(P2), YES)
            .apply(ContractPropose(P1, ContractTerms.VoteSale(debtor = P5, buyer = P1, price = 3)))
        state = state.apply(ContractRespond(P5, state.contracts.last().id, accept = true))
            .advanceTo(Phase.Vote)
            .apply(VoteSelect(P1, NO, forPlayer = P5))
            .apply(VoteLock(P1, forPlayer = P5))
            .advance()

        state.summary.verdictOf(P5) shouldBe VOID
        checkNotNull(state.player(P5)).brokenPromises shouldBe 0
    }

    @Test
    fun `flipping in the second chance window can break a promise`() {
        val state = startedGame(twist = Twist.SecondChance)
            .advanceTo(Phase.Negotiation)
            .promise(P1, ToPlayer(P2), YES)
            .playVote(P1 to YES, P2 to NO)
            .apply(VoteFlip(P1))
            .advance()

        state.summary.verdictOf(P1) shouldBe BROKEN
    }
}
