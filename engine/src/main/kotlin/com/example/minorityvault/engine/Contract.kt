package com.example.minorityvault.engine

enum class PaymentCondition { IF_FROM_WINS, IF_TO_WINS, ALWAYS }

/** The contract templates from CLAUDE.md §2. A contract is never free text. */
sealed interface ContractTerms {
    val parties: Set<PlayerId>

    /** Who pays the signing fee, or null for whoever signs. */
    val feePayer: PlayerId? get() = null

    data class ShareWinnings(val from: PlayerId, val to: PlayerId, val percent: Int) : ContractTerms {
        override val parties: Set<PlayerId> get() = setOf(from, to)
    }

    data class FixedPayment(val from: PlayerId, val to: PlayerId, val amount: Int, val condition: PaymentCondition) :
        ContractTerms {
        override val parties: Set<PlayerId> get() = setOf(from, to)
    }

    data class VoteSale(val debtor: PlayerId, val buyer: PlayerId, val price: Int) : ContractTerms {
        override val parties: Set<PlayerId> get() = setOf(debtor, buyer)
        override val feePayer: PlayerId get() = buyer
    }
}

enum class ContractStatus { PENDING, SIGNED, DECLINED, EXPIRED }

data class Contract(
    val id: ContractId,
    val proposer: PlayerId,
    val terms: ContractTerms,
    val status: ContractStatus = ContractStatus.PENDING,
) {
    /** The party who may sign or decline. */
    val counterparty: PlayerId get() = (terms.parties - proposer).single()
}

/** A contract payment made at settlement. [paid] is less than [owed] when the payer ran out of coins. */
data class ContractTransfer(
    val contractId: ContractId,
    val from: PlayerId,
    val to: PlayerId,
    val owed: Int,
    val paid: Int,
)

sealed interface PromiseTarget {
    data class ToPlayer(val id: PlayerId) : PromiseTarget

    data object Everyone : PromiseTarget
}

/** "I will vote [side] this round." Judged at REVEAL, never enforced. */
data class Promise(val from: PlayerId, val to: PromiseTarget, val side: Side) {
    internal fun involves(player: PlayerId): Boolean = from == player || to == PromiseTarget.ToPlayer(player)
}

enum class PromiseVerdict { KEPT, BROKEN, VOID }

data class JudgedPromise(val promise: Promise, val verdict: PromiseVerdict)
