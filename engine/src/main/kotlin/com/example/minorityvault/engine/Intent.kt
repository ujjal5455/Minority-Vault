package com.example.minorityvault.engine

/** Something that may change the game. Player intents use the same names as the protocol (CLAUDE.md §7). */
sealed interface Intent

/** Sent only by the host itself, never by a client. */
sealed interface HostIntent : Intent

data object StartGame : HostIntent

/** The host ends [from] early, e.g. everyone locked their vote or the reveal animation finished. */
data class AdvancePhase(val from: Phase) : HostIntent

/** The host's timer for [phase] ran out. Rejected if the game already left [phase]. */
data class TimerExpired(val phase: Phase) : HostIntent

/** The Vault Keeper's pick from the round's twist options. */
data class ChooseTwist(val twistId: TwistId) : HostIntent

/** Sent on behalf of [player]. The host sets [player] from the connection, never from the message body. */
sealed interface PlayerIntent : Intent {
    val player: PlayerId
}

data class ContractPropose(override val player: PlayerId, val terms: ContractTerms) : PlayerIntent

data class ContractRespond(override val player: PlayerId, val contractId: ContractId, val accept: Boolean) :
    PlayerIntent

/** [forPlayer] is set only by a VoteSale buyer acting on the debtor's vote. */
data class VoteSelect(override val player: PlayerId, val side: Side, val forPlayer: PlayerId? = null) : PlayerIntent

data class VoteLock(override val player: PlayerId, val forPlayer: PlayerId? = null) : PlayerIntent

data class VoteUnlock(override val player: PlayerId, val forPlayer: PlayerId? = null) : PlayerIntent

data class VoteFlip(override val player: PlayerId, val forPlayer: PlayerId? = null) : PlayerIntent

data class UseItem(override val player: PlayerId, val item: Item) : PlayerIntent

data class MakePromise(override val player: PlayerId, val to: PromiseTarget, val side: Side) : PlayerIntent

sealed interface ReduceResult {
    data class Applied(val state: GameState, val events: List<PublicEvent> = emptyList()) : ReduceResult

    data class Rejected(val error: RuleError) : ReduceResult
}

/** Things everyone may hear about, for Keeper interjections and sounds. Never carries private details. */
sealed interface PublicEvent {
    data class PhaseStarted(val round: Int, val phase: Phase) : PublicEvent

    data class PublicPromiseMade(val player: PlayerId, val side: Side) : PublicEvent

    /** Deliberately anonymous: no parties, no terms. */
    data object ContractSigned : PublicEvent

    data class PromiseBroken(val player: PlayerId) : PublicEvent

    data class BecameDebtor(val player: PlayerId) : PublicEvent

    data class GameEnded(val winners: List<PlayerId>) : PublicEvent
}
