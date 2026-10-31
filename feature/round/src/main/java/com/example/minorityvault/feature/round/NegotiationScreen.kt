package com.example.minorityvault.feature.round

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.minorityvault.designsystem.VaultPreviewTheme
import com.example.minorityvault.designsystem.VaultSpacing
import com.example.minorityvault.designsystem.VaultTheme
import com.example.minorityvault.designsystem.component.ChatBubble
import com.example.minorityvault.designsystem.component.ContractCard
import com.example.minorityvault.designsystem.component.PaperCard
import com.example.minorityvault.designsystem.component.VaultButton
import com.example.minorityvault.designsystem.preview.ScreenPreviews
import com.example.minorityvault.engine.Contract
import com.example.minorityvault.engine.ContractId
import com.example.minorityvault.engine.ContractStatus
import com.example.minorityvault.engine.ContractTerms
import com.example.minorityvault.engine.PaymentCondition
import com.example.minorityvault.engine.Promise
import com.example.minorityvault.engine.PromiseTarget
import com.example.minorityvault.engine.Side

/** Deals and promises, in three tabs. [onFinishTurn] is set only in pass-the-phone play. */
@Composable
fun NegotiationScreen(
    state: NegotiationUiState,
    onAction: (RoundAction) -> Unit,
    onFinishTurn: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var tab by rememberSaveable(state.me) { mutableIntStateOf(0) }
    RoundScaffold(
        header = state.header,
        title = stringResource(R.string.negotiation_turn, state.players.nameOf(state.me)),
        timer = state.timer,
        modifier = modifier,
        bottomBar = onFinishTurn?.let {
            { VaultButton(stringResource(R.string.done_pass_phone), onClick = it, modifier = Modifier.fillMaxWidth()) }
        },
    ) {
        // The question stays in view: it is what the table argues about while it schemes.
        PaperCard(Modifier.fillMaxWidth()) {
            Text(stringResource(state.header.questionRes), style = MaterialTheme.typography.titleLarge)
        }
        ErrorBanner(state.error)
        PrimaryTabRow(
            selectedTabIndex = tab,
            containerColor = Color.Transparent,
            contentColor = VaultTheme.colors.brass,
        ) {
            listOf(R.string.tab_lobby, R.string.tab_messages, R.string.tab_contracts).forEachIndexed { index, label ->
                Tab(
                    selected = tab == index,
                    onClick = { tab = index },
                    text = { Text(stringResource(label), style = MaterialTheme.typography.titleSmall) },
                    unselectedContentColor = VaultTheme.colors.onPanelMuted,
                )
            }
        }
        when (tab) {
            0 -> LobbyTab(state)
            1 -> PromisesTab(state, onAction)
            else -> ContractsTab(state, onAction)
        }
    }
}

@Composable
private fun LobbyTab(state: NegotiationUiState) {
    PlayerGrid(state.players, highlight = state.me)
    SectionTitle(stringResource(R.string.lobby_public_promises))
    if (state.publicPromises.isEmpty()) {
        BodyText(stringResource(R.string.lobby_no_public_promises))
    } else {
        PromiseList(state.publicPromises, state)
    }
}

@Composable
private fun PromisesTab(state: NegotiationUiState, onAction: (RoundAction) -> Unit) {
    SectionTitle(stringResource(R.string.promises_received))
    if (state.promisesReceived.isEmpty()) BodyText(stringResource(R.string.promises_none))
    PromiseList(state.promisesReceived, state)
    SectionTitle(stringResource(R.string.promises_made))
    if (state.promisesMade.isEmpty()) BodyText(stringResource(R.string.promises_none))
    PromiseList(state.promisesMade, state)
    PromiseComposer(state.me, state.players) { to, side -> onAction(RoundAction.MakePromise(to, side)) }
}

@Composable
private fun PromiseList(promises: List<Promise>, state: NegotiationUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(VaultSpacing.sm)) {
        promises.forEach { promise ->
            val mine = promise.from == state.me
            val sender = when {
                !mine -> state.players.nameOf(promise.from)

                promise.to is PromiseTarget.ToPlayer ->
                    stringResource(R.string.promise_to, state.players.nameOf((promise.to as PromiseTarget.ToPlayer).id))

                else -> stringResource(R.string.promise_to, stringResource(R.string.everyone))
            }
            ChatBubble(
                text = stringResource(R.string.promise_line, sideLabel(promise.side)),
                mine = mine,
                sender = sender,
                isPromise = true,
            )
        }
    }
}

@Composable
private fun ContractsTab(state: NegotiationUiState, onAction: (RoundAction) -> Unit) {
    if (state.contracts.isEmpty()) BodyText(stringResource(R.string.contracts_none))
    state.contracts.forEach { contract -> ContractItem(contract, state, onAction) }
    ContractComposer(state.me, state.players) { onAction(RoundAction.Propose(it)) }
}

@Composable
private fun ContractItem(contract: Contract, state: NegotiationUiState, onAction: (RoundAction) -> Unit) {
    val mine = contract.proposer == state.me
    val canRespond = contract.status == ContractStatus.PENDING && contract.counterparty == state.me
    val buyerPays = contract.terms is ContractTerms.VoteSale && contract.terms.feePayer != state.me
    ContractCard(
        title = if (mine) {
            stringResource(R.string.contract_to, state.players.nameOf(contract.counterparty))
        } else {
            stringResource(R.string.contract_from, state.players.nameOf(contract.proposer))
        },
        terms = contractTermsText(contract.terms, state.players),
        status = stringResource(
            when (contract.status) {
                ContractStatus.PENDING ->
                    if (canRespond) R.string.contract_status_pending_you else R.string.contract_status_pending_them

                ContractStatus.SIGNED -> R.string.contract_status_signed

                ContractStatus.DECLINED -> R.string.contract_status_declined

                ContractStatus.EXPIRED -> R.string.contract_status_expired
            },
        ),
        note = if (canRespond) {
            stringResource(if (buyerPays) R.string.contract_fee_buyer else R.string.contract_fee_you, state.contractFee)
        } else {
            null
        },
        onSign = if (canRespond) ({ onAction(RoundAction.Respond(contract.id, accept = true)) }) else null,
        onDecline = if (canRespond) ({ onAction(RoundAction.Respond(contract.id, accept = false)) }) else null,
    )
}

@ScreenPreviews
@Composable
private fun NegotiationScreenPreview() {
    val state = NegotiationUiState(
        header = PreviewData.header,
        me = PreviewData.asha,
        players = PreviewData.players,
        publicPromises = listOf(Promise(PreviewData.ben, PromiseTarget.Everyone, Side.NO)),
        promisesMade = emptyList(),
        promisesReceived = listOf(Promise(PreviewData.dev, PromiseTarget.ToPlayer(PreviewData.asha), Side.YES)),
        contracts = listOf(
            Contract(
                ContractId(1),
                proposer = PreviewData.ben,
                terms = ContractTerms.FixedPayment(PreviewData.ben, PreviewData.asha, 10, PaymentCondition.ALWAYS),
            ),
        ),
        contractFee = 5,
    )
    VaultPreviewTheme {
        NegotiationScreen(state, onAction = {}, onFinishTurn = {})
    }
}
