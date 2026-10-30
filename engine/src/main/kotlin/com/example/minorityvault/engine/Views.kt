package com.example.minorityvault.engine

/** What everyone at the table may see about a player. */
data class PublicPlayer(
    val id: PlayerId,
    val name: String,
    val isBot: Boolean,
    val balance: Int,
    val isDebtor: Boolean,
    val brokenPromises: Int,
    /** Lock status dots during VOTE. Says nothing about which side. */
    val hasLocked: Boolean,
)

data class PublicPromise(val from: PlayerId, val side: Side)

/** Sent to every client (CLAUDE.md §3 rule 3). Must never contain a vote before REVEAL. */
data class PublicGameView(
    val phase: Phase,
    val round: Int,
    val totalRounds: Int,
    val questionId: QuestionId?,
    val twist: TwistInfo?,
    val isAllIn: Boolean,
    val baseFine: Int,
    /** What signing a contract costs, so clients can warn before someone signs. */
    val contractFee: Int,
    val pot: Int,
    val players: List<PublicPlayer>,
    val publicPromises: List<PublicPromise>,
    /** Live totals during FLIP_WINDOW only. */
    val flipWindowTotals: VoteTotals?,
    /** The round's reveal from REVEAL onwards. */
    val reveal: RevealSequence?,
    val winners: List<PlayerId>?,
    /** At GAME_OVER every round, promise and contract is revealed for the betrayal graph. */
    val finalHistory: List<RoundSummary>?,
)

/** Sent only to [playerId]. */
data class PrivatePlayerView(
    val playerId: PlayerId,
    val items: Map<Item, Int>,
    /** Own ballot during VOTE; null if the vote is sold. */
    val ballot: Ballot?,
    /** Own final vote during FLIP_WINDOW; null if the vote is sold. */
    val vote: Vote?,
    /** The buyer casting this player's vote this round. */
    val voteSoldTo: PlayerId?,
    /** Ballots this player bought, during VOTE. */
    val controlledBallots: Map<PlayerId, Ballot>,
    /** Votes this player bought, during FLIP_WINDOW. */
    val controlledVotes: Map<PlayerId, Vote>,
    val isInsider: Boolean,
    /** Weights of locked ballots, live during VOTE, for the Insider only. */
    val insiderTotals: VoteTotals?,
    /** This round's contracts this player is a party to, in any status. */
    val contracts: List<Contract>,
    val promisesMade: List<Promise>,
    /** Promises addressed to this player directly. Public ones are in [PublicGameView.publicPromises]. */
    val promisesReceived: List<Promise>,
    /** From REVEAL: contract payments this player made or received. */
    val transfers: List<ContractTransfer>,
    /** From REVEAL: verdicts on promises this player made or received. */
    val judgedPromises: List<JudgedPromise>,
)

fun GameState.toPublicView(): PublicGameView {
    val round = round
    val number = round?.number ?: 0
    val promises = round?.promises.orEmpty()
    return PublicGameView(
        phase = phase,
        round = number,
        totalRounds = config.rounds,
        questionId = round?.questionId,
        twist = round?.twist?.publicInfo(),
        isAllIn = round != null && config.isFinalRound(number),
        baseFine = if (round == null) 0 else config.baseFine(number),
        contractFee = config.contractFee,
        pot = pot,
        players = players.map { player ->
            PublicPlayer(
                id = player.id,
                name = player.name,
                isBot = player.isBot,
                balance = player.balance,
                isDebtor = player.isDebtor,
                brokenPromises = player.brokenPromises,
                hasLocked = phase == Phase.Vote && round?.ballots?.get(player.id)?.locked == true,
            )
        },
        publicPromises = promises.filter { it.to == PromiseTarget.Everyone }.map { PublicPromise(it.from, it.side) },
        flipWindowTotals = round?.votes?.values?.totals()?.takeIf { phase == Phase.FlipWindow },
        reveal = round?.summary?.reveal?.takeIf { phase.isRevealed },
        winners = winners.takeIf { phase == Phase.GameOver },
        finalHistory = history.takeIf { phase == Phase.GameOver },
    )
}

/** Null if [playerId] isn't in the game. */
fun GameState.toPrivateView(playerId: PlayerId): PrivatePlayerView? {
    val me = player(playerId) ?: return null
    val round = round
    val controlsOwnVote = round == null || round.controllerOf(playerId) == playerId
    val bought = round?.signedContracts.orEmpty()
        .map { it.terms }
        .filterIsInstance<ContractTerms.VoteSale>()
        .filter { it.buyer == playerId }
        .map { it.debtor }
    val isInsider = round?.twist == Twist.Insider(playerId)
    val summary = round?.summary?.takeIf { phase.isRevealed }
    return PrivatePlayerView(
        playerId = playerId,
        items = me.items,
        ballot = round?.ballots?.get(playerId)?.takeIf { phase == Phase.Vote && controlsOwnVote },
        vote = round?.votes?.get(playerId)?.takeIf { phase == Phase.FlipWindow && controlsOwnVote },
        voteSoldTo = round?.voteSaleFor(playerId)?.buyer,
        controlledBallots = if (phase ==
            Phase.Vote
        ) {
            round?.ballots.orEmpty().filterKeys { it in bought }
        } else {
            emptyMap()
        },
        controlledVotes = if (phase ==
            Phase.FlipWindow
        ) {
            round?.votes.orEmpty().filterKeys { it in bought }
        } else {
            emptyMap()
        },
        isInsider = isInsider,
        insiderTotals = round?.ballots?.values
            ?.let { ballots -> VoteTotals(yes = ballots.lockedWeight(Side.YES), no = ballots.lockedWeight(Side.NO)) }
            ?.takeIf { phase == Phase.Vote && isInsider },
        contracts = round?.contracts.orEmpty().filter { playerId in it.terms.parties },
        promisesMade = round?.promises.orEmpty().filter { it.from == playerId },
        promisesReceived = round?.promises.orEmpty().filter { it.to == PromiseTarget.ToPlayer(playerId) },
        transfers = summary?.transfers.orEmpty().filter { it.from == playerId || it.to == playerId },
        judgedPromises = summary?.promises.orEmpty().filter { it.promise.involves(playerId) },
    )
}

private fun Collection<Ballot>.lockedWeight(side: Side): Int = filter { it.selection == side }.sumOf { it.lockedWeight }
