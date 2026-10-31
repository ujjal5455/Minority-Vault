package com.example.minorityvault.protocol

import com.example.minorityvault.engine.ContractId
import com.example.minorityvault.engine.ContractPropose
import com.example.minorityvault.engine.ContractRespond
import com.example.minorityvault.engine.MakePromise
import com.example.minorityvault.engine.PlayerId
import com.example.minorityvault.engine.PlayerIntent
import com.example.minorityvault.engine.PromiseTarget
import com.example.minorityvault.engine.UseItem
import com.example.minorityvault.engine.VoteFlip
import com.example.minorityvault.engine.VoteLock
import com.example.minorityvault.engine.VoteSelect
import com.example.minorityvault.engine.VoteUnlock

/**
 * The engine intent for this message. [sender] must come from the connection the message arrived on, never from
 * the message itself. Null for messages the engine doesn't handle (joining, chat, heartbeats).
 */
fun ClientIntent.toEngineIntent(sender: PlayerId): PlayerIntent? = when (this) {
    is ClientIntent.MakePromise ->
        MakePromise(sender, to?.let { PromiseTarget.ToPlayer(PlayerId(it)) } ?: PromiseTarget.Everyone, side)

    is ClientIntent.ContractPropose -> ContractPropose(sender, terms.toEngine())

    is ClientIntent.ContractRespond -> ContractRespond(sender, ContractId(contractId), accept)

    is ClientIntent.VoteSelect -> VoteSelect(sender, side, forPlayer?.let(::PlayerId))

    is ClientIntent.VoteLock -> VoteLock(sender, forPlayer?.let(::PlayerId))

    is ClientIntent.VoteUnlock -> VoteUnlock(sender, forPlayer?.let(::PlayerId))

    is ClientIntent.VoteFlip -> VoteFlip(sender, forPlayer?.let(::PlayerId))

    is ClientIntent.UseItem -> UseItem(sender, item)

    is ClientIntent.Join, ClientIntent.Ready, is ClientIntent.ChatPublic, is ClientIntent.ChatDm,
    is ClientIntent.Heartbeat,
    -> null
}
