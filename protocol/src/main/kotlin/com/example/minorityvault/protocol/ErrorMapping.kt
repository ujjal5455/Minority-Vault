package com.example.minorityvault.protocol

import com.example.minorityvault.engine.ContractId
import com.example.minorityvault.engine.ContractProblem
import com.example.minorityvault.engine.PlayerId
import com.example.minorityvault.engine.RuleError
import com.example.minorityvault.engine.SetupProblem

fun RuleError.toDto(): RuleErrorDto = when (this) {
    is RuleError.WrongPhase -> RuleErrorDto(RuleErrorCode.WRONG_PHASE, phase = phase.toWire())

    is RuleError.StalePhase ->
        RuleErrorDto(RuleErrorCode.STALE_PHASE, phase = actual.toWire(), expected = expected.toWire())

    is RuleError.UnknownPlayer -> RuleErrorDto(RuleErrorCode.UNKNOWN_PLAYER, player = player.value)

    is RuleError.InvalidSetup -> RuleErrorDto(RuleErrorCode.INVALID_SETUP, problem = problem.name)

    RuleError.TwistNotOffered -> RuleErrorDto(RuleErrorCode.TWIST_NOT_OFFERED)

    RuleError.VoteIsLocked -> RuleErrorDto(RuleErrorCode.VOTE_IS_LOCKED)

    RuleError.NotLocked -> RuleErrorDto(RuleErrorCode.NOT_LOCKED)

    RuleError.NothingSelected -> RuleErrorDto(RuleErrorCode.NOTHING_SELECTED)

    RuleError.VoteSold -> RuleErrorDto(RuleErrorCode.VOTE_SOLD)

    RuleError.NotYourVote -> RuleErrorDto(RuleErrorCode.NOT_YOUR_VOTE)

    RuleError.AlreadyFlipped -> RuleErrorDto(RuleErrorCode.ALREADY_FLIPPED)

    RuleError.CannotFlipAbstain -> RuleErrorDto(RuleErrorCode.CANNOT_FLIP_ABSTAIN)

    is RuleError.NoItemLeft -> RuleErrorDto(RuleErrorCode.NO_ITEM_LEFT, item = item)

    is RuleError.ItemAlreadyApplied -> RuleErrorDto(RuleErrorCode.ITEM_ALREADY_APPLIED, item = item)

    is RuleError.InvalidContract -> RuleErrorDto(RuleErrorCode.INVALID_CONTRACT, problem = problem.name)

    is RuleError.ContractNotFound -> RuleErrorDto(RuleErrorCode.CONTRACT_NOT_FOUND, contractId = contractId.value)

    is RuleError.ContractNotPending -> RuleErrorDto(RuleErrorCode.CONTRACT_NOT_PENDING, contractId = contractId.value)

    RuleError.NotCounterparty -> RuleErrorDto(RuleErrorCode.NOT_COUNTERPARTY)

    is RuleError.CannotAffordFee -> RuleErrorDto(RuleErrorCode.CANNOT_AFFORD_FEE, player = player.value)

    RuleError.PromiseToSelf -> RuleErrorDto(RuleErrorCode.PROMISE_TO_SELF)
}

/** The engine error, or null if a field it needs is missing (a newer host may know errors this build doesn't). */
fun RuleErrorDto.toEngine(): RuleError? = when (code) {
    RuleErrorCode.WRONG_PHASE -> phase?.let { RuleError.WrongPhase(it.toEngine()) }

    RuleErrorCode.STALE_PHASE -> if (expected != null && phase != null) {
        RuleError.StalePhase(expected.toEngine(), phase.toEngine())
    } else {
        null
    }

    RuleErrorCode.UNKNOWN_PLAYER -> player?.let { RuleError.UnknownPlayer(PlayerId(it)) }

    RuleErrorCode.INVALID_SETUP ->
        SetupProblem.entries.firstOrNull { it.name == problem }?.let(RuleError::InvalidSetup)

    RuleErrorCode.TWIST_NOT_OFFERED -> RuleError.TwistNotOffered

    RuleErrorCode.VOTE_IS_LOCKED -> RuleError.VoteIsLocked

    RuleErrorCode.NOT_LOCKED -> RuleError.NotLocked

    RuleErrorCode.NOTHING_SELECTED -> RuleError.NothingSelected

    RuleErrorCode.VOTE_SOLD -> RuleError.VoteSold

    RuleErrorCode.NOT_YOUR_VOTE -> RuleError.NotYourVote

    RuleErrorCode.ALREADY_FLIPPED -> RuleError.AlreadyFlipped

    RuleErrorCode.CANNOT_FLIP_ABSTAIN -> RuleError.CannotFlipAbstain

    RuleErrorCode.NO_ITEM_LEFT -> item?.let(RuleError::NoItemLeft)

    RuleErrorCode.ITEM_ALREADY_APPLIED -> item?.let(RuleError::ItemAlreadyApplied)

    RuleErrorCode.INVALID_CONTRACT ->
        ContractProblem.entries.firstOrNull { it.name == problem }?.let(RuleError::InvalidContract)

    RuleErrorCode.CONTRACT_NOT_FOUND -> contractId?.let { RuleError.ContractNotFound(ContractId(it)) }

    RuleErrorCode.CONTRACT_NOT_PENDING -> contractId?.let { RuleError.ContractNotPending(ContractId(it)) }

    RuleErrorCode.NOT_COUNTERPARTY -> RuleError.NotCounterparty

    RuleErrorCode.CANNOT_AFFORD_FEE -> player?.let { RuleError.CannotAffordFee(PlayerId(it)) }

    RuleErrorCode.PROMISE_TO_SELF -> RuleError.PromiseToSelf
}
