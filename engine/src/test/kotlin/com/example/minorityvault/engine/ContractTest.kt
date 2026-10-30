package com.example.minorityvault.engine

import com.example.minorityvault.engine.ContractTerms.FixedPayment
import com.example.minorityvault.engine.ContractTerms.ShareWinnings
import com.example.minorityvault.engine.ContractTerms.VoteSale
import com.example.minorityvault.engine.PaymentCondition.ALWAYS
import com.example.minorityvault.engine.PaymentCondition.IF_FROM_WINS
import com.example.minorityvault.engine.PaymentCondition.IF_TO_WINS
import com.example.minorityvault.engine.Side.NO
import com.example.minorityvault.engine.Side.YES
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

// Five players with 100 coins each; round 1 fine is 10; the signing fee is 5.
class ContractTest {
    private val negotiating = startedGame().advanceTo(Phase.Negotiation)

    private fun GameState.propose(proposer: PlayerId, terms: ContractTerms) = apply(ContractPropose(proposer, terms))

    private fun GameState.sign(signer: PlayerId, id: ContractId) = apply(ContractRespond(signer, id, accept = true))

    private fun GameState.lastContractId() = contracts.last().id

    @Test
    fun `contracts can only be proposed during negotiation`() {
        startedGame().rejection(ContractPropose(P1, FixedPayment(P1, P2, 5, ALWAYS))) shouldBe
            RuleError.WrongPhase(Phase.Question)
    }

    @Test
    fun `invalid terms are rejected`() {
        fun problem(terms: ContractTerms) = negotiating.rejection(ContractPropose(P1, terms))

        problem(ShareWinnings(P2, P3, 50)) shouldBe RuleError.InvalidContract(ContractProblem.NOT_A_PARTY)
        problem(ShareWinnings(P1, P1, 50)) shouldBe RuleError.InvalidContract(ContractProblem.SAME_PLAYER)
        problem(ShareWinnings(P1, P2, 0)) shouldBe RuleError.InvalidContract(ContractProblem.PERCENT_OUT_OF_RANGE)
        problem(ShareWinnings(P1, P2, 101)) shouldBe RuleError.InvalidContract(ContractProblem.PERCENT_OUT_OF_RANGE)
        problem(FixedPayment(P1, P2, 0, ALWAYS)) shouldBe RuleError.InvalidContract(ContractProblem.AMOUNT_NOT_POSITIVE)
        problem(VoteSale(P2, P1, 5)) shouldBe RuleError.InvalidContract(ContractProblem.NOT_A_DEBTOR)
        problem(FixedPayment(P1, PlayerId("ghost"), 5, ALWAYS)) shouldBe RuleError.UnknownPlayer(PlayerId("ghost"))
    }

    @Test
    fun `only the counterparty can respond, and only once`() {
        val proposed = negotiating.propose(P1, FixedPayment(P1, P2, 5, ALWAYS))
        val id = proposed.lastContractId()

        proposed.rejection(ContractRespond(P3, id, accept = true)) shouldBe RuleError.NotCounterparty
        proposed.rejection(ContractRespond(P1, id, accept = true)) shouldBe RuleError.NotCounterparty
        proposed.rejection(ContractRespond(P2, ContractId(99), accept = true)) shouldBe
            RuleError.ContractNotFound(ContractId(99))
        proposed.sign(P2, id).rejection(ContractRespond(P2, id, accept = true)) shouldBe
            RuleError.ContractNotPending(id)
    }

    @Test
    fun `the signer pays the fee into the pot and the signing is announced anonymously`() {
        val proposed = negotiating.propose(P1, FixedPayment(P1, P2, 5, ALWAYS))
        val id = proposed.lastContractId()

        val events = proposed.events(ContractRespond(P2, id, accept = true))
        val signed = proposed.sign(P2, id)

        signed.balance(P2) shouldBe 95
        signed.pot shouldBe 5
        signed.contracts.single().status shouldBe ContractStatus.SIGNED
        events shouldBe listOf(PublicEvent.ContractSigned)
        signed.totalCoins shouldBe negotiating.totalCoins
    }

    @Test
    fun `declining costs nothing`() {
        val proposed = negotiating.propose(P1, FixedPayment(P1, P2, 5, ALWAYS))
        val declined = proposed.apply(ContractRespond(P2, proposed.lastContractId(), accept = false))

        declined.contracts.single().status shouldBe ContractStatus.DECLINED
        declined.balance(P2) shouldBe 100
        declined.pot shouldBe 0
    }

    @Test
    fun `a signer who can't afford the fee can't sign`() {
        val proposed = negotiating.withBalances(100, 4, 100, 100, 100).propose(P1, FixedPayment(P1, P2, 5, ALWAYS))

        proposed.rejection(ContractRespond(P2, proposed.lastContractId(), accept = true)) shouldBe
            RuleError.CannotAffordFee(P2)
    }

    @Test
    fun `unsigned proposals expire when voting starts`() {
        val proposed = negotiating.propose(P1, FixedPayment(P1, P2, 5, ALWAYS))
        val voting = proposed.advance()

        voting.contracts.single().status shouldBe ContractStatus.EXPIRED
        voting.rejection(ContractRespond(P2, proposed.lastContractId(), accept = true)) shouldBe
            RuleError.WrongPhase(Phase.Vote)
    }

