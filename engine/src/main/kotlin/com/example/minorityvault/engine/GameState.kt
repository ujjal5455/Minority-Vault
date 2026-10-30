package com.example.minorityvault.engine

/** Everything about the current round. Contracts and promises expire with it. */
data class RoundState(
    val number: Int,
    val questionId: QuestionId,
    val twistOptions: List<Twist>,
    val twist: Twist? = null,
    val ballots: Map<PlayerId, Ballot> = emptyMap(),
    val votes: Map<PlayerId, Vote> = emptyMap(),
    val contracts: List<Contract> = emptyList(),
    val promises: List<Promise> = emptyList(),
    val summary: RoundSummary? = null,
) {
    val signedContracts: List<Contract> get() = contracts.filter { it.status == ContractStatus.SIGNED }

    /** The signed VoteSale for [debtor] this round, if any. */
    fun voteSaleFor(debtor: PlayerId): ContractTerms.VoteSale? = signedContracts
        .map { it.terms }
        .filterIsInstance<ContractTerms.VoteSale>()
        .firstOrNull { it.debtor == debtor }

    /** Who may cast [player]'s vote: their buyer if they sold it, otherwise themselves. */
    fun controllerOf(player: PlayerId): PlayerId = voteSaleFor(player)?.buyer ?: player
}

/**
 * The host's full, authoritative state. Never send it to a client; send [toPublicView] and [toPrivateView] instead.
 */
data class GameState(
    val config: GameConfig,
    val players: List<Player>,
    val phase: Phase,
    val pot: Int,
    val round: RoundState?,
    val unusedQuestions: List<QuestionId>,
    val history: List<RoundSummary>,
    val nextContractId: Int,
    val rngState: Long,
    val winners: List<PlayerId>,
) {
    /** Balances plus pot. Constant for a whole match. */
    val totalCoins: Int get() = players.sumOf { it.balance } + pot

    fun player(id: PlayerId): Player? = players.firstOrNull { it.id == id }

    internal fun withRound(change: (RoundState) -> RoundState): GameState = copy(round = change(checkNotNull(round)))

    companion object {
        /** A new match waiting in the lobby. [seed] decides every random choice in the match. */
        fun newLobby(config: GameConfig, seats: List<PlayerSeat>, questionIds: List<QuestionId>, seed: Long) =
            GameState(
                config = config,
                players = seats.map { seat ->
                    Player(
                        id = seat.id,
                        name = seat.name,
                        isBot = seat.isBot,
                        balance = config.startingBalance,
                        items = mapOf(Item.DOUBLE_VOTE to config.startingDoubleVotes),
                    )
                },
                phase = Phase.Lobby,
                pot = 0,
                round = null,
                unusedQuestions = questionIds.distinct(),
                history = emptyList(),
                nextContractId = 1,
                rngState = seed,
                winners = emptyList(),
            )
    }
}
