package com.example.minorityvault.feature.round

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.minorityvault.engine.ContractProblem
import com.example.minorityvault.engine.ContractTerms
import com.example.minorityvault.engine.PaymentCondition
import com.example.minorityvault.engine.PlayerId
import com.example.minorityvault.engine.RuleError
import com.example.minorityvault.engine.Side
import com.example.minorityvault.engine.TwistId
import com.example.minorityvault.engine.TwistInfo
import com.example.minorityvault.engine.Vote
import com.example.minorityvault.engine.VoteChoice

@Composable
fun sideLabel(side: Side): String = stringResource(if (side == Side.YES) R.string.side_yes else R.string.side_no)

@Composable
fun voteLabel(vote: Vote): String {
    val label = when (vote.choice) {
        VoteChoice.YES -> stringResource(R.string.side_yes)
        VoteChoice.NO -> stringResource(R.string.side_no)
        VoteChoice.ABSTAIN -> stringResource(R.string.side_abstain)
    }
    return if (vote.doubled) stringResource(R.string.reveal_vote_double, label) else label
}

@Composable
fun twistTitle(twist: TwistInfo): String = when (twist.id) {
    TwistId.NO_TWIST -> stringResource(R.string.twist_none_title)
    TwistId.EXACT_MINORITY -> stringResource(R.string.twist_exact_title, twist.exactMinoritySize ?: 0)
    TwistId.SECOND_CHANCE -> stringResource(R.string.twist_second_chance_title)
    TwistId.INSIDER -> stringResource(R.string.twist_insider_title)
    TwistId.ROBIN_HOOD -> stringResource(R.string.twist_robin_hood_title)
}

/** The Vault Keeper's announcement of the round's twist. */
@Composable
fun twistKeeperLine(twist: TwistInfo): String = when (twist.id) {
    TwistId.NO_TWIST -> stringResource(R.string.twist_none_line)

    TwistId.EXACT_MINORITY -> {
        val size = twist.exactMinoritySize ?: 0
        pluralStringResource(R.plurals.twist_exact_line, size, size)
    }

    TwistId.SECOND_CHANCE -> stringResource(R.string.twist_second_chance_line)

    TwistId.INSIDER -> stringResource(R.string.twist_insider_line)

    TwistId.ROBIN_HOOD -> stringResource(R.string.twist_robin_hood_line)
}

/** Contract terms in plain words, e.g. "Asha pays Ben 10 coins no matter what". */
@Composable
fun contractTermsText(terms: ContractTerms, players: List<PlayerUi>): String {
    fun name(id: PlayerId) = players.nameOf(id)
    return when (terms) {
        is ContractTerms.ShareWinnings ->
            stringResource(R.string.terms_share, name(terms.from), name(terms.to), terms.percent)

        is ContractTerms.FixedPayment -> stringResource(
            when (terms.condition) {
                PaymentCondition.IF_FROM_WINS -> R.string.terms_fixed_if_from_wins
                PaymentCondition.IF_TO_WINS -> R.string.terms_fixed_if_to_wins
                PaymentCondition.ALWAYS -> R.string.terms_fixed_always
            },
            name(terms.from),
            name(terms.to),
            terms.amount,
        )

        is ContractTerms.VoteSale ->
            stringResource(R.string.terms_vote_sale, name(terms.debtor), name(terms.buyer), terms.price)
    }
}

@Composable
fun ruleErrorText(error: RuleError): String = stringResource(
    when (error) {
        is RuleError.WrongPhase, is RuleError.StalePhase -> R.string.error_wrong_phase
        is RuleError.UnknownPlayer -> R.string.error_unknown_player
        RuleError.VoteIsLocked -> R.string.error_vote_locked
        RuleError.NotLocked -> R.string.error_not_locked
        RuleError.NothingSelected -> R.string.error_nothing_selected
        RuleError.VoteSold -> R.string.error_vote_sold
        RuleError.NotYourVote -> R.string.error_not_your_vote
        RuleError.AlreadyFlipped -> R.string.error_already_flipped
        RuleError.CannotFlipAbstain -> R.string.error_cannot_flip_abstain
        is RuleError.NoItemLeft -> R.string.error_no_item
        is RuleError.ItemAlreadyApplied -> R.string.error_item_applied
        is RuleError.InvalidContract -> contractProblemText(error.problem)
        is RuleError.ContractNotFound -> R.string.error_contract_not_found
        is RuleError.ContractNotPending -> R.string.error_contract_not_pending
        RuleError.NotCounterparty -> R.string.error_not_counterparty
        is RuleError.CannotAffordFee -> R.string.error_cannot_afford_fee
        RuleError.PromiseToSelf -> R.string.error_promise_self
        is RuleError.InvalidSetup, RuleError.TwistNotOffered -> R.string.error_generic
    },
)

private fun contractProblemText(problem: ContractProblem): Int = when (problem) {
    ContractProblem.SAME_PLAYER -> R.string.error_contract_same_player
    ContractProblem.NOT_A_PARTY -> R.string.error_contract_not_party
    ContractProblem.PERCENT_OUT_OF_RANGE -> R.string.error_contract_percent
    ContractProblem.AMOUNT_NOT_POSITIVE -> R.string.error_contract_amount
    ContractProblem.NOT_A_DEBTOR -> R.string.error_contract_not_debtor
    ContractProblem.DEBTOR_ALREADY_SOLD -> R.string.error_contract_already_sold
}
