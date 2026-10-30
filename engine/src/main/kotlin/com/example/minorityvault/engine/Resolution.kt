package com.example.minorityvault.engine

internal data class Resolution(val players: List<Player>, val pot: Int, val summary: RoundSummary)

/** Resolution steps 1–7 from CLAUDE.md §2, plus promise verdicts. Pure: the same input gives the same output. */
internal fun resolveRound(config: GameConfig, players: List<Player>, pot: Int, round: RoundState): Resolution {
    val twist = round.twist ?: Twist.NoTwist
    // 1–2. Count, find the minority.
    val votes = players.associate { it.id to (round.votes[it.id] ?: Vote.ABSTAIN) }
    val totals = votes.values.totals()
    val minoritySide = minoritySideOf(totals)
    val minority = minoritySide?.let { side -> votes.filterValues { it.choice == side.choice }.keys.toList() }
    // 3–5. Fines, then the twist decides who splits the pot.
    val ledger = Ledger(players, pot)
    val fines = collectFines(config, round.number, players, minority, ledger)
    val winners = twist.payoutWinners(minority, ledger.balances())
    val share = if (winners.isEmpty()) 0 else ledger.pot / winners.size
    winners.forEach { ledger.payFromPot(it, share) }
    val winnings = winners.associateWith { share }
    // 6. Contracts, in creation order.
    val contracts = round.signedContracts.sortedBy { it.id.value }
    val transfers = contracts.mapNotNull { executeContract(it, winnings, ledger) }
    // 7. Debtors, and promise verdicts.
    val promises = judgePromises(round, votes)
    val settled = players.map { player ->
        val balance = ledger.balance(player.id)
        val broken = promises.count { it.promise.from == player.id && it.verdict == PromiseVerdict.BROKEN }
        player.copy(balance = balance, isDebtor = balance == 0, brokenPromises = player.brokenPromises + broken)
    }
    val changes = players.zip(settled)
    val newDebtors = changes.filter { (before, after) -> !before.isDebtor && after.isDebtor }.map { it.second.id }
    val recovered = changes.filter { (before, after) -> before.isDebtor && !after.isDebtor }.map { it.second.id }

    val summary = RoundSummary(
        round = round.number,
        questionId = round.questionId,
        twist = twist,
        votes = votes,
        totals = totals,
        minoritySide = minoritySide,
        minorityPlayers = minority.orEmpty(),
        winners = winners,
        fines = fines,
        winnings = winnings,
        contracts = contracts,
        transfers = transfers,
        promises = promises,
        newDebtors = newDebtors,
        recoveredDebtors = recovered,
        potBefore = pot,
        potAfter = ledger.pot,
        balancesAfter = ledger.balances(),
    )
    return Resolution(settled, ledger.pot, summary)
}

/** The lighter side, or null on a tie or when either side is empty. */
internal fun minoritySideOf(totals: VoteTotals): Side? = when {
    totals.yes == totals.no || totals.yes == 0 || totals.no == 0 -> null
    totals.yes < totals.no -> Side.YES
    else -> Side.NO
}

/** Everyone outside the minority pays (everyone, if there is none). Returns what each actually paid. */
private fun collectFines(
    config: GameConfig,
    round: Int,
    players: List<Player>,
    minority: List<PlayerId>?,
    ledger: Ledger,
): Map<PlayerId, Int> = buildMap {
    for (player in players) {
        if (minority == null || player.id !in minority) {
            put(player.id, ledger.payIntoPot(player.id, config.fineFor(round, ledger.balance(player.id))))
        }
    }
}

/** Returns the transfer if the contract triggered, or null if its condition wasn't met. */
private fun executeContract(contract: Contract, winnings: Map<PlayerId, Int>, ledger: Ledger): ContractTransfer? {
    fun wins(player: PlayerId) = (winnings[player] ?: 0) > 0
    val (from, to, owed) = when (val terms = contract.terms) {
        is ContractTerms.ShareWinnings -> Triple(
            terms.from,
            terms.to,
            (winnings[terms.from] ?: 0) * terms.percent / PERCENT,
        )

        is ContractTerms.FixedPayment -> {
            val triggered = when (terms.condition) {
                PaymentCondition.IF_FROM_WINS -> wins(terms.from)
                PaymentCondition.IF_TO_WINS -> wins(terms.to)
                PaymentCondition.ALWAYS -> true
            }
            Triple(terms.from, terms.to, if (triggered) terms.amount else 0)
        }

        is ContractTerms.VoteSale -> Triple(terms.buyer, terms.debtor, terms.price)
    }
    return if (owed > 0) ContractTransfer(contract.id, from, to, owed, ledger.transfer(from, to, owed)) else null
}

/** Kept if the final vote matches, broken otherwise (abstaining included), void if the vote was sold. */
private fun judgePromises(round: RoundState, votes: Map<PlayerId, Vote>): List<JudgedPromise> =
    round.promises.map { promise ->
        val verdict = when {
            round.voteSaleFor(promise.from) != null -> PromiseVerdict.VOID
            votes[promise.from]?.choice == promise.side.choice -> PromiseVerdict.KEPT
            else -> PromiseVerdict.BROKEN
        }
        JudgedPromise(promise, verdict)
    }
