package com.example.minorityvault.protocol

import com.example.minorityvault.engine.Item
import com.example.minorityvault.engine.Side
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Client → host messages (CLAUDE.md §7). None of them names its sender: the host knows who sent a message from
 * the connection it arrived on, so a client can never act as someone else.
 */
@Serializable
sealed interface ClientIntent {
    /** [playerToken] is set when rejoining after a drop, to get the same seat back. */
    @Serializable
    @SerialName("Join")
    data class Join(val name: String, val playerToken: String? = null) : ClientIntent

    @Serializable
    @SerialName("Ready")
    data object Ready : ClientIntent

    @Serializable
    @SerialName("ChatPublic")
    data class ChatPublic(val text: String) : ClientIntent

    @Serializable
    @SerialName("ChatDm")
    data class ChatDm(val to: String, val text: String) : ClientIntent

    /** [to] is a player id, or null for everyone. */
    @Serializable
    @SerialName("MakePromise")
    data class MakePromise(val to: String? = null, val side: Side) : ClientIntent

    @Serializable
    @SerialName("ContractPropose")
    data class ContractPropose(val terms: ContractTermsDto) : ClientIntent

    @Serializable
    @SerialName("ContractRespond")
    data class ContractRespond(val contractId: Int, val accept: Boolean) : ClientIntent

    /** [forPlayer] is set only by a VoteSale buyer casting the debtor's vote. */
    @Serializable
    @SerialName("VoteSelect")
    data class VoteSelect(val side: Side, val forPlayer: String? = null) : ClientIntent

    @Serializable
    @SerialName("VoteLock")
    data class VoteLock(val forPlayer: String? = null) : ClientIntent

    @Serializable
    @SerialName("VoteUnlock")
    data class VoteUnlock(val forPlayer: String? = null) : ClientIntent

    @Serializable
    @SerialName("VoteFlip")
    data class VoteFlip(val forPlayer: String? = null) : ClientIntent

    @Serializable
    @SerialName("UseItem")
    data class UseItem(val item: Item) : ClientIntent

    /** [sentAt] is the client's clock; the host echoes it back so the client can measure the offset. */
    @Serializable
    @SerialName("Heartbeat")
    data class Heartbeat(val sentAt: Long) : ClientIntent
}
