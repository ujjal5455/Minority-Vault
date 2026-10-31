package com.example.minorityvault

import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.minorityvault.designsystem.VaultBackdrop
import com.example.minorityvault.designsystem.VaultTheme
import com.example.minorityvault.feature.lobby.HomeScreen
import com.example.minorityvault.hotseat.HotSeatRoute
import com.example.minorityvault.lobby.HostRoute
import com.example.minorityvault.lobby.JoinRoute
import com.example.minorityvault.net.NetConfig

private enum class Destination { HOME, PASS_PHONE, HOST, JOIN }

/** The top of the app: one vault backdrop behind every screen, and the way between home and each mode. */
@Composable
fun AppRoot() {
    var destination by rememberSaveable { mutableStateOf(Destination.HOME) }
    var name by rememberSaveable { mutableStateOf("") }
    val home = { destination = Destination.HOME }
    val reducedMotion = VaultTheme.reducedMotion
    VaultBackdrop {
        AnimatedContent(
            targetState = destination,
            transitionSpec = { if (reducedMotion) instant() else heavyPanelSlide() },
            label = "destination",
        ) { target ->
            when (target) {
                Destination.HOME -> HomeScreen(
                    name = name,
                    onNameChange = { name = it.take(MAX_NAME_LENGTH) },
                    onHost = { destination = Destination.HOST },
                    onJoin = { destination = Destination.JOIN },
                    onPassPhone = { destination = Destination.PASS_PHONE },
                )

                Destination.PASS_PHONE -> HotSeatRoute(onExit = home)

                Destination.HOST -> HostRoute(name.trim(), onExit = home)

                Destination.JOIN -> JoinRoute(name.trim(), onExit = home)
            }
        }
    }
}

private val MAX_NAME_LENGTH = NetConfig().maxNameLength
