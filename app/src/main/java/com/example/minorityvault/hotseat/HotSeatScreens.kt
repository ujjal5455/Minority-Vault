package com.example.minorityvault.hotseat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.example.minorityvault.R
import com.example.minorityvault.designsystem.VaultPreviewTheme
import com.example.minorityvault.designsystem.VaultSpacing
import com.example.minorityvault.designsystem.VaultTheme
import com.example.minorityvault.designsystem.component.KeeperBubble
import com.example.minorityvault.designsystem.component.VaultButton
import com.example.minorityvault.designsystem.component.VaultButtonStyle
import com.example.minorityvault.designsystem.component.VaultDoor
import com.example.minorityvault.designsystem.preview.ScreenPreviews
import com.example.minorityvault.designsystem.sheen
import com.example.minorityvault.engine.Phase
import kotlinx.coroutines.launch

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
            modifier = Modifier.semantics { heading() }.sheen(),
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

/**
 * Hides the last player's screen until the next player has the phone. A vault door swings shut as this
 * appears; when the next player taps, its wheel spins and the door swings open onto their turn.
 */
@Composable
fun PassPhoneScreen(state: HotSeatUiState.PassPhone, onShow: () -> Unit, modifier: Modifier = Modifier) {
    val colors = VaultTheme.colors
    val reducedMotion = VaultTheme.reducedMotion
    val scope = rememberCoroutineScope()
    val swing = remember(state) { Animatable(if (reducedMotion) 0f else DOOR_OPEN_DEGREES) }
    val wheel = remember(state) { Animatable(0f) }
    var opening by remember(state) { mutableStateOf(false) }
    LaunchedEffect(state) {
        if (!reducedMotion) swing.animateTo(0f, spring(dampingRatio = DOOR_DAMPING, stiffness = Spring.StiffnessLow))
    }
    val open = {
        if (!opening) {
            opening = true
            if (reducedMotion) {
                onShow()
            } else {
                scope.launch {
                    wheel.animateTo(WHEEL_TURN_DEGREES, tween(WHEEL_MILLIS, easing = FastOutSlowInEasing))
                    swing.animateTo(DOOR_OPEN_DEGREES, tween(SWING_MILLIS, easing = FastOutLinearInEasing))
                    onShow()
                }
            }
        }
    }
    FullScreen(modifier, verticalCenter = true, bottom = {
        VaultButton(
            stringResource(R.string.pass_phone_show, state.name),
            onClick = open,
            enabled = !opening,
            modifier = Modifier.fillMaxWidth(),
        )
    }) {
        VaultDoor(
            wheelDegrees = wheel.value,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .graphicsLayer {
                    rotationY = swing.value
                    transformOrigin = TransformOrigin(0f, HALF)
                    cameraDistance = DOOR_CAMERA_DISTANCE * density
                },
        )
        PassPhoneText(state)
    }
}

@Composable
private fun PassPhoneText(state: HotSeatUiState.PassPhone) {
    val colors = VaultTheme.colors
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
    // No background of its own: the vault backdrop shows through.
    Column(
        modifier
            .fillMaxSize()
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
    VaultPreviewTheme {
        HotSeatSetupScreen(
            HotSeatUiState.Setup(listOf("Asha", "Ben", "", "Dev"), canStart = false, canAdd = true, canRemove = false),
            onEvent = {},
        )
    }
}

@ScreenPreviews
@Composable
private fun PassPhonePreview() {
    VaultPreviewTheme {
        PassPhoneScreen(HotSeatUiState.PassPhone("Chandrika", Phase.Vote), onShow = {})
    }
}

@ScreenPreviews
@Composable
private fun PassDonePreview() {
    VaultPreviewTheme {
        NegotiationPassDoneScreen(HotSeatUiState.NegotiationPassDone(pendingContracts = 2), onEvent = {})
    }
}

private const val DOOR_OPEN_DEGREES = -100f
private const val DOOR_DAMPING = 0.65f
private const val DOOR_CAMERA_DISTANCE = 14f
private const val WHEEL_TURN_DEGREES = 270f
private const val WHEEL_MILLIS = 650
private const val SWING_MILLIS = 450
private const val HALF = 0.5f
