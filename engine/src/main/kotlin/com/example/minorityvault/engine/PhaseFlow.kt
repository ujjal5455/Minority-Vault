package com.example.minorityvault.engine

private val REVEALED_PHASES = setOf(Phase.Reveal, Phase.Settlement, Phase.GameOver)

internal val Phase.isRevealed: Boolean get() = this in REVEALED_PHASES

internal fun startGame(state: GameState): ReduceResult {
    val config = state.config
    val error = when {
        state.phase != Phase.Lobby -> RuleError.WrongPhase(state.phase)

        state.players.size !in config.minPlayers..config.maxPlayers ->
            RuleError.InvalidSetup(SetupProblem.PLAYER_COUNT)

        state.players.distinctBy { it.id }.size != state.players.size ->
            RuleError.InvalidSetup(SetupProblem.DUPLICATE_PLAYER)

        state.unusedQuestions.size < config.rounds -> RuleError.InvalidSetup(SetupProblem.NOT_ENOUGH_QUESTIONS)

        else -> null
    }
    return error.orApply { beginRound(state, number = 1) }
}

internal fun chooseTwist(state: GameState, twistId: TwistId): ReduceResult {
    val chosen = state.round?.twistOptions?.firstOrNull { it.id == twistId }
    val error = when {
        state.phase != Phase.Question -> RuleError.WrongPhase(state.phase)
        chosen == null -> RuleError.TwistNotOffered
        else -> null
    }
    return error.orApply { ReduceResult.Applied(state.withRound { it.copy(twist = chosen) }) }
}

internal fun advance(state: GameState, expected: Phase): ReduceResult {
    if (state.phase != expected) return ReduceResult.Rejected(RuleError.StalePhase(expected, state.phase))
    return when (state.phase) {
        Phase.Lobby, Phase.GameOver -> ReduceResult.Rejected(RuleError.WrongPhase(state.phase))

        Phase.Question -> enter(
            state.withRound { it.copy(twist = it.twist ?: it.twistOptions.firstOrNull() ?: Twist.NoTwist) },
            Phase.Negotiation,
        )

        Phase.Negotiation -> enter(openVote(state), Phase.Vote)

        Phase.Vote -> closeVote(state)

        Phase.FlipWindow -> reveal(state)

        Phase.Reveal -> enter(state, Phase.Settlement)

        Phase.Settlement -> finishRound(state)
    }
}

private fun beginRound(state: GameState, number: Int): ReduceResult {
    val random = SeededRandom(state.rngState)
    val question = random.pick(state.unusedQuestions)
    val options = TwistCatalog.offerTwists(state.players, random, state.config.twistOptionCount)
    val next = state.copy(
        phase = Phase.Question,
        round = RoundState(number = number, questionId = question, twistOptions = options),
        unusedQuestions = state.unusedQuestions - question,
        rngState = random.state,
    )
    return ReduceResult.Applied(next, listOf(PublicEvent.PhaseStarted(number, Phase.Question)))
}

private fun enter(state: GameState, phase: Phase): ReduceResult = ReduceResult.Applied(
    state.copy(phase = phase),
    listOf(PublicEvent.PhaseStarted(checkNotNull(state.round).number, phase)),
)

/** Unsigned proposals expire and everyone gets an empty ballot. */
private fun openVote(state: GameState): GameState = state.withRound { round ->
    round.copy(
        contracts = round.contracts.map {
            if (it.status == ContractStatus.PENDING) it.copy(status = ContractStatus.EXPIRED) else it
        },
        ballots = state.players.associate { it.id to Ballot() },
    )
}

/** Locked ballots become votes, everyone else abstains, and used Double Votes are consumed. */
private fun closeVote(state: GameState): ReduceResult {
    val round = checkNotNull(state.round)
    val votes = state.players.associate { it.id to (round.ballots[it.id] ?: Ballot()).toVote() }
    val closed = state.copy(
        players = state.players.map { if (votes.getValue(it.id).doubled) it.withItemUsed(Item.DOUBLE_VOTE) else it },
        round = round.copy(ballots = emptyMap(), votes = votes),
    )
    return if (round.twist is Twist.SecondChance) enter(closed, Phase.FlipWindow) else reveal(closed)
}

private fun reveal(state: GameState): ReduceResult {
    val round = checkNotNull(state.round)
    val resolution = resolveRound(state.config, state.players, state.pot, round)
    val summary = resolution.summary
    val events = listOf(PublicEvent.PhaseStarted(round.number, Phase.Reveal)) +
        summary.promises.filter { it.verdict == PromiseVerdict.BROKEN }
            .map { PublicEvent.PromiseBroken(it.promise.from) } +
        summary.newDebtors.map { PublicEvent.BecameDebtor(it) }
    val next = state.copy(
        phase = Phase.Reveal,
        players = resolution.players,
        pot = resolution.pot,
        round = round.copy(summary = summary),
    )
    return ReduceResult.Applied(next, events)
}

private fun finishRound(state: GameState): ReduceResult {
    val round = checkNotNull(state.round)
    val recorded = state.copy(history = state.history + checkNotNull(round.summary))
    return if (state.config.isFinalRound(round.number)) gameOver(recorded) else beginRound(recorded, round.number + 1)
}

/** The richest non-Debtors win; ties share the win; if everyone is a Debtor nobody does. */
private fun gameOver(state: GameState): ReduceResult {
    val eligible = state.players.filterNot { it.isDebtor }
    val best = eligible.maxOfOrNull { it.balance }
    val winners = eligible.filter { it.balance == best }.map { it.id }
    return ReduceResult.Applied(
        state.copy(phase = Phase.GameOver, winners = winners),
        listOf(
            PublicEvent.PhaseStarted(checkNotNull(state.round).number, Phase.GameOver),
            PublicEvent.GameEnded(winners),
        ),
    )
}
