package com.example.minorityvault.protocol

import com.example.minorityvault.engine.ContractStatus
import com.example.minorityvault.engine.Item
import com.example.minorityvault.engine.PaymentCondition
import com.example.minorityvault.engine.PromiseVerdict
import com.example.minorityvault.engine.Side
import com.example.minorityvault.engine.TwistId
import com.example.minorityvault.engine.VoteChoice
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Wire copies of the engine's view types. The engine stays free of serialization, and the wire format only
// changes when this file does. Player ids travel as plain strings; see Mapping.kt for the conversions.

@Serializable
enum class WirePhase { LOBBY, QUESTION, NEGOTIATION, VOTE, FLIP_WINDOW, REVEAL, SETTLEMENT, GAME_OVER }

@Serializable
data class PublicPlayerDto(
    val id: String,
    val name: String,
    val isBot: Boolean,
    val balance: Int,
    val isDebtor: Boolean,
    val brokenPromises: Int,
    val hasLocked: Boolean,
)

@Serializable
data class PublicPromiseDto(val from: String, val side: Side)

@Serializable
data class TwistInfoDto(val id: TwistId, val exactMinoritySize: Int? = null)

@Serializable
data class VoteTotalsDto(val yes: Int, val no: Int)

@Serializable
data class BallotDto(val selection: Side? = null, val doubled: Boolean = false, val locked: Boolean = false)

@Serializable
data class VoteDto(val choice: VoteChoice, val doubled: Boolean = false, val flipped: Boolean = false)

@Serializable
sealed interface ContractTermsDto {
    @Serializable
    @SerialName("ShareWinnings")
    data class ShareWinnings(val from: String, val to: String, val percent: Int) : ContractTermsDto

    @Serializable
    @SerialName("FixedPayment")
    data class FixedPayment(val from: String, val to: String, val amount: Int, val condition: PaymentCondition) :
        ContractTermsDto

    @Serializable
    @SerialName("VoteSale")
    data class VoteSale(val debtor: String, val buyer: String, val price: Int) : ContractTermsDto
}

@Serializable
data class ContractDto(val id: Int, val proposer: String, val terms: ContractTermsDto, val status: ContractStatus)

@Serializable
data class ContractTransferDto(val contractId: Int, val from: String, val to: String, val owed: Int, val paid: Int)

/** [to] is a player id, or null for everyone. */
@Serializable
data class PromiseDto(val from: String, val to: String? = null, val side: Side)

@Serializable
data class JudgedPromiseDto(val promise: PromiseDto, val verdict: PromiseVerdict)

/** The full twist, Insider included: only sent in the GAME_OVER history. */
@Serializable
sealed interface TwistDto {
    @Serializable
    @SerialName("ExactMinority")
    data class ExactMinority(val size: Int) : TwistDto

    @Serializable
    @SerialName("SecondChance")
    data object SecondChance : TwistDto

    @Serializable
    @SerialName("Insider")
    data class Insider(val player: String) : TwistDto

    @Serializable
    @SerialName("RobinHood")
    data object RobinHood : TwistDto

    @Serializable
    @SerialName("NoTwist")
    data object NoTwist : TwistDto
}

@Serializable
sealed interface RevealStepDto {
    @Serializable
    @SerialName("VotesShown")
    data class VotesShown(val votes: Map<String, VoteDto>) : RevealStepDto

    @Serializable
    @SerialName("TotalsShown")
    data class TotalsShown(val totals: VoteTotalsDto) : RevealStepDto

    @Serializable
    @SerialName("MinorityFound")
    data class MinorityFound(val side: Side? = null, val players: List<String>) : RevealStepDto

    @Serializable
    @SerialName("TwistResolved")
    data class TwistResolved(val twist: TwistInfoDto, val winners: List<String>) : RevealStepDto

    @Serializable
    @SerialName("FinesPaid")
    data class FinesPaid(val fines: Map<String, Int>) : RevealStepDto