    @Test
    fun `ShareWinnings passes on a percentage of winnings, rounded down`() {
        val proposed = negotiating.propose(P1, ShareWinnings(from = P1, to = P4, percent = 50))
        val state = proposed.sign(P4, proposed.lastContractId())
            .playVote(P1 to YES, P2 to YES, P3 to NO, P4 to NO, P5 to NO)

        // Pot: 5 fee + 30 fines = 35, so 17 each with 1 left over. Half of 17 is 8.
        state.summary.winnings.getValue(P1) shouldBe 17
        state.summary.transfers.single() shouldBe ContractTransfer(ContractId(1), P1, P4, owed = 8, paid = 8)
        state.balance(P1) shouldBe 109
        state.balance(P4) shouldBe 93
    }

    @Test
    fun `FixedPayment triggers only when its condition holds`() {
        var state = negotiating
        state = state.propose(P1, FixedPayment(P3, P1, 5, IF_FROM_WINS)).let { it.sign(P3, it.lastContractId()) }
        state = state.propose(P1, FixedPayment(P3, P1, 6, IF_TO_WINS)).let { it.sign(P3, it.lastContractId()) }
        state = state.propose(P4, FixedPayment(P4, P5, 7, ALWAYS)).let { it.sign(P5, it.lastContractId()) }

        state = state.playVote(P1 to YES, P2 to YES, P3 to NO, P4 to NO, P5 to NO)

        // Pot: 15 fees + 30 fines = 45, so 22 each. P3 lost, so only IF_TO_WINS and ALWAYS trigger.
        state.summary.transfers.map { it.from to it.paid } shouldBe listOf(P3 to 6, P4 to 7)
        state.balances() shouldBe listOf(128, 122, 74, 83, 92)
        state.pot shouldBe 1
    }

    @Test
    fun `contracts run in creation order, not signing order, each capped at the payer's balance`() {
        var state = negotiating.withBalances(100, 100, 100, 30, 100)
        state = state.propose(P5, FixedPayment(from = P4, to = P5, amount = 50, condition = ALWAYS))
        val first = state.lastContractId()
        state = state.propose(P4, FixedPayment(from = P5, to = P4, amount = 40, condition = ALWAYS))
        val second = state.lastContractId()
        state = state.sign(P5, second).sign(P4, first)

        state = state.playVote(P1 to YES, P2 to YES, P3 to NO, P4 to NO, P5 to NO)

        // P4: 30 - 5 fee - 10 fine = 15, pays 15 of the 50 owed, then receives 40.
        state.summary.transfers shouldBe listOf(
            ContractTransfer(first, P4, P5, owed = 50, paid = 15),
            ContractTransfer(second, P5, P4, owed = 40, paid = 40),
        )
        state.balance(P4) shouldBe 40
        state.balance(P5) shouldBe 60
        checkNotNull(state.player(P4)).isDebtor shouldBe false
    }

    @Test
    fun `a VoteSale hands the debtor's vote to the buyer, who pays the fee and the price`() {
        var state = negotiating.asDebtor(P5)
        state = state.propose(P1, VoteSale(debtor = P5, buyer = P1, price = 10))
        state = state.sign(P5, state.lastContractId())

        state.balance(P1) shouldBe 95
        state.balance(P5) shouldBe 0

        state = state.advanceTo(Phase.Vote)
        state.rejection(VoteSelect(P5, YES)) shouldBe RuleError.VoteSold
        state.rejection(UseItem(P5, Item.DOUBLE_VOTE)) shouldBe RuleError.VoteSold
        state.rejection(VoteSelect(P2, YES, forPlayer = P5)) shouldBe RuleError.NotYourVote

        state = state.vote(P1, YES)
            .apply(VoteSelect(P1, YES, forPlayer = P5))
            .apply(VoteLock(P1, forPlayer = P5))
            .vote(P2, NO).vote(P3, NO).vote(P4, NO)
            .advance()

        // YES {P1, P5} beats NO 2 to 3. Pot: 5 fee + 30 fines = 35, so 17 each; then P1 pays P5 the price.
        state.summary.minorityPlayers shouldBe listOf(P1, P5)
        state.balance(P5) shouldBe 27
        state.balance(P1) shouldBe 102
        checkNotNull(state.player(P5)).isDebtor shouldBe false
    }

    @Test
    fun `a sold vote the buyer never locks is an abstention`() {
        var state = negotiating.asDebtor(P5).propose(P1, VoteSale(debtor = P5, buyer = P1, price = 10))
        state = state.sign(P5, state.lastContractId()).advanceTo(Phase.Vote)
            .apply(VoteSelect(P1, NO, forPlayer = P5))
            .advance()

        state.summary.votes.getValue(P5) shouldBe Vote.ABSTAIN
    }

    @Test
    fun `a debtor can sell their vote only once per round`() {
        var state = negotiating.asDebtor(P5)
        state = state.propose(P1, VoteSale(debtor = P5, buyer = P1, price = 10))
        val first = state.lastContractId()
        state = state.propose(P2, VoteSale(debtor = P5, buyer = P2, price = 12))
        val second = state.lastContractId()

        state.sign(P5, first).rejection(ContractRespond(P5, second, accept = true)) shouldBe
            RuleError.InvalidContract(ContractProblem.DEBTOR_ALREADY_SOLD)
    }
}
