package com.example.minorityvault.protocol

import com.example.minorityvault.engine.ContractId
import com.example.minorityvault.engine.ContractProblem
import com.example.minorityvault.engine.ContractStatus
import com.example.minorityvault.engine.Item
import com.example.minorityvault.engine.PaymentCondition
import com.example.minorityvault.engine.Phase
import com.example.minorityvault.engine.PlayerId
import com.example.minorityvault.engine.PromiseVerdict
import com.example.minorityvault.engine.RuleError
import com.example.minorityvault.engine.SetupProblem
import com.example.minorityvault.engine.Side
import com.example.minorityvault.engine.TwistId
import com.example.minorityvault.engine.VoteChoice

/** One of every message, with fields set to non-default values so a dropped field would show. */
val clientSamples: List<ClientIntent> = listOf(
    ClientIntent.Join("Asha", playerToken = "tok-123"),
    ClientIntent.Ready,
    ClientIntent.ChatPublic("I'm voting yes. Trust me."),
    ClientIntent.ChatDm(to = "p2", text = "Between us: no."),
    ClientIntent.MakePromise(to = "p2", side = Side.NO),
    ClientIntent.MakePromise(to = null, side = Side.YES),
    ClientIntent.ContractPropose(ContractTermsDto.ShareWinnings("p1", "p2", 40)),
    ClientIntent.ContractPropose(ContractTermsDto.FixedPayment("p1", "p3", 15, PaymentCondition.IF_TO_WINS)),
    ClientIntent.ContractPropose(ContractTermsDto.VoteSale("p4", "p1", 20)),
    ClientIntent.ContractRespond(contractId = 7, accept = true),
    ClientIntent.VoteSelect(Side.YES, forPlayer = "p4"),
    ClientIntent.VoteLock(forPlayer = "p4"),
    ClientIntent.VoteUnlock(),
    ClientIntent.VoteFlip(forPlayer = "p4"),
    ClientIntent.UseItem(Item.DOUBLE_VOTE),
    ClientIntent.Heartbeat(sentAt = 1_700_000_000_123),
)

private val summary = RoundSummaryDto(
    round = 2,
    questionId = "table_liar",
    twist = TwistDto.Insider("p3"),
    votes = mapOf("p1" to VoteDto(VoteChoice.YES, doubled = true), "p2" to VoteDto(VoteChoice.ABSTAIN)),
    totals = VoteTotalsDto(yes = 2, no = 0),
    minoritySide = null,
    minorityPlayers = emptyList(),
    winners = emptyList(),
    fines = mapOf("p1" to 10, "p2" to 10),
    winnings = emptyMap(),
    contracts = listOf(ContractDto(1, "p1", ContractTermsDto.VoteSale("p2", "p1", 5), ContractStatus.SIGNED)),
    transfers = listOf(ContractTransferDto(1, "p1", "p2", owed = 5, paid = 3)),
    promises = listOf(JudgedPromiseDto(PromiseDto("p1", "p2", Side.NO), PromiseVerdict.BROKEN)),
    newDebtors = listOf("p2"),
    recoveredDebtors = emptyList(),
    potBefore = 30,
    potAfter = 50,
    balancesBefore = mapOf("p1" to 90, "p2" to 13),
    balancesAfter = mapOf("p1" to 75, "p2" to 0),
)

private val reveal = RevealSequenceDto(
    listOf(
        RevealStepDto.VotesShown(mapOf("p1" to VoteDto(VoteChoice.NO, flipped = true))),
        RevealStepDto.TotalsShown(VoteTotalsDto(1, 3)),
        RevealStepDto.MinorityFound(Side.NO, listOf("p1")),
        RevealStepDto.TwistResolved(TwistInfoDto(TwistId.EXACT_MINORITY, exactMinoritySize = 1), listOf("p1")),
        RevealStepDto.FinesPaid(mapOf("p2" to 10)),
        RevealStepDto.PotSplit(listOf("p1"), share = 30, potLeft = 0),
        RevealStepDto.PromisesBroken(listOf("p2", "p2")),
        RevealStepDto.DebtorsChanged(listOf("p3"), listOf("p4")),
        RevealStepDto.Settled(mapOf("p1" to 130), pot = 0, previous = mapOf("p1" to 100)),
    ),
)

