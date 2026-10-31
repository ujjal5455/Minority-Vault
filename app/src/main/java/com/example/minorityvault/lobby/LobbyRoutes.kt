package com.example.minorityvault.lobby

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.minorityvault.feature.lobby.HostLobbyScreen
import com.example.minorityvault.feature.lobby.JoinScreen
import com.example.minorityvault.feature.lobby.NearbyPermissionGate

@Composable
fun HostRoute(name: String, onExit: () -> Unit, viewModel: HostViewModel = viewModel()) {
    val leave = {
        viewModel.close()
        onExit()
    }
    BackHandler(onBack = leave)
    NearbyPermissionGate(onBack = onExit) {
        LaunchedEffect(name) { viewModel.open(name) }
        val state by viewModel.state.collectAsStateWithLifecycle()
        state?.let {
            HostLobbyScreen(
                it,
                onAddBot = viewModel::addBot,
                onRemove = viewModel::remove,
                onStart = viewModel::start,
                onRetry = viewModel::retry,
                onClose = leave,
            )
        }
    }
}

@Composable
fun JoinRoute(name: String, onExit: () -> Unit, viewModel: JoinViewModel = viewModel()) {
    val leave = {
        viewModel.leave()
        onExit()
    }
    BackHandler(onBack = leave)
    NearbyPermissionGate(onBack = onExit) {
        LaunchedEffect(Unit) { viewModel.start() }
        val state by viewModel.state.collectAsStateWithLifecycle()
        JoinScreen(state, name, onJoin = viewModel::join, onBrowse = viewModel::browse, onLeave = leave)
    }
}
