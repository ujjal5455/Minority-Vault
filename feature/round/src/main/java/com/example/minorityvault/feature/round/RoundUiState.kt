package com.example.minorityvault.feature.round

import com.example.minorityvault.ai.QuestionBank
import com.example.minorityvault.engine.Ballot
import com.example.minorityvault.engine.Contract
import com.example.minorityvault.engine.Item
import com.example.minorityvault.engine.PlayerId
import com.example.minorityvault.engine.PrivatePlayerView
import com.example.minorityvault.engine.Promise
import com.example.minorityvault.engine.PromiseTarget
import com.example.minorityvault.engine.PublicGameView
import com.example.minorityvault.engine.RevealStep
import com.example.minorityvault.engine.RuleError
import com.example.minorityvault.engine.Side
import com.example.minorityvault.engine.TwistInfo
import com.example.minorityvault.engine.Vote
import com.example.minorityvault.engine.VoteTotals

data class PlayerUi(
    val id: PlayerId,
    val name: String,
    val balance: Int,
    val isDebtor: Boolean = false,
    val brokenPromises: Int = 0,
    val hasLocked: Boolean = false,
)

data class RoundHeaderUi(
    val round: Int,
    val totalRounds: Int,
    val pot: Int,
    val baseFine: Int,
    val isAllIn: Boolean,
    val twist: TwistInfo?,
    /** String resource for the question text. */
    val questionRes: Int,
)

/** Null in pass-the-phone play, where nobody runs a clock. */
data class TimerUi(val secondsLeft: Int, val fraction: Float)

data class QuestionUiState(val header: RoundHeaderUi, val players: List<PlayerUi>, val timer: TimerUi? = null)

data class NegotiationUiState(
    val header: RoundHeaderUi,
    val me: PlayerId,
    val players: List<PlayerUi>,
    val publicPromises: List<Promise>,
    val promisesMade: List<Promise>,
    val promisesReceived: List<Promise>,
    val contracts: List<Contract>,
    val contractFee: Int,
    val timer: TimerUi? = null,
    val error: RuleError? = null,
)

data class VoteUiState(
    val header: RoundHeaderUi,
    val me: PlayerId,
    val players: List<PlayerUi>,
    /** Null when this player's vote is sold. */
    val ballot: Ballot?,
    val soldTo: PlayerId?,
    val doubleVoteAvailable: Boolean,
    /** The player has chosen to use their Double Vote when they lock. */
    val doubleArmed: Boolean,
    /** Votes this player bought, keyed by debtor. */
    val controlled: Map<PlayerId, Ballot>,
    val insiderTotals: VoteTotals?,
    val timer: TimerUi? = null,
    val error: RuleError? = null,
)

data class FlipUiState(
    val header: RoundHeaderUi,
    val me: PlayerId,
    val players: List<PlayerUi>,
    val totals: VoteTotals,
    val myVote: Vote?,
    val soldTo: PlayerId?,
    val controlled: Map<PlayerId, Vote>,
    val timer: TimerUi? = null,
    val error: RuleError? = null,
)

data class RevealUiState(
    val header: RoundHeaderUi,
    /** Balances after settlement. */
    val players: List<PlayerUi>,
    val votes: Map<PlayerId, Vote>,
    val totals: VoteTotals,
    val minoritySide: Side?,
    val minorityPlayers: List<PlayerId>,
    val winners: List<PlayerId>,
    val share: Int,
    val potLeft: Int,
    val fines: Map<PlayerId, Int>,
    val promiseBreakers: List<PlayerId>,
    val newDebtors: List<PlayerId>,
    val recoveredDebtors: List<PlayerId>,
)

fun List<PlayerUi>.nameOf(id: PlayerId): String = firstOrNull { it.id == id }?.name ?: id.value

fun PublicGameView.toHeaderUi(): RoundHeaderUi = RoundHeaderUi(
    round = round,
    totalRounds = totalRounds,
    pot = pot,
    baseFine = baseFine,
    isAllIn = isAllIn,
    twist = twist,
    questionRes = questionId?.let(QuestionBank::textRes) ?: R.string.round_no_question,
)

fun PublicGameView.toPlayersUi(): List<PlayerUi> = players.map {
    PlayerUi(it.id, it.name, it.balance, it.isDebtor, it.brokenPromises, it.hasLocked)
}

fun questionUiState(view: PublicGameView, timer: TimerUi? = null) =
    QuestionUiState(view.toHeaderUi(), view.toPlayersUi(), timer)

fun negotiationUiState(view: PublicGameView, me: PrivatePlayerView, timer: TimerUi? = null, error: RuleError? = null) =
    NegotiationUiState(
        header = view.toHeaderUi(),
        me = me.playerId,
        players = view.toPlayersUi(),
        publicPromises = view.publicPromises.map { Promise(it.from, PromiseTarget.Everyone, it.side) },
        promisesMade = me.promisesMade,
        promisesReceived = me.promisesReceived,
        contracts = me.contracts,
        contractFee = view.contractFee,
        timer = timer,
        error = error,
    )

fun voteUiState(
    view: PublicGameView,
    me: PrivatePlayerView,
    doubleArmed: Boolean,
    timer: TimerUi? = null,
    error: RuleError? = null,
) = VoteUiState(
    header = view.toHeaderUi(),
    me = me.playerId,
    players = view.toPlayersUi(),
    ballot = me.ballot,
    soldTo = me.voteSoldTo,
    doubleVoteAvailable = (me.items[Item.DOUBLE_VOTE] ?: 0) > 0,
    doubleArmed = doubleArmed,
    controlled = me.controlledBallots,
    insiderTotals = me.insiderTotals,
    timer = timer,
    error = error,
)

fun flipUiState(view: PublicGameView, me: PrivatePlayerView, timer: TimerUi? = null, error: RuleError? = null) =
    FlipUiState(
        header = view.toHeaderUi(),
        me = me.playerId,
        players = view.toPlayersUi(),
        totals = view.flipWindowTotals ?: VoteTotals(0, 0),
        myVote = me.vote,
        soldTo = me.voteSoldTo,
        controlled = me.controlledVotes,
        timer = timer,
        error = error,
    )

/** Null if the view has no reveal yet (before REVEAL). */
fun revealUiState(view: PublicGameView): RevealUiState? {
    val steps = view.reveal?.steps ?: return null
    val votes = steps.filterIsInstance<RevealStep.VotesShown>().single()
    val minority = steps.filterIsInstance<RevealStep.MinorityFound>().single()
    val split = steps.filterIsInstance<RevealStep.PotSplit>().single()
    val debtors = steps.filterIsInstance<RevealStep.DebtorsChanged>().singleOrNull()
    return RevealUiState(
        header = view.toHeaderUi(),
        players = view.toPlayersUi(),
        votes = votes.votes,
        totals = steps.filterIsInstance<RevealStep.TotalsShown>().single().totals,
        minoritySide = minority.side,
        minorityPlayers = minority.players,
        winners = split.winners,
        share = split.share,
        potLeft = split.potLeft,
        fines = steps.filterIsInstance<RevealStep.FinesPaid>().single().fines,
        promiseBreakers = steps.filterIsInstance<RevealStep.PromisesBroken>().singleOrNull()?.players.orEmpty(),
        newDebtors = debtors?.newDebtors.orEmpty(),
        recoveredDebtors = debtors?.recovered.orEmpty(),
    )
}
