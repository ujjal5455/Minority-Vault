package com.example.minorityvault.feature.lobby

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.minorityvault.designsystem.VaultPreviewTheme
import com.example.minorityvault.designsystem.component.KeeperBubble
import com.example.minorityvault.designsystem.component.VaultButton
import com.example.minorityvault.designsystem.component.VaultButtonStyle
import com.example.minorityvault.designsystem.preview.ScreenPreviews
import com.example.minorityvault.engine.PlayerId
import com.example.minorityvault.net.ClientState
import com.example.minorityvault.net.EndReason
import com.example.minorityvault.net.LobbyInfo
import com.example.minorityvault.net.NearbyHost
import com.example.minorityvault.protocol.ErrorCode

/** A player's way into a room: find it, knock, wait in the lobby, and find out plainly if anything goes wrong. */
@Composable
fun JoinScreen(
    state: ClientState,
    name: String,
    onJoin: (NearbyHost, String) -> Unit,
    onBrowse: () -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is ClientState.Browsing -> Browsing(state, name, onJoin, onBrowse, onLeave, modifier)
        is ClientState.Connecting -> Knocking(state.host, onLeave, modifier)
        is ClientState.NameRefused -> NameRefused(state, name, onJoin, onLeave, modifier)
        is ClientState.InLobby -> Waiting(state, onLeave, modifier)
        is ClientState.Ended -> Ended(state.reason, onBrowse, onLeave, modifier)
    }
}

@Composable
private fun Browsing(
    state: ClientState.Browsing,
    name: String,
    onJoin: (NearbyHost, String) -> Unit,
    onBrowse: () -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier,
) {
    LobbyScaffold(modifier, title = stringResource(R.string.join_title), bottom = { BackButton(onLeave) }) {
        val failure = state.failure
        if (failure != null) {
            Warning(failureText(failure))
            VaultButton(
                stringResource(R.string.host_retry),
                onBrowse,
                Modifier.fillMaxWidth(),
                VaultButtonStyle.Secondary,
            )
        } else {
            KeeperBubble(stringResource(R.string.join_searching))
            MutedText(stringResource(R.string.join_searching_hint))
        }
        state.hosts.forEach { host ->
            VaultButton(
                stringResource(R.string.join_room, host.roomCode, host.hostName),
                onClick = { onJoin(host, name) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun Knocking(host: NearbyHost, onLeave: () -> Unit, modifier: Modifier) {
    LobbyScaffold(modifier, title = stringResource(R.string.join_title), bottom = { BackButton(onLeave) }) {
        KeeperBubble(stringResource(R.string.join_connecting, host.roomCode))
    }
}

@Composable
private fun NameRefused(
    state: ClientState.NameRefused,
    name: String,
    onJoin: (NearbyHost, String) -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier,
) {
    var newName by rememberSaveable(state) { mutableStateOf(name) }
    LobbyScaffold(modifier, title = stringResource(R.string.join_title), bottom = {
        VaultButton(
            stringResource(R.string.join_try_name),
            onClick = { onJoin(state.host, newName) },
            modifier = Modifier.fillMaxWidth(),
            enabled = newName.isNotBlank(),
        )
        BackButton(onLeave)
    }) {
        Warning(
            stringResource(
                if (state.reason == ErrorCode.NAME_TAKEN) R.string.join_name_taken else R.string.join_name_invalid,
            ),
        )
        NameField(newName, { newName = it }, stringResource(R.string.home_name_label))
    }
}

@Composable
private fun Waiting(state: ClientState.InLobby, onLeave: () -> Unit, modifier: Modifier) {
    LobbyScaffold(
        modifier,
        title = stringResource(R.string.join_room_title, state.lobby.roomCode),
        bottom = {
            VaultButton(
                stringResource(R.string.join_leave),
                onLeave,
                Modifier.fillMaxWidth(),
                VaultButtonStyle.Secondary,
            )
        },
    ) {
        when {
            state.reconnecting -> Warning(stringResource(R.string.join_reconnecting))
            state.lobby.started -> Sealed()
            else -> KeeperBubble(stringResource(R.string.join_waiting, state.host.hostName))
        }
        PlayerList(state.lobby.players, state.lobby.maxPlayers, state.me)
    }
}

@Composable
private fun Ended(reason: EndReason, onBrowse: () -> Unit, onLeave: () -> Unit, modifier: Modifier) {
    LobbyScaffold(modifier, title = stringResource(R.string.join_title), bottom = {
        VaultButton(stringResource(R.string.join_look_again), onBrowse, Modifier.fillMaxWidth())
        BackButton(onLeave)
    }) {
        Warning(endText(reason))
    }
}

@Composable
private fun endText(reason: EndReason): String = when (reason) {
    is EndReason.VersionMismatch -> reason.theirs?.let { stringResource(R.string.end_version, reason.ours, it) }
        ?: stringResource(R.string.end_version_unknown)

    EndReason.LobbyFull -> stringResource(R.string.end_full)

    EndReason.GameInProgress -> stringResource(R.string.end_in_progress)

    EndReason.RemovedByHost -> stringResource(R.string.end_removed)

    EndReason.LobbyClosed -> stringResource(R.string.end_closed)

    EndReason.HostLost -> stringResource(R.string.end_host_lost)

    EndReason.ConnectionFailed -> stringResource(R.string.end_connection_failed)
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    VaultButton(stringResource(R.string.back), onBack, Modifier.fillMaxWidth(), VaultButtonStyle.Secondary)
}

private val previewHost = NearbyHost("e1", "KTRW", "Asha")

@ScreenPreviews
@Composable
private fun JoinBrowsingPreview() {
    VaultPreviewTheme {
        JoinScreen(
            ClientState.Browsing(listOf(previewHost, NearbyHost("e2", "MXPA", "Dev"))),
            name = "Ben",
            onJoin = { _, _ -> },
            onBrowse = {},
            onLeave = {},
        )
    }
}

@ScreenPreviews
@Composable
private fun JoinWaitingPreview() {
    VaultPreviewTheme {
        JoinScreen(
            ClientState.InLobby(
                previewHost,
                PlayerId("p2"),
                LobbyInfo("KTRW", previewPlayers, minPlayers = 4, maxPlayers = 8, started = false),
                reconnecting = true,
            ),
            name = "Ben",
            onJoin = { _, _ -> },
            onBrowse = {},
            onLeave = {},
        )
    }
}

@ScreenPreviews
@Composable
private fun JoinVersionPreview() {
    VaultPreviewTheme {
        JoinScreen(
            ClientState.Ended(EndReason.VersionMismatch(ours = "1.0", theirs = "2.0")),
            name = "Ben",
            onJoin = { _, _ -> },
            onBrowse = {},
            onLeave = {},
        )
    }
}
