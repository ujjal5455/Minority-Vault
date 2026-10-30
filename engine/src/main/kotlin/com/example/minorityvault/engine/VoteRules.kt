package com.example.minorityvault.engine

internal fun selectVote(state: GameState, intent: VoteSelect): ReduceResult =
    updateBallot(state, intent.player, intent.forPlayer, check = { if (it.locked) RuleError.VoteIsLocked else null }) {
        it.copy(selection = intent.side)
    }

internal fun lockVote(state: GameState, intent: VoteLock): ReduceResult = updateBallot(
    state,
    intent.player,
    intent.forPlayer,
    check = {
        when {
            it.locked -> RuleError.VoteIsLocked
            it.selection == null -> RuleError.NothingSelected
            else -> null
        }
    },
) { it.copy(locked = true) }

/** Unlocking also takes back a Double Vote; it was never consumed. */
internal fun unlockVote(state: GameState, intent: VoteUnlock): ReduceResult =
    updateBallot(state, intent.player, intent.forPlayer, check = { if (it.locked) null else RuleError.NotLocked }) {
        it.copy(locked = false, doubled = false)
    }

/** Items only ever apply to the player's own vote, so a VoteSale buyer can't use the debtor's. */
internal fun useItem(state: GameState, intent: UseItem): ReduceResult {
    val owned = state.player(intent.player)?.itemCount(intent.item) ?: 0
    return when (intent.item) {
        Item.DOUBLE_VOTE -> updateBallot(
            state,
            intent.player,
            target = null,
            check = {
                when {
                    it.locked -> RuleError.VoteIsLocked
                    it.doubled -> RuleError.ItemAlreadyApplied(intent.item)
                    owned < 1 -> RuleError.NoItemLeft(intent.item)
                    else -> null
                }
            },
        ) { it.copy(doubled = true) }
    }
}

internal fun flipVote(state: GameState, intent: VoteFlip): ReduceResult {
    val owner = intent.forPlayer ?: intent.player
    val round = state.round
    val vote = round?.votes?.get(owner)
    val error = when {
        state.phase != Phase.FlipWindow || round == null -> RuleError.WrongPhase(state.phase)

        vote == null -> RuleError.UnknownPlayer(owner)

        else -> controlError(round, intent.player, owner) ?: when {
            vote.choice == VoteChoice.ABSTAIN -> RuleError.CannotFlipAbstain
            vote.flipped -> RuleError.AlreadyFlipped
            else -> null
        }
    }
    return error.orApply {
        val flipped = checkNotNull(vote).copy(choice = vote.choice.opposite, flipped = true)
        ReduceResult.Applied(state.withRound { it.copy(votes = it.votes + (owner to flipped)) })
    }
}

/** Shared checks for every VOTE-phase change to [target]'s ballot (or [actor]'s own when [target] is null). */
private fun updateBallot(
    state: GameState,
    actor: PlayerId,
    target: PlayerId?,
    check: (Ballot) -> RuleError?,
    change: (Ballot) -> Ballot,
): ReduceResult {
    val owner = target ?: actor
    val round = state.round
    val ballot = round?.ballots?.get(owner)
    val error = when {
        state.phase != Phase.Vote || round == null -> RuleError.WrongPhase(state.phase)
        ballot == null -> RuleError.UnknownPlayer(owner)
        else -> controlError(round, actor, owner) ?: check(ballot)
    }
    return error.orApply {
        ReduceResult.Applied(
            state.withRound {
                it.copy(ballots = it.ballots + (owner to change(checkNotNull(ballot))))
            },
        )
    }
}

/** Only the vote's controller may touch it: the owner, or the buyer while it is sold. */
private fun controlError(round: RoundState, actor: PlayerId, owner: PlayerId): RuleError? {
    val controller = round.controllerOf(owner)
    return when {
        controller == actor -> null
        owner == actor -> RuleError.VoteSold
        else -> RuleError.NotYourVote
    }
}
