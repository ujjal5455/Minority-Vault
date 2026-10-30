package com.example.minorityvault.engine

/** Stable ids the Vault Keeper picks from. */
enum class TwistId { EXACT_MINORITY, SECOND_CHANCE, INSIDER, ROBIN_HOOD, NO_TWIST }

/**
 * A round's twist. Twists never change vote counts; they only change who gets paid
 * ([payoutWinners]) or what players may see and do (SecondChance, Insider).
 */
sealed interface Twist {
    val id: TwistId

    fun isValid(players: List<Player>): Boolean = true

    /**
     * Who splits the pot. [minority] is null when there is no minority. [balancesAfterFines] keeps seat order.
     */
    fun payoutWinners(minority: List<PlayerId>?, balancesAfterFines: Map<PlayerId, Int>): List<PlayerId> =
        minority.orEmpty()

    /** The minority wins only if it has exactly [size] players; otherwise the pot rolls over. */
    data class ExactMinority(val size: Int) : Twist {
        override val id: TwistId get() = TwistId.EXACT_MINORITY

        override fun isValid(players: List<Player>): Boolean = size >= 1 && size * 2 < players.size

        override fun payoutWinners(minority: List<PlayerId>?, balancesAfterFines: Map<PlayerId, Int>) =
            if (minority?.size == size) minority else emptyList()
    }

    /** Opens a FLIP_WINDOW after VOTE where each voter may flip once. */
    data object SecondChance : Twist {
        override val id: TwistId get() = TwistId.SECOND_CHANCE
    }

    /** [player] sees live vote totals during VOTE. Nobody else learns who it is. */
    data class Insider(val player: PlayerId) : Twist {
        override val id: TwistId get() = TwistId.INSIDER

        override fun isValid(players: List<Player>): Boolean = players.any { it.id == player }
    }

    /** The poorest player (after fines) takes the pot instead of the minority. Ties split it. */
    data object RobinHood : Twist {
        override val id: TwistId get() = TwistId.ROBIN_HOOD

        override fun payoutWinners(minority: List<PlayerId>?, balancesAfterFines: Map<PlayerId, Int>): List<PlayerId> {
            val lowest = balancesAfterFines.values.minOrNull()
            return if (minority == null || lowest == null) {
                emptyList()
            } else {
                balancesAfterFines.filterValues { it == lowest }.keys.toList()
            }
        }
    }

    data object NoTwist : Twist {
        override val id: TwistId get() = TwistId.NO_TWIST
    }
}

/** What everyone may know about a twist: it hides who the Insider is. */
data class TwistInfo(val id: TwistId, val exactMinoritySize: Int? = null)

fun Twist.publicInfo(): TwistInfo = TwistInfo(id, (this as? Twist.ExactMinority)?.size)

object TwistCatalog {
    /** Up to [count] valid twists with distinct ids, in random order. The first one is the fallback choice. */
    fun offerTwists(players: List<Player>, random: SeededRandom, count: Int): List<Twist> {
        val largestExactMinority = (players.size - 1) / 2
        val candidates = listOfNotNull(
            if (largestExactMinority >= 1) Twist.ExactMinority(1 + random.nextInt(largestExactMinority)) else null,
            Twist.SecondChance,
            Twist.Insider(random.pick(players).id),
            Twist.RobinHood,
            Twist.NoTwist,
        ).filter { it.isValid(players) }
        return random.shuffled(candidates).take(count)
    }
}
