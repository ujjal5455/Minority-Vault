package com.example.minorityvault.feature.lobby

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.em
import com.example.minorityvault.designsystem.GlassSurface
import com.example.minorityvault.designsystem.GlassTint
import com.example.minorityvault.designsystem.VaultPreviewTheme
import com.example.minorityvault.designsystem.VaultSpacing
import com.example.minorityvault.designsystem.VaultTheme
import com.example.minorityvault.designsystem.component.KeeperBubble
import com.example.minorityvault.designsystem.component.VaultButton
import com.example.minorityvault.designsystem.component.VaultButtonStyle
import com.example.minorityvault.designsystem.glassRim
import com.example.minorityvault.designsystem.preview.ScreenPreviews
import com.example.minorityvault.designsystem.sheen
import com.example.minorityvault.engine.PlayerId
import com.example.minorityvault.net.Advertising
import com.example.minorityvault.net.HostState
import com.example.minorityvault.net.LobbyPlayer
import com.example.minorityvault.net.TransportFailure

/** The host's room: a big code to read out, everyone who has joined, bots to fill seats, and the start. */
@Composable
fun HostLobbyScreen(
    state: HostState,
    onAddBot: () -> Unit,
    onRemove: (PlayerId) -> Unit,
    onStart: () -> Unit,
    onRetry: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val me = state.players.first { it.isHost }.id
    LobbyScaffold(modifier, title = stringResource(R.string.host_title), bottom = {
        if (!state.started) {
            StartHint(state)
            VaultButton(stringResource(R.string.host_start), onStart, Modifier.fillMaxWidth(), enabled = state.canStart)
        }
        VaultButton(stringResource(R.string.host_close), onClose, Modifier.fillMaxWidth(), VaultButtonStyle.Secondary)
    }) {
        RoomCode(state.roomCode)
        when (val advertising = state.advertising) {
            Advertising.Starting -> MutedText(stringResource(R.string.host_status_starting))

            Advertising.Live -> if (!state.started) MutedText(stringResource(R.string.host_status_live))

            is Advertising.Failed -> {
                Warning(failureText(advertising.failure))
                VaultButton(
                    stringResource(R.string.host_retry),
                    onRetry,
                    Modifier.fillMaxWidth(),
                    VaultButtonStyle.Secondary,
                )
            }
        }
        if (state.started) Sealed()
        PlayerList(
            state.players,
            state.maxPlayers,
            me,
            onRemove = if (state.started) null else { player -> onRemove(player.id) },
        )
        if (!state.started && !state.isFull) {
            VaultButton(
                stringResource(R.string.host_add_bot),
                onAddBot,
                Modifier.fillMaxWidth(),
                VaultButtonStyle.Secondary,
            )
        }
    }
}

@Composable
private fun StartHint(state: HostState) {
    val missing = state.minPlayers - state.players.size
    when {
        missing > 0 -> MutedText(pluralStringResource(R.plurals.host_need_players, missing, missing))
        state.players.any { !it.connected } -> MutedText(stringResource(R.string.host_waiting_reconnect))
    }
}

/** The code is spelled out letter by letter for screen readers. */
@Composable
internal fun RoomCode(code: String) {
    val colors = VaultTheme.colors
    val description = stringResource(R.string.host_code_description, code.toList().joinToString(" "))
    GlassSurface(
        Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description },
        tint = GlassTint.Raised,
        rim = glassRim(colors.brass, strength = CODE_RIM_STRENGTH),
    ) {
        Column(Modifier.fillMaxWidth().padding(VaultSpacing.lg), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.host_code_label),
                style = MaterialTheme.typography.labelLarge,
                color = colors.onPanelMuted,
            )
            Text(
                code,
                style = MaterialTheme.typography.displayLarge.copy(letterSpacing = CODE_LETTER_SPACING.em),
                color = colors.brass,
                modifier = Modifier.sheen(),
            )
        }
    }
}

/** Shown once the host starts: network rounds arrive in Phase 4. */
@Composable
internal fun Sealed() {
    Text(
        stringResource(R.string.sealed_title),
        style = MaterialTheme.typography.headlineMedium,
        color = VaultTheme.colors.onPanel,
        modifier = Modifier.semantics { heading() },
    )
    KeeperBubble(stringResource(R.string.sealed_body))
}

private const val CODE_RIM_STRENGTH = 1.8f
private const val CODE_LETTER_SPACING = 0.18f

internal val previewPlayers = listOf(
    LobbyPlayer(PlayerId("p1"), "Asha", isHost = true, isBot = false, connected = true),
    LobbyPlayer(PlayerId("p2"), "Ben", isHost = false, isBot = false, connected = true),
    LobbyPlayer(PlayerId("p3"), "Bot 1", isHost = false, isBot = true, connected = true),
    LobbyPlayer(PlayerId("p4"), "Chandrika", isHost = false, isBot = false, connected = false),
)

private fun previewHost(advertising: Advertising = Advertising.Live, players: List<LobbyPlayer> = previewPlayers) =
    HostState("KTRW", advertising, players, started = false, minPlayers = 4, maxPlayers = 8)

@ScreenPreviews
@Composable
private fun HostLobbyPreview() {
    VaultPreviewTheme {
        HostLobbyScreen(previewHost(), onAddBot = {}, onRemove = {}, onStart = {}, onRetry = {}, onClose = {})
    }
}

@ScreenPreviews
@Composable
private fun HostLobbyFailedPreview() {
    VaultPreviewTheme {
        HostLobbyScreen(
            previewHost(Advertising.Failed(TransportFailure.RADIO_UNAVAILABLE), previewPlayers.take(1)),
            onAddBot = {},
            onRemove = {},
            onStart = {},
            onRetry = {},
            onClose = {},
        )
    }
}
