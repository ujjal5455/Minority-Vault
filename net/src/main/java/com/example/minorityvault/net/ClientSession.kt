package com.example.minorityvault.net

import com.example.minorityvault.engine.PlayerId
import com.example.minorityvault.protocol.ClientIntent
import com.example.minorityvault.protocol.Decoded
import com.example.minorityvault.protocol.ErrorCode
import com.example.minorityvault.protocol.HostEvent
import com.example.minorityvault.protocol.ProtocolCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class LobbyInfo(
    val roomCode: String,
    val players: List<LobbyPlayer>,
    val minPlayers: Int,
    val maxPlayers: Int,
    val started: Boolean,
)

sealed interface ClientState {
    /** Looking for rooms. [failure] says why the search couldn't start, if it couldn't. */
    data class Browsing(val hosts: List<NearbyHost> = emptyList(), val failure: TransportFailure? = null) :
        ClientState

    data class Connecting(val host: NearbyHost) : ClientState

    /** Still connected; join again with another name. */
    data class NameRefused(val host: NearbyHost, val reason: ErrorCode) : ClientState

    /** [reconnecting] while the host is out of reach; the seat is kept for the rejoin window. */
    data class InLobby(val host: NearbyHost, val me: PlayerId, val lobby: LobbyInfo, val reconnecting: Boolean) :
        ClientState

    data class Ended(val reason: EndReason) : ClientState
}

sealed interface EndReason {
    /** [theirs] is null if the host only said "mismatch" without its version. */
    data class VersionMismatch(val ours: String, val theirs: String?) : EndReason

    data object LobbyFull : EndReason

    data object GameInProgress : EndReason

    data object RemovedByHost : EndReason

    data object LobbyClosed : EndReason

    /** The host stayed out of reach for the whole rejoin window. */
    data object HostLost : EndReason

    data object ConnectionFailed : EndReason
}

/**
 * A player's side of the connection: finds rooms, joins one, keeps a heartbeat going (which also measures the
 * host's clock), and after a drop keeps looking for the same room to take the same seat back with its token.
 */
