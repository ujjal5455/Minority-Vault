package com.example.minorityvault.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minorityvault.engine.PlayerId
import com.example.minorityvault.net.ClientSession
import com.example.minorityvault.net.ClientState
import com.example.minorityvault.net.HostSession
import com.example.minorityvault.net.HostState
import com.example.minorityvault.net.NearbyHost
import com.example.minorityvault.net.Transport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.SecureRandom
import javax.inject.Inject
import kotlin.random.asKotlinRandom

/** Makes a fresh radio connection for each room. Tests can hand out fakes. */
fun interface TransportFactory {
    fun create(): Transport
}

/** The host's room. A session lives from [open] until [close]; reopening after that starts a new room. */
@HiltViewModel
class HostViewModel @Inject constructor(private val transports: TransportFactory) : ViewModel() {
    private var session: HostSession? = null
    private var mirror: Job? = null
    private val _state = MutableStateFlow<HostState?>(null)
    val state: StateFlow<HostState?> = _state.asStateFlow()

    /** Opens a room as [hostName], unless one is already open (e.g. after the screen rotated). */
    fun open(hostName: String) {
        if (session != null) return
        val room = HostSession(
            transports.create(),
            viewModelScope,
            hostName,
            System::currentTimeMillis,
            SecureRandom().asKotlinRandom(),
        )
        session = room
        mirror = viewModelScope.launch { room.state.collect { _state.value = it } }
        room.start()
    }

    fun addBot() {
        session?.addBot()
    }

    fun remove(player: PlayerId) {
        session?.removePlayer(player)
    }

    fun start() {
        session?.startGame()
    }

    fun retry() {
        session?.retryAdvertising()
    }

    /** Tells everyone the room is closing. Returns at once; the farewell finishes in the background. */
    fun close() {
        val room = session ?: return
        session = null
        mirror?.cancel()
        _state.value = null
        viewModelScope.launch { room.close() }
    }

    override fun onCleared() {
        session?.stop()
    }
}

/** A player looking for a room and sitting in one. */
@HiltViewModel
class JoinViewModel @Inject constructor(private val transports: TransportFactory) : ViewModel() {
    private var session: ClientSession? = null
    private var mirror: Job? = null
    private val _state = MutableStateFlow<ClientState>(ClientState.Browsing())
    val state: StateFlow<ClientState> = _state.asStateFlow()

    /** Starts looking for rooms, unless a search or room is already under way (e.g. after rotation). */
    fun start() {
        if (session == null) browse()
    }

    fun browse() {
        val client = session ?: ClientSession(transports.create(), viewModelScope, System::currentTimeMillis).also {
            session = it
            mirror = viewModelScope.launch { it.state.collect { state -> _state.value = state } }
        }
        client.browse()
    }

    fun join(room: NearbyHost, name: String) {
        session?.join(room, name)
    }

    fun leave() {
        session?.leave()
        session = null
        mirror?.cancel()
        _state.value = ClientState.Browsing()
    }

    override fun onCleared() {
        session?.leave()
    }
}
