package com.example.minorityvault.engine.sim

import com.example.minorityvault.engine.Ballot
import com.example.minorityvault.engine.ContractPropose
import com.example.minorityvault.engine.ContractRespond
import com.example.minorityvault.engine.ContractStatus
import com.example.minorityvault.engine.ContractTerms
import com.example.minorityvault.engine.Item
import com.example.minorityvault.engine.MakePromise
import com.example.minorityvault.engine.PaymentCondition
import com.example.minorityvault.engine.Phase
import com.example.minorityvault.engine.PlayerId
import com.example.minorityvault.engine.PlayerIntent
import com.example.minorityvault.engine.PrivatePlayerView
import com.example.minorityvault.engine.PromiseTarget
import com.example.minorityvault.engine.PublicGameView
import com.example.minorityvault.engine.SeededRandom
import com.example.minorityvault.engine.Side
import com.example.minorityvault.engine.UseItem
import com.example.minorityvault.engine.Vote
import com.example.minorityvault.engine.VoteChoice
import com.example.minorityvault.engine.VoteFlip
import com.example.minorityvault.engine.VoteLock
import com.example.minorityvault.engine.VoteSelect
import com.example.minorityvault.engine.VoteUnlock

/**
 * Random moves, some deliberately illegal, to stress the rules in property tests. Not a real bot: personalities
 * arrive in Phase 7.
 */
class RandomPolicy : PlayerPolicy {
    override fun decide(view: PublicGameView, me: PrivatePlayerView, random: SeededRandom): List<PlayerIntent> =
        when (view.phase) {
            Phase.Negotiation -> negotiate(view, me, random)
            Phase.Vote -> vote(me, random)
            Phase.FlipWindow -> flip(me, random)
            else -> emptyList()
        }

    private fun negotiate(view: PublicGameView, me: PrivatePlayerView, random: SeededRandom) = buildList {
        val others = view.players.map { it.id } - me.playerId
        if (random.chance(PROMISE_CHANCE)) {
            val target = if (random.chance(PUBLIC_PROMISE_CHANCE)) {
                PromiseTarget.Everyone
            } else {
                PromiseTarget.ToPlayer(random.pick(others))
            }
            add(MakePromise(me.playerId, target, random.pick(Side.entries)))
        }
        if (random.chance(
                PROPOSE_CHANCE,
            )
        ) {
            add(ContractPropose(me.playerId, randomTerms(view, me.playerId, others, random)))
        }
        me.contracts
            .filter { it.status == ContractStatus.PENDING && it.counterparty == me.playerId }
            .forEach { add(ContractRespond(me.playerId, it.id, accept = random.chance(ACCEPT_CHANCE))) }
    }

    private fun vote(me: PrivatePlayerView, random: SeededRandom) = buildList {
        me.ballot?.let { addAll(ballotMoves(me.playerId, null, it, me.items[Item.DOUBLE_VOTE] ?: 0, random)) }
        me.controlledBallots.forEach { (debtor, ballot) -> addAll(ballotMoves(me.playerId, debtor, ballot, 0, random)) }
    }

    private fun ballotMoves(
        actor: PlayerId,
        target: PlayerId?,
        ballot: Ballot,
        doubleVotes: Int,
        random: SeededRandom,
    ): List<PlayerIntent> = buildList {
        if (ballot.locked) {
            if (random.chance(UNLOCK_CHANCE)) add(VoteUnlock(actor, target))
        } else {
            add(VoteSelect(actor, random.pick(Side.entries), target))
            if (doubleVotes > 0 && random.chance(DOUBLE_CHANCE)) add(UseItem(actor, Item.DOUBLE_VOTE))
            if (random.chance(LOCK_CHANCE)) add(VoteLock(actor, target))
        }
    }

    private fun flip(me: PrivatePlayerView, random: SeededRandom) = buildList {
        me.vote?.let { if (wantsFlip(it, random)) add(VoteFlip(me.playerId)) }
        me.controlledVotes.forEach { (debtor, vote) -> if (wantsFlip(vote, random)) add(VoteFlip(me.playerId, debtor)) }
    }

    // Sometimes flips abstentions or flips twice, to exercise the rejections.
    private fun wantsFlip(vote: Vote, random: SeededRandom): Boolean =
        random.chance(if (vote.choice == VoteChoice.ABSTAIN || vote.flipped) ILLEGAL_MOVE_CHANCE else FLIP_CHANCE)

    private fun randomTerms(
        view: PublicGameView,
        me: PlayerId,
        others: List<PlayerId>,
        random: SeededRandom,
    ): ContractTerms {
        val other = random.pick(others)
        val (from, to) = if (random.chance(HALF)) me to other else other to me
        return when (random.nextInt(TEMPLATE_COUNT)) {
            0 -> ContractTerms.ShareWinnings(from, to, percent = random.nextInt(MAX_PERCENT_TRIED))

            1 -> ContractTerms.FixedPayment(
                from,
                to,
                amount = random.nextInt(MAX_AMOUNT_TRIED),
                condition = random.pick(PaymentCondition.entries),
            )

            else -> voteSaleTerms(view, me, other, random)
        }
    }

    private fun voteSaleTerms(
        view: PublicGameView,
        me: PlayerId,
        other: PlayerId,
        random: SeededRandom,
    ): ContractTerms {
        val price = 1 + random.nextInt(MAX_PRICE)
        val iAmDebtor = view.players.first { it.id == me }.isDebtor
        val debtors = view.players.filter { it.isDebtor && it.id != me }.map { it.id }
        return when {
            iAmDebtor -> ContractTerms.VoteSale(debtor = me, buyer = other, price = price)

            debtors.isNotEmpty() -> ContractTerms.VoteSale(debtor = random.pick(debtors), buyer = me, price = price)

            // Usually illegal (the other player isn't a Debtor); kept to exercise the rejection.
            else -> ContractTerms.VoteSale(debtor = other, buyer = me, price = price)
        }
    }

    private companion object {
        const val PROMISE_CHANCE = 35
        const val PUBLIC_PROMISE_CHANCE = 30
        const val PROPOSE_CHANCE = 35
        const val ACCEPT_CHANCE = 60
        const val UNLOCK_CHANCE = 10
        const val DOUBLE_CHANCE = 20
        const val LOCK_CHANCE = 85
        const val FLIP_CHANCE = 30
        const val ILLEGAL_MOVE_CHANCE = 5
        const val HALF = 50
        const val TEMPLATE_COUNT = 3
        const val MAX_PERCENT_TRIED = 110
        const val MAX_AMOUNT_TRIED = 40
        const val MAX_PRICE = 15
    }
}