class ClientSession(
    private val transport: Transport,
    private val scope: CoroutineScope,
    private val clock: () -> Long,
    private val config: NetConfig = NetConfig(),
    private val codec: ProtocolCodec = ProtocolCodec(),
) {
    private val mutex = Mutex()
    private var jobs = emptyList<Job>()
    private val hosts = linkedMapOf<String, NearbyHost>()
    private val clockSync = ClockSync()

    private var name = ""
    private var host: NearbyHost? = null
    private var endpointId: String? = null
    private var me: PlayerId? = null
    private var token: String? = null
    private var lobby: LobbyInfo? = null
    private var droppedAt: Long? = null
    private var lastHeard = 0L
    private var lastHeartbeat = 0L
    private var lastDiscoveryStart = 0L
    private var lastSeq = -1L
    private var outSeq = 0L

    private val _state = MutableStateFlow<ClientState>(ClientState.Browsing())
    val state: StateFlow<ClientState> = _state.asStateFlow()

    private val _hostEvents = MutableSharedFlow<HostEvent>(extraBufferCapacity = EVENT_BUFFER)

    /** Game messages from the host. Lobby business is handled here and shows up in [state] instead. */
    val hostEvents: SharedFlow<HostEvent> = _hostEvents.asSharedFlow()

    /** Host time minus ours, once a heartbeat has come back. */
    val clockOffsetMillis: Long? get() = clockSync.offsetMillis

    /** Starts (or restarts) looking for rooms, leaving any room we were in. */
    fun browse() = act {
        ensureRunning()
        transport.stopAll()
        forgetRoom()
        hosts.clear()
        _state.value = ClientState.Browsing()
        val result = transport.startDiscovery()
        if (result is TransportResult.Failed) _state.value = ClientState.Browsing(failure = result.failure)
    }

    /** Joins [room] as [playerName]. After [ClientState.NameRefused], call again with a new name. */
    fun join(room: NearbyHost, playerName: String) = act {
        name = playerName
        val connected = endpointId
        _state.value = ClientState.Connecting(room)
        if (connected != null && host?.roomCode == room.roomCode) {
            sendToHost(ClientIntent.Join(name, token))
        } else {
            host = room
            transport.stopDiscovery()
            if (transport.requestConnection(room.endpointId, name) is TransportResult.Failed) {
                end(EndReason.ConnectionFailed)
            }
        }
    }

    /** Sends a game message. Dropped while out of reach. */
    fun send(intent: ClientIntent) = act { sendToHost(intent) }

    fun leave() {
        jobs.forEach { it.cancel() }
        jobs = emptyList()
        transport.stopAll()
    }

    private fun act(block: suspend () -> Unit) {
        scope.launch { mutex.withLock { block() } }
    }

    private fun ensureRunning() {
        if (jobs.isNotEmpty()) return
        jobs = listOf(
            scope.launch { transport.events.collect { event -> mutex.withLock { handle(event) } } },
            scope.launch {
                while (isActive) {
                    delay(config.tickMillis)
                    mutex.withLock { tick() }
                }
            },
        )
    }

    private suspend fun handle(event: TransportEvent) {
        when (event) {
            is TransportEvent.EndpointFound -> found(event)

            is TransportEvent.EndpointLost -> {
                hosts.remove(event.endpointId)
                if (_state.value is ClientState.Browsing) showHosts()
            }

            is TransportEvent.ConnectionRequested ->
                if (event.endpointId == host?.endpointId) {
                    transport.acceptConnection(event.endpointId)
                } else {
                    transport.rejectConnection(event.endpointId)
                }

            is TransportEvent.Connected -> if (event.endpointId == host?.endpointId) connected(event.endpointId)

            // While rejoining, a failed attempt just waits for the room to show up again.
            is TransportEvent.ConnectionFailed ->
                if (event.endpointId == host?.endpointId && droppedAt == null) end(EndReason.ConnectionFailed)

            is TransportEvent.Disconnected -> if (event.endpointId == endpointId) lostHost()

            is TransportEvent.Received -> if (event.endpointId == endpointId) receive(event.bytes)
        }
    }

    private suspend fun found(event: TransportEvent.EndpointFound) {
        val room = EndpointName.parse(event.endpointId, event.name) ?: return
        hosts[room.endpointId] = room
        when {
            _state.value is ClientState.Browsing -> showHosts()

            droppedAt != null && endpointId == null && room.roomCode == host?.roomCode -> {
                host = room
                transport.requestConnection(room.endpointId, name)
            }
        }
    }

    private fun connected(id: String) {
        endpointId = id
        lastHeard = clock()
        lastSeq = -1
        outSeq = 0
        sendToHost(ClientIntent.Join(name, token))
    }

    private fun receive(bytes: ByteArray) {
        lastHeard = clock()
        when (val decoded = codec.decodeHostEvent(bytes)) {
            is Decoded.VersionMismatch -> end(EndReason.VersionMismatch(codec.version, decoded.theirVersion))

            is Decoded.Malformed -> Unit

            is Decoded.Message -> if (decoded.seq > lastSeq) {
                lastSeq = decoded.seq
                onEvent(decoded.message)
            }
        }
    }

    private fun onEvent(event: HostEvent) {
        when (event) {
            is HostEvent.Welcome -> {
                me = PlayerId(event.playerId)
                token = event.playerToken
                droppedAt = null
                transport.stopDiscovery()
                sendHeartbeat()
                showLobby()
            }

            is HostEvent.LobbyUpdate -> {
                lobby = LobbyInfo(
                    event.roomCode,
                    event.players.map { it.toLobbyPlayer() },
                    event.minPlayers,
                    event.maxPlayers,
                    event.started,
                )
                showLobby()
            }

            is HostEvent.HeartbeatReply -> clockSync.add(event.clientSentAt, event.hostTime, clock())

            is HostEvent.Error -> refused(event)

            else -> _hostEvents.tryEmit(event)
        }
    }

    private fun refused(error: HostEvent.Error) {
        val room = host
        when (error.code) {
            ErrorCode.NAME_TAKEN, ErrorCode.NAME_INVALID ->
                if (room != null) _state.value = ClientState.NameRefused(room, error.code)

            ErrorCode.LOBBY_FULL -> end(EndReason.LobbyFull)

            ErrorCode.GAME_IN_PROGRESS -> end(EndReason.GameInProgress)

            ErrorCode.REMOVED_BY_HOST -> end(EndReason.RemovedByHost)

            ErrorCode.LOBBY_CLOSED -> end(EndReason.LobbyClosed)

            ErrorCode.VERSION_MISMATCH -> end(EndReason.VersionMismatch(codec.version, theirs = null))

            ErrorCode.NOT_JOINED, ErrorCode.MALFORMED, ErrorCode.RULE -> _hostEvents.tryEmit(error)
        }
    }

    /** The host is out of reach: keep the seat and look for the room again. */
    private suspend fun lostHost() {
        endpointId = null
        if (me == null) {
            end(EndReason.ConnectionFailed)
        } else {
            droppedAt = droppedAt ?: clock()
            showLobby()
            restartDiscovery()
        }
    }

    private suspend fun tick() {
        val now = clock()
        val connectedTo = endpointId
        val dropped = droppedAt
        when {
            connectedTo != null && now - lastHeard > config.silenceTimeoutMillis -> {
                transport.disconnect(connectedTo)
                lostHost()
            }

            connectedTo != null && me != null && now - lastHeartbeat >= config.heartbeatMillis -> sendHeartbeat()

            dropped != null && now - dropped > config.rejoinWindowMillis -> end(EndReason.HostLost)

            // Radios that were switched off don't always resume a search by themselves.
            dropped != null && connectedTo == null && now - lastDiscoveryStart >= REDISCOVER_MILLIS ->
                restartDiscovery()
        }
    }

    private suspend fun restartDiscovery() {
        lastDiscoveryStart = clock()
        transport.stopDiscovery()
        transport.startDiscovery()
    }

    private fun sendHeartbeat() {
        lastHeartbeat = clock()
        sendToHost(ClientIntent.Heartbeat(lastHeartbeat))
    }

    private fun sendToHost(intent: ClientIntent) {
        endpointId?.let { transport.send(it, codec.encode(outSeq++, intent)) }
    }

    private fun showHosts() {
        _state.value = ClientState.Browsing(hosts.values.toList())
    }

    private fun showLobby() {
        val room = host
        val seat = me
        val info = lobby
        if (room != null && seat != null && info != null) {
            _state.value = ClientState.InLobby(room, seat, info, reconnecting = droppedAt != null)
        }
    }

    private fun end(reason: EndReason) {
        transport.stopAll()
        forgetRoom()
        _state.value = ClientState.Ended(reason)
    }

    private fun forgetRoom() {
        host = null
        endpointId = null
        me = null
        token = null
        lobby = null
        droppedAt = null
        clockSync.reset()
    }

    private companion object {
        const val EVENT_BUFFER = 64
        const val REDISCOVER_MILLIS = 10_000L
    }
}