val hostSamples: List<HostEvent> = listOf(
    HostEvent.Welcome(playerId = "p2", playerToken = "tok-abc", roomCode = "KTRW"),
    HostEvent.LobbyUpdate(
        roomCode = "KTRW",
        players = listOf(
            LobbyPlayerDto("p1", "Asha", isHost = true, isBot = false, connected = true),
            LobbyPlayerDto("p2", "Bot 1", isHost = false, isBot = true, connected = true),
            LobbyPlayerDto("p3", "Ben", isHost = false, isBot = false, connected = false),
        ),
        minPlayers = 4,
        maxPlayers = 8,
        started = true,
    ),
    HostEvent.PhaseStarted(round = 3, phase = WirePhase.NEGOTIATION, endsAt = 1_700_000_150_000),
    HostEvent.PublicView(
        PublicGameViewDto(
            phase = WirePhase.FLIP_WINDOW,
            round = 3,
            totalRounds = 10,
            questionId = "dreaming",
            twist = TwistInfoDto(TwistId.SECOND_CHANCE),
            isAllIn = false,
            baseFine = 10,
            contractFee = 5,
            pot = 40,
            players = listOf(PublicPlayerDto("p1", "Asha", false, 95, false, 1, hasLocked = true)),
            publicPromises = listOf(PublicPromiseDto("p1", Side.YES)),
            flipWindowTotals = VoteTotalsDto(3, 2),
            reveal = reveal,
            winners = listOf("p1"),
            finalHistory = listOf(summary),
        ),
    ),
    HostEvent.PrivateView(
        PrivatePlayerViewDto(
            playerId = "p1",
            items = mapOf(Item.DOUBLE_VOTE to 1),
            ballot = BallotDto(Side.NO, doubled = true, locked = true),
            vote = VoteDto(VoteChoice.YES),
            voteSoldTo = "p3",
            controlledBallots = mapOf("p4" to BallotDto(Side.YES)),
            controlledVotes = mapOf("p4" to VoteDto(VoteChoice.NO)),
            isInsider = true,
            insiderTotals = VoteTotalsDto(2, 1),
            contracts = listOf(
                ContractDto(2, "p1", ContractTermsDto.ShareWinnings("p1", "p2", 50), ContractStatus.PENDING),
            ),
            promisesMade = listOf(PromiseDto("p1", null, Side.YES)),
            promisesReceived = listOf(PromiseDto("p2", "p1", Side.NO)),
            transfers = listOf(ContractTransferDto(2, "p1", "p2", 10, 10)),
            judgedPromises = listOf(JudgedPromiseDto(PromiseDto("p1", null, Side.YES), PromiseVerdict.KEPT)),
        ),
    ),
    HostEvent.ChatMessage(id = 41, from = "p2", to = "p1", text = "Deal?", sentAt = 1_700_000_000_500),
    HostEvent.ChatMessage(id = 42, from = "p1", promise = PromiseDto("p1", null, Side.NO), sentAt = 1_700_000_000_900),
    HostEvent.ContractUpdate(
        ContractDto(
            id = 3,
            proposer = "p2",
            terms = ContractTermsDto.FixedPayment("p2", "p1", 8, PaymentCondition.ALWAYS),
            status = ContractStatus.DECLINED,
        ),
    ),
    HostEvent.KeeperLine("Somewhere, ink is drying."),
    HostEvent.RevealSequence(round = 3, sequence = reveal),
    HostEvent.RoundSettled(round = 3, balances = mapOf("p1" to 130, "p2" to 0), pot = 4),
    HostEvent.GameOver(winners = listOf("p1", "p3"), history = listOf(summary)),
    HostEvent.Error(ErrorCode.NAME_TAKEN),
    HostEvent.Error(ErrorCode.RULE, RuleErrorDto(RuleErrorCode.CANNOT_AFFORD_FEE, player = "p2")),
    HostEvent.HeartbeatReply(clientSentAt = 1_700_000_000_123, hostTime = 1_700_000_004_567),
)

/** Every engine rule error, so the wire mapping can't silently drop one. */
val ruleErrorSamples: List<RuleError> = listOf(
    RuleError.WrongPhase(Phase.Vote),
    RuleError.StalePhase(Phase.Negotiation, Phase.Vote),
    RuleError.UnknownPlayer(PlayerId("p9")),
    RuleError.InvalidSetup(SetupProblem.NOT_ENOUGH_QUESTIONS),
    RuleError.TwistNotOffered,
    RuleError.VoteIsLocked,
    RuleError.NotLocked,
    RuleError.NothingSelected,
    RuleError.VoteSold,
    RuleError.NotYourVote,
    RuleError.AlreadyFlipped,
    RuleError.CannotFlipAbstain,
    RuleError.NoItemLeft(Item.DOUBLE_VOTE),
    RuleError.ItemAlreadyApplied(Item.DOUBLE_VOTE),
    RuleError.InvalidContract(ContractProblem.DEBTOR_ALREADY_SOLD),
    RuleError.ContractNotFound(ContractId(4)),
    RuleError.ContractNotPending(ContractId(5)),
    RuleError.NotCounterparty,
    RuleError.CannotAffordFee(PlayerId("p2")),
    RuleError.PromiseToSelf,
)
