package com.example.minorityvault.engine

/**
 * Everything that happened when a round resolved. The host keeps all of it; clients get [reveal] publicly and
 * only their own [transfers] and [promises] privately (see [toPrivateView]).
 */
data class RoundSummary(
    val round: Int,
    val questionId: QuestionId,
    val twist: Twist,
    val votes: Map<PlayerId, Vote>,
    val totals: VoteTotals,
    val minoritySide: Side?,
    val minorityPlayers: List<PlayerId>,
    val winners: List<PlayerId>,
    val fines: Map<PlayerId, Int>,
    val winnings: Map<PlayerId, Int>,
    val contracts: List<Contract>,
    val transfers: List<ContractTransfer>,
    val promises: List<JudgedPromise>,
    val newDebtors: List<PlayerId>,
    val recoveredDebtors: List<PlayerId>,
    val potBefore: Int,
    /** Contracts move coins between players, never into or out of the pot, so this is also what rolls over. */
    val potAfter: Int,
    /** Balances as the round reached REVEAL (after any signing fees), so clients can count from them. */
    val balancesBefore: Map<PlayerId, Int>,
    val balancesAfter: Map<PlayerId, Int>,
) {
    /** The public part of this summary, as an ordered story. */
    val reveal: RevealSequence
        get() = RevealSequence(
            buildList {
                add(RevealStep.VotesShown(votes))
                add(RevealStep.TotalsShown(totals))
                add(RevealStep.MinorityFound(minoritySide, minorityPlayers))
                add(RevealStep.TwistResolved(twist.publicInfo(), winners))
                add(RevealStep.FinesPaid(fines))
                add(RevealStep.PotSplit(winners, share = winnings.values.firstOrNull() ?: 0, potLeft = potAfter))
                val breakers = promises.filter { it.verdict == PromiseVerdict.BROKEN }.map { it.promise.from }
                if (breakers.isNotEmpty()) add(RevealStep.PromisesBroken(breakers))
                if (newDebtors.isNotEmpty() || recoveredDebtors.isNotEmpty()) {
                    add(RevealStep.DebtorsChanged(newDebtors, recoveredDebtors))
                }
                add(RevealStep.Settled(balancesAfter, potAfter, balancesBefore))
            },
        )
}

/** The public, ordered story of a reveal, for animations and Keeper narration. */
data class RevealSequence(val steps: List<RevealStep>)

sealed interface RevealStep {
    data class VotesShown(val votes: Map<PlayerId, Vote>) : RevealStep

    data class TotalsShown(val totals: VoteTotals) : RevealStep

    /** [side] is null when there is no minority (tie or an empty side). */
    data class MinorityFound(val side: Side?, val players: List<PlayerId>) : RevealStep

    data class TwistResolved(val twist: TwistInfo, val winners: List<PlayerId>) : RevealStep

    data class FinesPaid(val fines: Map<PlayerId, Int>) : RevealStep

    /** [potLeft] rolls over to the next round. */
    data class PotSplit(val winners: List<PlayerId>, val share: Int, val potLeft: Int) : RevealStep

    /** One entry per broken promise, so a double-dealer can appear twice. */
    data class PromisesBroken(val players: List<PlayerId>) : RevealStep

    data class DebtorsChanged(val newDebtors: List<PlayerId>, val recovered: List<PlayerId>) : RevealStep

    /** [balances] after the round; [previous] as it started, so balances can count from one to the other. */
    data class Settled(val balances: Map<PlayerId, Int>, val pot: Int, val previous: Map<PlayerId, Int>) : RevealStep
}
