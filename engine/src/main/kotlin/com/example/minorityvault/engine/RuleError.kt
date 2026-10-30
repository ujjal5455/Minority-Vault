package com.example.minorityvault.engine

/** Why an intent was refused. The engine returns these instead of throwing. */
sealed interface RuleError {
    data class WrongPhase(val phase: Phase) : RuleError

    /** A timer or advance for [expected] arrived after the game had moved on to [actual]. */
    data class StalePhase(val expected: Phase, val actual: Phase) : RuleError

    data class UnknownPlayer(val player: PlayerId) : RuleError

    data class InvalidSetup(val problem: SetupProblem) : RuleError

    data object TwistNotOffered : RuleError

    data object VoteIsLocked : RuleError

    data object NotLocked : RuleError

    data object NothingSelected : RuleError

    /** The player sold this round's vote, so only the buyer may cast it. */
    data object VoteSold : RuleError

    /** The player tried to cast someone else's vote without having bought it. */
    data object NotYourVote : RuleError

    data object AlreadyFlipped : RuleError

    data object CannotFlipAbstain : RuleError

    data class NoItemLeft(val item: Item) : RuleError

    data class ItemAlreadyApplied(val item: Item) : RuleError

    data class InvalidContract(val problem: ContractProblem) : RuleError

    data class ContractNotFound(val contractId: ContractId) : RuleError

    data class ContractNotPending(val contractId: ContractId) : RuleError

    data object NotCounterparty : RuleError

    data class CannotAffordFee(val player: PlayerId) : RuleError

    data object PromiseToSelf : RuleError
}

enum class SetupProblem { PLAYER_COUNT, DUPLICATE_PLAYER, NOT_ENOUGH_QUESTIONS }

enum class ContractProblem {
    SAME_PLAYER,
    NOT_A_PARTY,
    PERCENT_OUT_OF_RANGE,
    AMOUNT_NOT_POSITIVE,
    NOT_A_DEBTOR,
    DEBTOR_ALREADY_SOLD,
}
