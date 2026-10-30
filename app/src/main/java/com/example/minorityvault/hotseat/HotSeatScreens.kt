package com.example.minorityvault.hotseat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.example.minorityvault.R
import com.example.minorityvault.designsystem.MinorityVaultTheme
import com.example.minorityvault.designsystem.VaultSpacing
import com.example.minorityvault.designsystem.VaultTheme
import com.example.minorityvault.designsystem.component.KeeperBubble
import com.example.minorityvault.designsystem.component.VaultButton
import com.example.minorityvault.designsystem.component.VaultButtonStyle
import com.example.minorityvault.designsystem.preview.ScreenPreviews
import com.example.minorityvault.engine.Phase

@Composable
fun HotSeatSetupScreen(state: HotSeatUiState.Setup, onEvent: (HotSeatEvent) -> Unit, modifier: Modifier = Modifier) {
    val colors = VaultTheme.colors
    FullScreen(modifier.imePadding(), bottom = {
        if (!state.canStart) {
            Text(
                stringResource(R.string.setup_names_hint),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onPanelMuted,
            )
        }
        VaultButton(
            stringResource(R.string.setup_start),
            onClick = { onEvent(HotSeatEvent.Start) },
            enabled = state.canStart,
            modifier = Modifier.fillMaxWidth(),
        )
    }) {
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.displayMedium,
            color = colors.brass,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            stringResource(R.string.setup_tagline),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.onPanel,
        )
        Text(
            stringResource(R.string.setup_mode),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onPanelMuted,
        )
        state.names.forEachIndexed { index, name ->
            NameRow(index, name, canRemove = state.canRemove, onEvent = onEvent)
        }
        if (state.canAdd) {
            VaultButton(
                stringResource(R.string.setup_add_player),
                onClick = { onEvent(HotSeatEvent.AddPlayer) },
                style = VaultButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun NameRow(index: Int, name: String, canRemove: Boolean, onEvent: (HotSeatEvent) -> Unit) {
    val colors = VaultTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VaultSpacing.sm)) {
        OutlinedTextField(
            value = name,
            onValueChange = { onEvent(HotSeatEvent.SetName(index, it)) },
            label = { Text(stringResource(R.string.setup_player_label, index + 1)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.brass,
                unfocusedBorderColor = colors.steel,
                focusedLabelColor = colors.brass,
                unfocusedLabelColor = colors.onPanelMuted,
                cursorColor = colors.brass,
                focusedTextColor = colors.onPanel,
                unfocusedTextColor = colors.onPanel,
            ),
            modifier = Modifier.weight(1f),
        )
        if (canRemove) {
            val description = stringResource(R.string.setup_remove_player, index + 1)
            VaultButton(
                "✕",
                onClick = { onEvent(HotSeatEvent.RemovePlayer(index)) },
                style = VaultButtonStyle.Secondary,
                modifier = Modifier.semantics { contentDescription = description },
            )
        }
    }
}

/** Hides the last player's screen until the next player has the phone. */
@Composable
fun PassPhoneScreen(state: HotSeatUiState.PassPhone, onShow: () -> Unit, modifier: Modifier = Modifier) {
    val colors = VaultTheme.colors
    FullScreen(modifier, verticalCenter = true, bottom = {
        VaultButton(
            stringResource(R.string.pass_phone_show, state.name),
            onClick = onShow,
            modifier = Modifier.fillMaxWidth(),
        )
    }) {
        Text(
            stringResource(R.string.pass_phone_title, state.name),
            style = MaterialTheme.typography.displaySmall,
            color = colors.brass,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            stringResource(
                when (state.phase) {
                    Phase.Vote -> R.string.pass_phone_vote
                    Phase.FlipWindow -> R.string.pass_phone_flip
                    else -> R.string.pass_phone_negotiation
                },
            ),
            style = MaterialTheme.typography.titleLarge,
            color = colors.onPanel,
        )
        Text(
            stringResource(R.string.pass_phone_privacy),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onPanelMuted,
        )
    }
}

@Composable
fun NegotiationPassDoneScreen(
    state: HotSeatUiState.NegotiationPassDone,
    onEvent: (HotSeatEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    FullScreen(modifier, verticalCenter = true, bottom = {
        VaultButton(
            stringResource(R.string.pass_done_again),
            onClick = { onEvent(HotSeatEvent.AnotherNegotiationPass) },
            style = VaultButtonStyle.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
        VaultButton(
            stringResource(R.string.pass_done_vote),
            onClick = { onEvent(HotSeatEvent.StartVote) },
            modifier = Modifier.fillMaxWidth(),
        )
    }) {
        Text(
            stringResource(R.string.pass_done_title),
            style = MaterialTheme.typography.displaySmall,
            color = VaultTheme.colors.onPanel,
            modifier = Modifier.semantics { heading() },
        )
        KeeperBubble(
            if (state.pendingContracts == 0) {
                stringResource(R.string.pass_done_pending_none)
            } else {
                pluralStringResource(R.plurals.pass_done_pending, state.pendingContracts, state.pendingContracts)
            },
        )
    }
}

@Composable
private fun FullScreen(
    modifier: Modifier = Modifier,
    verticalCenter: Boolean = false,
    bottom: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(VaultTheme.colors.ink)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(VaultSpacing.lg),
    ) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = if (verticalCenter) {
                Arrangement.spacedBy(VaultSpacing.md, Alignment.CenterVertically)
            } else {
                Arrangement.spacedBy(VaultSpacing.md)
            },
            content = content,
        )
        Column(
            Modifier.padding(top = VaultSpacing.md),
            verticalArrangement = Arrangement.spacedBy(VaultSpacing.sm),
            content = bottom,
        )
    }
}

@ScreenPreviews
@Composable
private fun SetupPreview() {
    MinorityVaultTheme {
        HotSeatSetupScreen(
            HotSeatUiState.Setup(listOf("Asha", "Ben", "", "Dev"), canStart = false, canAdd = true, canRemove = false),
            onEvent = {},
        )
    }
}

@ScreenPreviews
@Composable
private fun PassPhonePreview() {
    MinorityVaultTheme {
        PassPhoneScreen(HotSeatUiState.PassPhone("Chandrika", Phase.Vote), onShow = {})
    }
}

@ScreenPreviews
@Composable
private fun PassDonePreview() {
    MinorityVaultTheme {
        NegotiationPassDoneScreen(HotSeatUiState.NegotiationPassDone(pendingContracts = 2), onEvent = {})
    }
}
