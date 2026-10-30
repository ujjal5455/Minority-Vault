package com.example.minorityvault.feature.round

import com.example.minorityvault.engine.ContractId
import com.example.minorityvault.engine.ContractPropose
import com.example.minorityvault.engine.ContractRespond
import com.example.minorityvault.engine.ContractTerms
import com.example.minorityvault.engine.Item
import com.example.minorityvault.engine.MakePromise
import com.example.minorityvault.engine.PlayerId
import com.example.minorityvault.engine.PlayerIntent
import com.example.minorityvault.engine.PromiseTarget
import com.example.minorityvault.engine.Side
import com.example.minorityvault.engine.UseItem
import com.example.minorityvault.engine.VoteFlip
import com.example.minorityvault.engine.VoteLock
import com.example.minorityvault.engine.VoteSelect
import com.example.minorityvault.engine.VoteUnlock

/** What a player did on a round screen. [forPlayer] is set only when acting on a bought vote. */
sealed interface RoundAction {
    data class MakePromise(val to: PromiseTarget, val side: Side) : RoundAction

    data class Propose(val terms: ContractTerms) : RoundAction

    data class Respond(val contractId: ContractId, val accept: Boolean) : RoundAction

    data class SelectVote(val side: Side, val forPlayer: PlayerId? = null) : RoundAction

    /** Arms or disarms the Double Vote; it's applied when the player locks. */
    data class ArmDoubleVote(val armed: Boolean) : RoundAction

    data class Lock(val forPlayer: PlayerId? = null) : RoundAction

    data class Unlock(val forPlayer: PlayerId? = null) : RoundAction

    data class Flip(val forPlayer: PlayerId? = null) : RoundAction
}

/**
 * The engine intents for [this] action taken by [player]. Locking your own vote with the Double Vote armed
 * applies it first. [RoundAction.ArmDoubleVote] is screen state and has no intent.
 */
fun RoundAction.toIntents(player: PlayerId, doubleArmed: Boolean): List<PlayerIntent> = when (this) {
    is RoundAction.MakePromise -> listOf(MakePromise(player, to, side))

    is RoundAction.Propose -> listOf(ContractPropose(player, terms))

    is RoundAction.Respond -> listOf(ContractRespond(player, contractId, accept))

    is RoundAction.SelectVote -> listOf(VoteSelect(player, side, forPlayer))

    is RoundAction.ArmDoubleVote -> emptyList()

    is RoundAction.Lock ->
        if (forPlayer == null && doubleArmed) {
            listOf(UseItem(player, Item.DOUBLE_VOTE), VoteLock(player))
        } else {
            listOf(VoteLock(player, forPlayer))
        }

    is RoundAction.Unlock -> listOf(VoteUnlock(player, forPlayer))

    is RoundAction.Flip -> listOf(VoteFlip(player, forPlayer))
}
