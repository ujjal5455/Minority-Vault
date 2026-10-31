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
import kotlin.random.Random

/** The host's lobby as its own screen shows it. */
data class HostState(
    val roomCode: String,
    val advertising: Advertising,
    val players: List<LobbyPlayer>,
    val started: Boolean,
    val minPlayers: Int,
    val maxPlayers: Int,
) {
    val canStart: Boolean get() = !started && players.size >= minPlayers && players.all { it.connected }
    val isFull: Boolean get() = players.size >= maxPlayers
}

sealed interface Advertising {
    data object Starting : Advertising

    data object Live : Advertising

    data class Failed(val failure: TransportFailure) : Advertising
}

/**
 * The hub of a game (CLAUDE.md §3, §7). Advertises a room, seats players who join, hands each a token to get
 * their seat back after a drop, answers heartbeats, and notices phones that go quiet. The host player sits in
 * seat one without a connection. Game messages are passed on through [gameMessages], tagged with the sender's
 * seat, which comes from the connection and never from the message.
 */
class HostSession(
    private val transport: Transport,
    private val scope: CoroutineScope,
    hostName: String,
    private val clock: () -> Long,
    private val random: Random,
    private val config: NetConfig = NetConfig(),
    private val codec: ProtocolCodec = ProtocolCodec(),
) {
    private class Seat(var player: LobbyPlayer, var token: String? = null) {
        var endpointId: String? = null
        var droppedAt: Long? = null
    }

    private class Endpoint(val connectedAt: Long) {
        var player: PlayerId? = null
        var lastHeard = connectedAt
        var lastSeq = -1L
        var outSeq = 0L
        var leaving = false
    }

    private val mutex = Mutex()
    private val seats = mutableListOf<Seat>()
    private val endpoints = mutableMapOf<String, Endpoint>()
    private var nextPlayer = 1
    private var nextBot = 1
    private var jobs = emptyList<Job>()
    private var advertising: Advertising = Advertising.Starting
    private var started = false

    private val roomCode = random.roomCode()
    private val hostName = cleanName(hostName, config.maxNameLength) ?: DEFAULT_HOST_NAME

    /** The host player's own seat. */
    val me: PlayerId = newPlayerId()

    init {
        seats += Seat(LobbyPlayer(me, this.hostName, isHost = true, isBot = false, connected = true))
    }

    private val _state = MutableStateFlow(snapshot())
    val state: StateFlow<HostState> = _state.asStateFlow()

    private val _gameMessages = MutableSharedFlow<Pair<PlayerId, ClientIntent>>(extraBufferCapacity = MESSAGE_BUFFER)
    val gameMessages: SharedFlow<Pair<PlayerId, ClientIntent>> = _gameMessages.asSharedFlow()

    /** Opens the room. Call once. */
    fun start() {
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
        retryAdvertising()
    }

    /** Opens the room to nearby phones, or tries again after a failure (e.g. Bluetooth was off). */
    fun retryAdvertising() = act {
        advertising = Advertising.Starting
        publish()
        val result = transport.startAdvertising(EndpointName.of(roomCode, hostName))
        advertising = if (result is TransportResult.Failed) Advertising.Failed(result.failure) else Advertising.Live
        publish()
    }

    fun addBot() = act {
        if (!started && seats.size < config.maxPlayers) {
            seats += Seat(LobbyPlayer(newPlayerId(), botName(), isHost = false, isBot = true, connected = true))
            publish()
        }
    }

    /** Takes a bot or a player out of the lobby. The host can't remove themself. */
    fun removePlayer(id: PlayerId) = act {
        val seat = seats.firstOrNull { it.player.id == id && !it.player.isHost }
        if (seat != null) {
            seats -= seat
            seat.endpointId?.let { refuse(it, ErrorCode.REMOVED_BY_HOST) }
            publish()
        }
    }

    /** Locks the lobby: from now on only players who already have a seat may come back. */
    fun startGame() = act {
        if (snapshot().canStart) {
            started = true
            publish()
        }
    }

    fun send(to: PlayerId, event: HostEvent) = act {
        seats.firstOrNull { it.player.id == to }?.endpointId?.let { sendTo(it, event) }
    }

    fun broadcast(event: HostEvent) = act { broadcastNow(event) }

    /** Tells everyone the room is closing, gives the message a moment to leave, then shuts down. */
    suspend fun close() {
        mutex.withLock { broadcastNow(HostEvent.Error(ErrorCode.LOBBY_CLOSED)) }
        delay(config.farewellMillis)
        stop()
    }

    /** Shuts down at once, without telling anyone. */
    fun stop() {
        jobs.forEach { it.cancel() }
        jobs = emptyList()
        transport.stopAll()
    }

    private fun act(block: suspend () -> Unit) {
        scope.launch { mutex.withLock { block() } }
    }

    private suspend fun handle(event: TransportEvent) {
        when (event) {
            // Anyone may connect; Join decides whether they get a seat.
            is TransportEvent.ConnectionRequested -> transport.acceptConnection(event.endpointId)

            is TransportEvent.Connected -> endpoints[event.endpointId] = Endpoint(clock())

            is TransportEvent.Disconnected -> drop(event.endpointId)

            is TransportEvent.Received -> receive(event.endpointId, event.bytes)

            is TransportEvent.ConnectionFailed, is TransportEvent.EndpointFound, is TransportEvent.EndpointLost -> Unit
        }
    }

    private fun receive(endpointId: String, bytes: ByteArray) {
        val endpoint = endpoints[endpointId] ?: return
        endpoint.lastHeard = clock()
        when (val decoded = codec.decodeClientIntent(bytes)) {
            is Decoded.VersionMismatch -> refuse(endpointId, ErrorCode.VERSION_MISMATCH)

            is Decoded.Malformed -> sendTo(endpointId, HostEvent.Error(ErrorCode.MALFORMED))

            is Decoded.Message -> if (decoded.seq > endpoint.lastSeq) {
                endpoint.lastSeq = decoded.seq
                onMessage(endpointId, endpoint, decoded.message)
            }
        }
    }

    private fun onMessage(endpointId: String, endpoint: Endpoint, message: ClientIntent) {
        val player = endpoint.player
        when {
            message is ClientIntent.Join -> join(endpointId, endpoint, message)
            message is ClientIntent.Heartbeat -> sendTo(endpointId, HostEvent.HeartbeatReply(message.sentAt, clock()))
            player == null -> sendTo(endpointId, HostEvent.Error(ErrorCode.NOT_JOINED))
            else -> _gameMessages.tryEmit(player to message)
        }
    }

    private fun join(endpointId: String, endpoint: Endpoint, join: ClientIntent.Join) {
        val returning = join.playerToken?.let { token -> seats.firstOrNull { it.token == token } }
        val name = cleanName(join.name, config.maxNameLength)
        when {
            returning != null -> seat(endpointId, endpoint, returning)

            // A stray second Join from a seated phone changes nothing.
            endpoint.player != null -> Unit

            started -> refuse(endpointId, ErrorCode.GAME_IN_PROGRESS)

            seats.size >= config.maxPlayers -> refuse(endpointId, ErrorCode.LOBBY_FULL)

            // The connection stays open so the player can try another name.
            name == null -> sendTo(endpointId, HostEvent.Error(ErrorCode.NAME_INVALID))

            isTaken(name) -> sendTo(endpointId, HostEvent.Error(ErrorCode.NAME_TAKEN))

            else -> {
                val seat = Seat(LobbyPlayer(newPlayerId(), name, false, false, true), random.playerToken())
                seats += seat
                seat(endpointId, endpoint, seat)
            }
        }
    }

    private fun seat(endpointId: String, endpoint: Endpoint, seat: Seat) {
        // The player came back before their old connection timed out, so that one is stale.
        seat.endpointId?.takeIf { it != endpointId }?.let { stale ->
            endpoints.remove(stale)
            transport.disconnect(stale)
        }
        seat.endpointId = endpointId
        seat.droppedAt = null
        seat.player = seat.player.copy(connected = true)
        endpoint.player = seat.player.id
        sendTo(endpointId, HostEvent.Welcome(seat.player.id.value, checkNotNull(seat.token), roomCode))
        publish(alwaysBroadcast = true)
    }

    /** The connection is gone. A seated player keeps the seat for the rejoin window. */
    private fun drop(endpointId: String) {
        endpoints.remove(endpointId) ?: return
        val seat = seats.firstOrNull { it.endpointId == endpointId } ?: return
        seat.endpointId = null
        seat.droppedAt = clock()
        seat.player = seat.player.copy(connected = false)
        publish()
    }

    private fun tick() {
        val now = clock()
        endpoints.filterValues { it.player == null && !it.leaving && now - it.connectedAt > config.joinTimeoutMillis }
            .keys.forEach { id ->
                endpoints.remove(id)
                transport.disconnect(id)
            }
        // A phone can vanish without the radio noticing; silence counts as a drop.
        endpoints.filterValues { it.player != null && now - it.lastHeard > config.silenceTimeoutMillis }
            .keys.forEach { id ->
                transport.disconnect(id)
                drop(id)
            }
        val expired = seats.filter { seat -> seat.droppedAt?.let { now - it > config.rejoinWindowMillis } == true }
        if (expired.isNotEmpty()) {
            if (started) {
                // The seat stays in the game, but nobody can claim it any more (Phase 10 decides what it becomes).
                expired.forEach {
                    it.token = null
                    it.droppedAt = null
                }
            } else {
                seats -= expired.toSet()
            }
            publish()
        }
    }

    /** Sends a last message, then lets the connection go once it has had time to leave. */
    private fun refuse(endpointId: String, code: ErrorCode) {
        sendTo(endpointId, HostEvent.Error(code))
        endpoints[endpointId]?.leaving = true
        scope.launch {
            delay(config.farewellMillis)
            mutex.withLock { endpoints.remove(endpointId) }
            transport.disconnect(endpointId)
        }
    }

    private fun sendTo(endpointId: String, event: HostEvent) {
        val endpoint = endpoints[endpointId] ?: return
        transport.send(endpointId, codec.encode(endpoint.outSeq++, event))
    }

    private fun broadcastNow(event: HostEvent) {
        endpoints.filterValues { it.player != null && !it.leaving }.keys.forEach { sendTo(it, event) }
    }

    private fun publish(alwaysBroadcast: Boolean = false) {
        val next = snapshot()
        val previous = _state.value
        _state.value = next
        if (alwaysBroadcast || next.players != previous.players || next.started != previous.started) {
            broadcastNow(
                HostEvent.LobbyUpdate(
                    roomCode,
                    next.players.map { it.toDto() },
                    config.minPlayers,
                    config.maxPlayers,
                    started,
                ),
            )
        }
    }

    private fun snapshot() =
        HostState(roomCode, advertising, seats.map { it.player }, started, config.minPlayers, config.maxPlayers)

    private fun isTaken(name: String) = seats.any { it.player.name.equals(name, ignoreCase = true) }

    private fun newPlayerId() = PlayerId("p${nextPlayer++}")

    private fun botName(): String = generateSequence { "Bot ${nextBot++}" }.first { !isTaken(it) }

    private companion object {
        const val DEFAULT_HOST_NAME = "Host"
        const val MESSAGE_BUFFER = 64
    }
}
