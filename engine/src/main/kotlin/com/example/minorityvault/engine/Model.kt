package com.example.minorityvault.engine

@JvmInline
value class PlayerId(val value: String)

@JvmInline
value class QuestionId(val value: String)

@JvmInline
value class ContractId(val value: Int)

sealed interface Phase {
    data object Lobby : Phase
    data object Question : Phase
    data object Negotiation : Phase
    data object Vote : Phase
    data object FlipWindow : Phase
    data object Reveal : Phase
    data object Settlement : Phase
    data object GameOver : Phase
}

enum class Item { DOUBLE_VOTE }

/** A seat in the lobby before the game starts. */
data class PlayerSeat(val id: PlayerId, val name: String, val isBot: Boolean = false)

data class Player(
    val id: PlayerId,
    val name: String,
    val isBot: Boolean,
    val balance: Int,
    val isDebtor: Boolean = false,
    val items: Map<Item, Int> = emptyMap(),
    val brokenPromises: Int = 0,
) {
    fun itemCount(item: Item): Int = items[item] ?: 0

    internal fun withItemUsed(item: Item): Player = copy(items = items + (item to itemCount(item) - 1))
}

enum class Side {
    YES,
    NO,
    ;

    val choice: VoteChoice get() = if (this == YES) VoteChoice.YES else VoteChoice.NO
}

enum class VoteChoice {
    YES,
    NO,
    ABSTAIN,
    ;

    val opposite: VoteChoice get() = when (this) {
        YES -> NO
        NO -> YES
        ABSTAIN -> ABSTAIN
    }
}

/** A vote while VOTE is open. Only a locked ballot counts when the phase ends. */
data class Ballot(val selection: Side? = null, val doubled: Boolean = false, val locked: Boolean = false) {
    internal fun toVote(): Vote = if (locked && selection != null) Vote(selection.choice, doubled) else Vote.ABSTAIN

    internal val lockedWeight: Int get() = if (locked) toVote().weight else 0
}

/** A final vote, fixed when VOTE ends. Only a flip during FLIP_WINDOW can change it. */
data class Vote(val choice: VoteChoice, val doubled: Boolean = false, val flipped: Boolean = false) {
    val weight: Int get() = when {
        choice == VoteChoice.ABSTAIN -> 0
        doubled -> 2
        else -> 1
    }

    companion object {
        val ABSTAIN = Vote(VoteChoice.ABSTAIN)
    }
}

/** Summed vote weights. Never says who voted what. */
data class VoteTotals(val yes: Int, val no: Int)

internal fun Collection<Vote>.totals(): VoteTotals = VoteTotals(
    yes = filter { it.choice == VoteChoice.YES }.sumOf { it.weight },
    no = filter { it.choice == VoteChoice.NO }.sumOf { it.weight },
)
