package com.example.minorityvault.engine

/** The single entry point to the rules: every change to a [GameState] goes through [reduce]. */
object GameEngine {
    /**
     * Applies [intent] to [state]. Illegal intents come back as [ReduceResult.Rejected] and never throw.
     * All randomness comes from [GameState.rngState], so the same seed and intents always give the same states.
     */
    fun reduce(state: GameState, intent: Intent): ReduceResult = when (intent) {
        StartGame -> startGame(state)
        is AdvancePhase -> advance(state, intent.from)
        is TimerExpired -> advance(state, intent.phase)
        is ChooseTwist -> chooseTwist(state, intent.twistId)
        is PlayerIntent -> reducePlayerIntent(state, intent)
    }

    private fun reducePlayerIntent(state: GameState, intent: PlayerIntent): ReduceResult =
        if (state.player(intent.player) == null) {
            ReduceResult.Rejected(RuleError.UnknownPlayer(intent.player))
        } else {
            when (intent) {
                is ContractPropose -> proposeContract(state, intent)
                is ContractRespond -> respondToContract(state, intent)
                is VoteSelect -> selectVote(state, intent)
                is VoteLock -> lockVote(state, intent)
                is VoteUnlock -> unlockVote(state, intent)
                is VoteFlip -> flipVote(state, intent)
                is UseItem -> useItem(state, intent)
                is MakePromise -> makePromise(state, intent)
            }
        }
}

internal fun RuleError?.orApply(apply: () -> ReduceResult): ReduceResult =
    if (this != null) ReduceResult.Rejected(this) else apply()
