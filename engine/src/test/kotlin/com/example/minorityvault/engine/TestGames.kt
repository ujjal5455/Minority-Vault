package com.example.minorityvault.engine

val P1 = PlayerId("p1")
val P2 = PlayerId("p2")
val P3 = PlayerId("p3")
val P4 = PlayerId("p4")
val P5 = PlayerId("p5")

fun seats(count: Int): List<PlayerSeat> = (1..count).map { PlayerSeat(PlayerId("p$it"), "Player $it") }

fun questions(count: Int = 30): List<QuestionId> = (1..count).map { QuestionId("q$it") }

fun lobby(players: Int = 5, config: GameConfig = GameConfig(), seed: Long = 7L): GameState =
    GameState.newLobby(config, seats(players), questions(), seed)

/** A started game in QUESTION with [twist] forced, so tests never depend on which twists the RNG offered. */
fun startedGame(players: Int = 5, config: GameConfig = GameConfig(), twist: Twist = Twist.NoTwist): GameState =
    lobby(players, config)
        .apply(StartGame)
        .withRound { it.copy(twistOptions = listOf(twist)) }
        .apply(ChooseTwist(twist.id))

fun GameState.result(intent: Intent): ReduceResult = GameEngine.reduce(this, intent)

fun GameState.apply(intent: Intent): GameState = when (val result = result(intent)) {
    is ReduceResult.Applied -> result.state
    is ReduceResult.Rejected -> error("Expected $intent to apply in $phase, but it was rejected: ${result.error}")
}

fun GameState.events(intent: Intent): List<PublicEvent> = when (val result = result(intent)) {
    is ReduceResult.Applied -> result.events
    is ReduceResult.Rejected -> error("Expected $intent to apply in $phase, but it was rejected: ${result.error}")
}

fun GameState.rejection(intent: Intent): RuleError = when (val result = result(intent)) {
    is ReduceResult.Applied -> error("Expected $intent to be rejected in $phase, but it applied")
    is ReduceResult.Rejected -> result.error
}

fun GameState.advance(): GameState = apply(AdvancePhase(phase))

fun GameState.advanceTo(target: Phase): GameState {
    var state = this
    repeat(MAX_ADVANCES) {
        if (state.phase == target) return state
        state = state.advance()
    }
    error("Never reached $target")
}

fun GameState.withBalances(vararg balances: Int): GameState =
    copy(players = players.mapIndexed { i, player -> player.copy(balance = balances[i]) })

/** Makes [player] a Debtor as if a previous settlement had left them at 0. */
fun GameState.asDebtor(player: PlayerId): GameState =
    copy(players = players.map { if (it.id == player) it.copy(balance = 0, isDebtor = true) else it })

/** Selects, optionally doubles, and locks [player]'s own vote. */
fun GameState.vote(player: PlayerId, side: Side, doubled: Boolean = false): GameState {
    val selected = apply(VoteSelect(player, side))
    val maybeDoubled = if (doubled) selected.apply(UseItem(player, Item.DOUBLE_VOTE)) else selected
    return maybeDoubled.apply(VoteLock(player))
}

/** Goes to VOTE, casts [votes] (everyone else abstains), and closes VOTE. */
fun GameState.playVote(vararg votes: Pair<PlayerId, Side>, doubled: Set<PlayerId> = emptySet()): GameState =
    votes.fold(advanceTo(Phase.Vote)) { state, (player, side) -> state.vote(player, side, player in doubled) }
        .advance()

fun GameState.balance(player: PlayerId): Int = checkNotNull(player(player)).balance

fun GameState.balances(): List<Int> = players.map { it.balance }

val GameState.summary: RoundSummary get() = checkNotNull(round?.summary) { "No summary in $phase" }

val GameState.contracts: List<Contract> get() = checkNotNull(round).contracts

private const val MAX_ADVANCES = 100
