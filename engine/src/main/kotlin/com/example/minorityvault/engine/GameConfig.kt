package com.example.minorityvault.engine

internal const val PERCENT = 100

/** Rounds up to and including [lastRound] (after the previous tier) charge [fine]. */
data class FineTier(val lastRound: Int, val fine: Int)

/** Every number in the rules (CLAUDE.md §2). Nothing else may hardcode them. */
data class GameConfig(
    val minPlayers: Int = DEFAULT_MIN_PLAYERS,
    val maxPlayers: Int = DEFAULT_MAX_PLAYERS,
    val startingBalance: Int = DEFAULT_STARTING_BALANCE,
    val rounds: Int = DEFAULT_ROUNDS,
    val fineTiers: List<FineTier> = DEFAULT_FINE_TIERS,
    val allInPercent: Int = DEFAULT_ALL_IN_PERCENT,
    val questionSeconds: Int = DEFAULT_QUESTION_SECONDS,
    val negotiationSeconds: Int = DEFAULT_NEGOTIATION_SECONDS,
    val voteSeconds: Int = DEFAULT_VOTE_SECONDS,
    val flipWindowSeconds: Int = DEFAULT_FLIP_WINDOW_SECONDS,
    val contractFee: Int = DEFAULT_CONTRACT_FEE,
    val startingDoubleVotes: Int = 1,
    val keeperInterjectionCooldownSeconds: Int = DEFAULT_INTERJECTION_COOLDOWN_SECONDS,
    val twistOptionCount: Int = DEFAULT_TWIST_OPTION_COUNT,
) {
    init {
        require(minPlayers in 2..maxPlayers) { "minPlayers must be between 2 and maxPlayers" }
        require(startingBalance > 0) { "startingBalance must be positive" }
        require(rounds > 0) { "rounds must be positive" }
        require(fineTiers.isNotEmpty()) { "fineTiers must not be empty" }
        require(fineTiers.zipWithNext().all { (a, b) -> a.lastRound < b.lastRound }) { "fineTiers must be ascending" }
        require(allInPercent in 0..PERCENT) { "allInPercent must be 0–100" }
        require(negotiationSeconds in NEGOTIATION_SECONDS_RANGE) { "negotiationSeconds must be in 60–300" }
        require(contractFee >= 0) { "contractFee must not be negative" }
        require(twistOptionCount > 0) { "twistOptionCount must be positive" }
    }

    fun isFinalRound(round: Int): Boolean = round == rounds

    /** The tier fine for [round]. The final round uses [allInPercent] instead; see [fineFor]. */
    fun baseFine(round: Int): Int = (fineTiers.firstOrNull { round <= it.lastRound } ?: fineTiers.last()).fine

    /** What a non-minority player with [balance] owes in [round], before capping at their balance. */
    fun fineFor(round: Int, balance: Int): Int =
        if (isFinalRound(round)) balance * allInPercent / PERCENT else baseFine(round)

    /** How long the host's timer runs for [phase], or null for phases the host advances itself. */
    fun phaseSeconds(phase: Phase): Int? = when (phase) {
        Phase.Question -> questionSeconds
        Phase.Negotiation -> negotiationSeconds
        Phase.Vote -> voteSeconds
        Phase.FlipWindow -> flipWindowSeconds
        Phase.Lobby, Phase.Reveal, Phase.Settlement, Phase.GameOver -> null
    }

    companion object {
        const val DEFAULT_MIN_PLAYERS = 4
        const val DEFAULT_MAX_PLAYERS = 8
        const val DEFAULT_STARTING_BALANCE = 100
        const val DEFAULT_ROUNDS = 10
        const val DEFAULT_ALL_IN_PERCENT = 50
        const val DEFAULT_QUESTION_SECONDS = 10
        const val DEFAULT_NEGOTIATION_SECONDS = 150
        const val DEFAULT_VOTE_SECONDS = 20
        const val DEFAULT_FLIP_WINDOW_SECONDS = 10
        const val DEFAULT_CONTRACT_FEE = 5
        const val DEFAULT_INTERJECTION_COOLDOWN_SECONDS = 20
        const val DEFAULT_TWIST_OPTION_COUNT = 3
        val NEGOTIATION_SECONDS_RANGE = 60..300
        val DEFAULT_FINE_TIERS = listOf(
            FineTier(lastRound = 3, fine = 10),
            FineTier(lastRound = 6, fine = 20),
            FineTier(lastRound = Int.MAX_VALUE, fine = 30),
        )
    }
}