    @Serializable
    @SerialName("PotSplit")
    data class PotSplit(val winners: List<String>, val share: Int, val potLeft: Int) : RevealStepDto

    @Serializable
    @SerialName("PromisesBroken")
    data class PromisesBroken(val players: List<String>) : RevealStepDto

    @Serializable
    @SerialName("DebtorsChanged")
    data class DebtorsChanged(val newDebtors: List<String>, val recovered: List<String>) : RevealStepDto

    @Serializable
    @SerialName("Settled")
    data class Settled(val balances: Map<String, Int>, val pot: Int, val previous: Map<String, Int>) : RevealStepDto
}

@Serializable
data class RevealSequenceDto(val steps: List<RevealStepDto>)

@Serializable
data class RoundSummaryDto(
    val round: Int,
    val questionId: String,
    val twist: TwistDto,
    val votes: Map<String, VoteDto>,
    val totals: VoteTotalsDto,
    val minoritySide: Side? = null,
    val minorityPlayers: List<String>,
    val winners: List<String>,
    val fines: Map<String, Int>,
    val winnings: Map<String, Int>,
    val contracts: List<ContractDto>,
    val transfers: List<ContractTransferDto>,
    val promises: List<JudgedPromiseDto>,
    val newDebtors: List<String>,
    val recoveredDebtors: List<String>,
    val potBefore: Int,
    val potAfter: Int,
    val balancesBefore: Map<String, Int>,
    val balancesAfter: Map<String, Int>,
)

@Serializable
data class PublicGameViewDto(
    val phase: WirePhase,
    val round: Int,
    val totalRounds: Int,
    val questionId: String? = null,
    val twist: TwistInfoDto? = null,
    val isAllIn: Boolean,
    val baseFine: Int,
    val contractFee: Int,
    val pot: Int,
    val players: List<PublicPlayerDto>,
    val publicPromises: List<PublicPromiseDto>,
    val flipWindowTotals: VoteTotalsDto? = null,
    val reveal: RevealSequenceDto? = null,
    val winners: List<String>? = null,
    val finalHistory: List<RoundSummaryDto>? = null,
)

@Serializable
data class PrivatePlayerViewDto(
    val playerId: String,
    val items: Map<Item, Int>,
    val ballot: BallotDto? = null,
    val vote: VoteDto? = null,
    val voteSoldTo: String? = null,
    val controlledBallots: Map<String, BallotDto>,
    val controlledVotes: Map<String, VoteDto>,
    val isInsider: Boolean,
    val insiderTotals: VoteTotalsDto? = null,
    val contracts: List<ContractDto>,
    val promisesMade: List<PromiseDto>,
    val promisesReceived: List<PromiseDto>,
    val transfers: List<ContractTransferDto>,
    val judgedPromises: List<JudgedPromiseDto>,
)

/** A [com.example.minorityvault.engine.RuleError] flattened: [code] says which, the other fields carry its data. */
@Serializable
data class RuleErrorDto(
    val code: RuleErrorCode,
    val phase: WirePhase? = null,
    val expected: WirePhase? = null,
    val player: String? = null,
    val contractId: Int? = null,
    val item: Item? = null,
    val problem: String? = null,
)

@Serializable
enum class RuleErrorCode {
    WRONG_PHASE,
    STALE_PHASE,
    UNKNOWN_PLAYER,
    INVALID_SETUP,
    TWIST_NOT_OFFERED,
    VOTE_IS_LOCKED,
    NOT_LOCKED,
    NOTHING_SELECTED,
    VOTE_SOLD,
    NOT_YOUR_VOTE,
    ALREADY_FLIPPED,
    CANNOT_FLIP_ABSTAIN,
    NO_ITEM_LEFT,
    ITEM_ALREADY_APPLIED,
    INVALID_CONTRACT,
    CONTRACT_NOT_FOUND,
    CONTRACT_NOT_PENDING,
    NOT_COUNTERPARTY,
    CANNOT_AFFORD_FEE,
    PROMISE_TO_SELF,
}
