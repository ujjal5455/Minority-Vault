package com.example.minorityvault.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Host → client messages (CLAUDE.md §7). Each client only ever receives what it may see. */
@Serializable
sealed interface HostEvent {
    /** Sent only to the player who joined. Keep [playerToken]: it is the only way back into this seat. */
    @Serializable
    @SerialName("Welcome")
    data class Welcome(val playerId: String, val playerToken: String, val roomCode: String) : HostEvent

    @Serializable
    @SerialName("LobbyUpdate")
    data class LobbyUpdate(
        val roomCode: String,
        val players: List<LobbyPlayerDto>,
        val minPlayers: Int,
        val maxPlayers: Int,
        val started: Boolean,
    ) : HostEvent

    /** [endsAt] is on the host's clock, or null for phases the host ends itself. */
    @Serializable
    @SerialName("PhaseStarted")
    data class PhaseStarted(val round: Int, val phase: WirePhase, val endsAt: Long? = null) : HostEvent

    @Serializable
    @SerialName("PublicView")
    data class PublicView(val view: PublicGameViewDto) : HostEvent

    @Serializable
    @SerialName("PrivateView")
    data class PrivateView(val view: PrivatePlayerViewDto) : HostEvent

    /** [to] is null for the public lobby. A message with a [promise] is a structured promise, not just words. */
    @Serializable
    @SerialName("ChatMessage")
    data class ChatMessage(
        val id: Long,
        val from: String,
        val to: String? = null,
        val text: String = "",
        val promise: PromiseDto? = null,
        val sentAt: Long,
    ) : HostEvent

    @Serializable
    @SerialName("ContractUpdate")
    data class ContractUpdate(val contract: ContractDto) : HostEvent

    @Serializable
    @SerialName("KeeperLine")
    data class KeeperLine(val text: String) : HostEvent

    @Serializable
    @SerialName("RevealSequence")
    data class RevealSequence(val round: Int, val sequence: RevealSequenceDto) : HostEvent

    @Serializable
    @SerialName("RoundSettled")
    data class RoundSettled(val round: Int, val balances: Map<String, Int>, val pot: Int) : HostEvent

    @Serializable
    @SerialName("GameOver")
    data class GameOver(val winners: List<String>, val history: List<RoundSummaryDto>) : HostEvent

    /** [ruleError] is set when [code] is [ErrorCode.RULE]. */
    @Serializable
    @SerialName("Error")
    data class Error(val code: ErrorCode, val ruleError: RuleErrorDto? = null) : HostEvent

    /** The answer to a heartbeat: [clientSentAt] echoed back, with the host's clock when it replied. */
    @Serializable
    @SerialName("HeartbeatReply")
    data class HeartbeatReply(val clientSentAt: Long, val hostTime: Long) : HostEvent
}

@Serializable
data class LobbyPlayerDto(
    val id: String,
    val name: String,
    val isHost: Boolean,
    val isBot: Boolean,
    val connected: Boolean,
)

@Serializable
enum class ErrorCode {
    /** The other phone runs a different major protocol version. */
    VERSION_MISMATCH,
    LOBBY_FULL,
    NAME_TAKEN,
    NAME_INVALID,
    GAME_IN_PROGRESS,

    /** A game message arrived before Join. */
    NOT_JOINED,
    REMOVED_BY_HOST,
    LOBBY_CLOSED,
    MALFORMED,

    /** The engine refused the intent; see [HostEvent.Error.ruleError]. */
    RULE,
}
