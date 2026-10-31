package com.example.minorityvault.feature.round

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.minorityvault.designsystem.GlassSurface
import com.example.minorityvault.designsystem.VaultPreviewTheme
import com.example.minorityvault.designsystem.VaultSpacing
import com.example.minorityvault.designsystem.VaultTheme
import com.example.minorityvault.designsystem.component.VaultButton
import com.example.minorityvault.designsystem.preview.ComponentPreviews
import com.example.minorityvault.engine.ContractTerms
import com.example.minorityvault.engine.PaymentCondition
import com.example.minorityvault.engine.PlayerId
import com.example.minorityvault.engine.PromiseTarget
import com.example.minorityvault.engine.Side

/** Builds a structured promise: to whom, and which side. Draft state is local until sent. */
@Composable
internal fun PromiseComposer(me: PlayerId, players: List<PlayerUi>, onPromise: (PromiseTarget, Side) -> Unit) {
    var target by remember(me) { mutableStateOf<PromiseTarget>(PromiseTarget.Everyone) }
    var side by remember(me) { mutableStateOf<Side?>(null) }
    val targets = listOf(Choice<PromiseTarget>(PromiseTarget.Everyone, stringResource(R.string.everyone))) +
        players.filter { it.id != me }.map { Choice<PromiseTarget>(PromiseTarget.ToPlayer(it.id), it.name) }
    ComposerPanel(stringResource(R.string.promise_compose_title)) {
        BodyText(stringResource(R.string.promise_compose_hint))
        SectionTitle(stringResource(R.string.promise_compose_to))
        ChoiceGroup(targets, target, onSelect = { target = it })
        SectionTitle(stringResource(R.string.promise_compose_side))
        ChoiceGroup(Side.entries.map { Choice(it, sideLabel(it)) }, side, onSelect = { side = it })
        VaultButton(
            stringResource(R.string.promise_compose_send),
            onClick = { side?.let { onPromise(target, it) } },
            enabled = side != null,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private enum class Template(
    val label: Int,
    val amountLabel: Int,
    val default: Int,
    val step: Int,
    val min: Int,
    val max: Int,
) {
    SHARE(R.string.template_share, R.string.contract_compose_percent, default = 50, step = 10, min = 10, max = 100),
    FIXED(R.string.template_fixed, R.string.contract_compose_amount, default = 10, step = 5, min = 5, max = 100),
    VOTE_SALE(R.string.template_vote_sale, R.string.contract_compose_price, default = 10, step = 5, min = 5, max = 100),
}

/** A contract being put together, before it's proposed. */
private data class ContractDraft(
    val template: Template = Template.FIXED,
    val counterparty: PlayerId? = null,
    val iPay: Boolean = true,
    val amount: Int = Template.FIXED.default,
    val condition: PaymentCondition = PaymentCondition.ALWAYS,
) {
    /** The structured terms, or null if the draft can't be proposed yet. */
    fun terms(me: PlayerId, players: List<PlayerUi>): ContractTerms? = counterparty?.let { other ->
        val (from, to) = if (iPay) me to other else other to me
        val isDebtor = { id: PlayerId -> players.firstOrNull { it.id == id }?.isDebtor == true }
        when (template) {
            Template.SHARE -> ContractTerms.ShareWinnings(from, to, amount)

            Template.FIXED -> ContractTerms.FixedPayment(from, to, amount, condition)

            Template.VOTE_SALE -> when {
                isDebtor(me) -> ContractTerms.VoteSale(debtor = me, buyer = other, price = amount)
                isDebtor(other) -> ContractTerms.VoteSale(debtor = other, buyer = me, price = amount)
                else -> null
            }
        }
    }
}

/** Builds contract terms from a template, never free text. */
@Composable
internal fun ContractComposer(me: PlayerId, players: List<PlayerUi>, onPropose: (ContractTerms) -> Unit) {
    var draft by remember(me) { mutableStateOf(ContractDraft()) }
    val terms = draft.terms(me, players)
    ComposerPanel(stringResource(R.string.contract_compose_title)) {
        SectionTitle(stringResource(R.string.contract_compose_with))
        ChoiceGroup(
            players.filter { it.id != me }.map { Choice(it.id, it.name) },
            draft.counterparty,
            onSelect = { draft = draft.copy(counterparty = it) },
        )
        SectionTitle(stringResource(R.string.contract_compose_type))
        ChoiceGroup(
            Template.entries.map { Choice(it, stringResource(it.label)) },
            draft.template,
            onSelect = { draft = draft.copy(template = it, amount = it.default) },
        )
        DraftOptions(draft, onChange = { draft = it })
        Text(
            when {
                terms != null -> contractTermsText(terms, players)
                draft.counterparty == null -> stringResource(R.string.contract_compose_pick_player)
                else -> stringResource(R.string.contract_compose_vote_sale_unavailable)
            },
            style = MaterialTheme.typography.titleMedium,
            color = if (terms != null) VaultTheme.colors.onPanel else VaultTheme.colors.onPanelMuted,
        )
        VaultButton(
            stringResource(R.string.contract_compose_send),
            onClick = { terms?.let(onPropose) },
            enabled = terms != null,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Who pays, when, and how much: only the choices that apply to the draft's template. */
@Composable
private fun DraftOptions(draft: ContractDraft, onChange: (ContractDraft) -> Unit) {
    val template = draft.template
    if (template != Template.VOTE_SALE) {
        SectionTitle(stringResource(R.string.contract_compose_who_pays))
        ChoiceGroup(
            listOf(
                Choice(true, stringResource(R.string.contract_compose_i_pay)),
                Choice(false, stringResource(R.string.contract_compose_they_pay)),
            ),
            draft.iPay,
            onSelect = { onChange(draft.copy(iPay = it)) },
        )
    }
    if (template == Template.FIXED) {
        SectionTitle(stringResource(R.string.contract_compose_when))
        ChoiceGroup(
            listOf(
                Choice(PaymentCondition.ALWAYS, stringResource(R.string.condition_always)),
                Choice(PaymentCondition.IF_FROM_WINS, stringResource(R.string.condition_if_from_wins)),
                Choice(PaymentCondition.IF_TO_WINS, stringResource(R.string.condition_if_to_wins)),
            ),
            draft.condition,
            onSelect = { onChange(draft.copy(condition = it)) },
        )
    }
    SectionTitle(stringResource(template.amountLabel))
    Stepper(
        value = if (template == Template.SHARE) "${draft.amount}%" else "${draft.amount}",
        onDecrease = { onChange(draft.copy(amount = (draft.amount - template.step).coerceAtLeast(template.min))) },
        onIncrease = { onChange(draft.copy(amount = (draft.amount + template.step).coerceAtMost(template.max))) },
    )
}

@Composable
private fun ComposerPanel(title: String, content: @Composable () -> Unit) {
    GlassSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(VaultSpacing.lg), verticalArrangement = Arrangement.spacedBy(VaultSpacing.sm)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            content()
        }
    }
}

@ComponentPreviews
@Composable
private fun ComposersPreview() {
    VaultPreviewTheme {
        Column(Modifier.padding(VaultSpacing.lg), verticalArrangement = Arrangement.spacedBy(VaultSpacing.lg)) {
            PromiseComposer(PreviewData.asha, PreviewData.players, onPromise = { _, _ -> })
            ContractComposer(PreviewData.asha, PreviewData.players, onPropose = {})
        }
    }
}
