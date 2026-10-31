package com.example.minorityvault.feature.lobby

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.example.minorityvault.designsystem.GlassSurface
import com.example.minorityvault.designsystem.GlassTint
import com.example.minorityvault.designsystem.MinTouchTarget
import com.example.minorityvault.designsystem.VaultSpacing
import com.example.minorityvault.designsystem.VaultTheme
import com.example.minorityvault.designsystem.component.VaultButton
import com.example.minorityvault.designsystem.component.VaultButtonStyle
import com.example.minorityvault.designsystem.glassRim
import com.example.minorityvault.engine.PlayerId
import com.example.minorityvault.net.LobbyPlayer
import com.example.minorityvault.net.TransportFailure

/** A lobby screen: scrolling content over the vault backdrop, with actions pinned to the bottom. */
@Composable
internal fun LobbyScaffold(
    modifier: Modifier = Modifier,
    title: String? = null,
    bottom: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(VaultSpacing.lg),
    ) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(VaultSpacing.md),
        ) {
            if (title != null) {
                Text(
                    title,
                    style = MaterialTheme.typography.displaySmall,
                    color = VaultTheme.colors.brass,
                    modifier = Modifier.semantics { heading() },
                )
            }
            content()
        }
        Column(
            Modifier.padding(top = VaultSpacing.md),
            verticalArrangement = Arrangement.spacedBy(VaultSpacing.sm),
            content = bottom,
        )
    }
}

/** One seat in the lobby. [onRemove] is set only on the host's screen. */
@Composable
internal fun PlayerRow(player: LobbyPlayer, isMe: Boolean, onRemove: (() -> Unit)?) {
    val colors = VaultTheme.colors
    GlassSurface(
        Modifier.fillMaxWidth(),
        tint = GlassTint.Panel,
        rim = if (isMe) glassRim(colors.brass, strength = ME_RIM_STRENGTH) else glassRim(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = MinTouchTarget)
                .padding(horizontal = VaultSpacing.md, vertical = VaultSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VaultSpacing.sm),
        ) {
            Column(Modifier.weight(1f)) {
                Text(player.name, style = MaterialTheme.typography.titleMedium, color = colors.onPanel)
                val tags = listOfNotNull(
                    stringResource(R.string.player_you).takeIf { isMe },
                    stringResource(R.string.player_host).takeIf { player.isHost },
                    stringResource(R.string.player_bot).takeIf { player.isBot },
                )
                if (tags.isNotEmpty()) {
                    Text(tags.joinToString(" · "), style = MaterialTheme.typography.labelMedium, color = colors.brass)
                }
                if (!player.connected) {
                    Text(
                        stringResource(R.string.player_reconnecting),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.stampRed,
                    )
                }
            }
            if (onRemove != null) {
                val description = stringResource(R.string.host_remove, player.name)
                VaultButton(
                    "✕",
                    onClick = onRemove,
                    style = VaultButtonStyle.Secondary,
                    modifier = Modifier.semantics { contentDescription = description },
                )
            }
        }
    }
}

@Composable
internal fun PlayerList(
    players: List<LobbyPlayer>,
    maxPlayers: Int,
    me: PlayerId,
    onRemove: ((LobbyPlayer) -> Unit)? = null,
) {
    Text(
        stringResource(R.string.players_count, players.size, maxPlayers),
        style = MaterialTheme.typography.titleSmall,
        color = VaultTheme.colors.onPanelMuted,
    )
    players.forEach { player ->
        PlayerRow(
            player,
            isMe = player.id == me,
            onRemove = onRemove?.takeIf {
                !player.isHost
            }?.let { { it(player) } },
        )
    }
}

@Composable
internal fun BodyText(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = VaultTheme.colors.onPanel, modifier = modifier)
}

@Composable
internal fun MutedText(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = VaultTheme.colors.onPanelMuted, modifier = modifier)
}

/** A warning on red glass, read out as soon as it appears. */
@Composable
internal fun Warning(text: String) {
    GlassSurface(Modifier.fillMaxWidth(), tint = GlassTint.Danger) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = VaultTheme.colors.onPanel,
            modifier = Modifier
                .padding(VaultSpacing.md)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

@Composable
internal fun failureText(failure: TransportFailure): String = stringResource(
    when (failure) {
        TransportFailure.MISSING_PERMISSION -> R.string.failure_permission
        TransportFailure.RADIO_UNAVAILABLE -> R.string.failure_radio
        TransportFailure.LOCATION_OFF -> R.string.failure_location
        TransportFailure.ENDPOINT_UNAVAILABLE -> R.string.failure_endpoint
        TransportFailure.OTHER -> R.string.failure_other
    },
)

private const val ME_RIM_STRENGTH = 1.8f
