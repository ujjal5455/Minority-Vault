package com.example.minorityvault.engine

import com.example.minorityvault.engine.ContractTerms.FixedPayment
import com.example.minorityvault.engine.ContractTerms.ShareWinnings
import com.example.minorityvault.engine.ContractTerms.VoteSale

internal fun proposeContract(state: GameState, intent: ContractPropose): ReduceResult {
    val round = state.round
    val error = if (state.phase != Phase.Negotiation || round == null) {
        RuleError.WrongPhase(state.phase)
    } else {
        termsError(state, round, intent.player, intent.terms)
    }
    return error.orApply {
        val contract = Contract(ContractId(state.nextContractId), intent.player, intent.terms)
        ReduceResult.Applied(
            state.copy(nextContractId = state.nextContractId + 1).withRound {
                it.copy(
                    contracts =
                    it.contracts + contract,
                )
            },
        )
    }
}

internal fun respondToContract(state: GameState, intent: ContractRespond): ReduceResult {
    val round = state.round
    val contract = round?.contracts?.firstOrNull { it.id == intent.contractId }
    val error = when {
        state.phase != Phase.Negotiation || round == null -> RuleError.WrongPhase(state.phase)

        contract == null -> RuleError.ContractNotFound(intent.contractId)

        contract.status != ContractStatus.PENDING -> RuleError.ContractNotPending(contract.id)

        contract.counterparty != intent.player -> RuleError.NotCounterparty

        !intent.accept -> null

        // Re-check the terms: debtor status or another sale may have changed since the proposal.
        else -> termsError(state, round, contract.proposer, contract.terms) ?: feeError(state, contract)
    }
    return error.orApply {
        val pending = checkNotNull(contract)
        if (intent.accept) {
            sign(
                state,
                pending,
            )
        } else {
            ReduceResult.Applied(state.withContract(pending.copy(status = ContractStatus.DECLINED)))
        }
    }
}

/** A promise only the speaker can make. A new one to the same recipient replaces the old one. */
internal fun makePromise(state: GameState, intent: MakePromise): ReduceResult {
    val target = intent.to
    val error = when {
        state.phase != Phase.Negotiation -> RuleError.WrongPhase(state.phase)
        target is PromiseTarget.ToPlayer && target.id == intent.player -> RuleError.PromiseToSelf
        target is PromiseTarget.ToPlayer && state.player(target.id) == null -> RuleError.UnknownPlayer(target.id)
        else -> null
    }
    return error.orApply {
        val promise = Promise(intent.player, target, intent.side)
        val next = state.withRound { round ->
            round.copy(promises = round.promises.filterNot { it.from == promise.from && it.to == promise.to } + promise)
        }
        val events = if (target == PromiseTarget.Everyone) {
            listOf(PublicEvent.PublicPromiseMade(intent.player, intent.side))
        } else {
            emptyList()
        }
        ReduceResult.Applied(next, events)
    }
}

private fun sign(state: GameState, contract: Contract): ReduceResult {
    val ledger = Ledger(state.players, state.pot)
    ledger.payIntoPot(feePayer(contract), state.config.contractFee)
    val next = state.copy(players = ledger.applyTo(state.players), pot = ledger.pot)
        .withContract(contract.copy(status = ContractStatus.SIGNED))
    return ReduceResult.Applied(next, listOf(PublicEvent.ContractSigned))
}

private fun feePayer(contract: Contract): PlayerId = contract.terms.feePayer ?: contract.counterparty

private fun feeError(state: GameState, contract: Contract): RuleError? {
    val payer = feePayer(contract)
    val balance = state.player(payer)?.balance ?: 0
    return if (balance < state.config.contractFee) RuleError.CannotAffordFee(payer) else null
}

private fun termsError(state: GameState, round: RoundState, proposer: PlayerId, terms: ContractTerms): RuleError? {
    val unknown = terms.parties.firstOrNull { state.player(it) == null }
    return when {
        terms.parties.size != 2 -> RuleError.InvalidContract(ContractProblem.SAME_PLAYER)
        unknown != null -> RuleError.UnknownPlayer(unknown)
        proposer !in terms.parties -> RuleError.InvalidContract(ContractProblem.NOT_A_PARTY)
        else -> templateProblem(state, round, terms)?.let { RuleError.InvalidContract(it) }
    }
}

private fun templateProblem(state: GameState, round: RoundState, terms: ContractTerms): ContractProblem? =
    when (terms) {
        is ShareWinnings -> if (terms.percent in 1..PERCENT) null else ContractProblem.PERCENT_OUT_OF_RANGE

        is FixedPayment -> if (terms.amount >= 1) null else ContractProblem.AMOUNT_NOT_POSITIVE

        is VoteSale -> when {
            terms.price < 1 -> ContractProblem.AMOUNT_NOT_POSITIVE
            state.player(terms.debtor)?.isDebtor != true -> ContractProblem.NOT_A_DEBTOR
            round.voteSaleFor(terms.debtor) != null -> ContractProblem.DEBTOR_ALREADY_SOLD
            else -> null
        }
    }

private fun GameState.withContract(updated: Contract): GameState = withRound { round ->
    round.copy(contracts = round.contracts.map { if (it.id == updated.id) updated else it })
}
