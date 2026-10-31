package com.example.minorityvault.net

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * An in-memory radio for tests: every [FakeTransport] made from one network can find and reach the others.
 * Events are delivered immediately and in order. [goOffline], [goOnline] and [mute] simulate trouble.
 */
class FakeNetwork {
    private val transports = mutableListOf<FakeTransport>()
    private var nextId = 1

    fun transport(): FakeTransport = FakeTransport(this, newId()).also { transports += it }

    /** Like turning Bluetooth off: every connection drops and the phone vanishes from discovery. */
    fun goOffline(transport: FakeTransport) {
        transport.connections.toList().forEach { peer -> breakLink(transport, peer) }
        if (transport.advertisedName != null) withdraw(transport)
        transport.online = false
    }

    /** Back on the air, under a new endpoint id, as with a real radio. */
    fun goOnline(transport: FakeTransport) {
        transport.online = true
        transport.id = newId()
        if (transport.discovering) announceAdvertisersTo(transport)
        if (transport.advertisedName != null) announce(transport)
    }

    /** Messages to and from [transport] silently vanish, but nobody is told the connection is gone. */
    fun mute(transport: FakeTransport, muted: Boolean = true) {
        transport.muted = muted
    }

    internal fun find(id: String): FakeTransport? = transports.firstOrNull { it.id == id && it.online }

    internal fun announce(advertiser: FakeTransport) {
        transports.filter { it.discovering && it.online && it !== advertiser }.forEach {
            it.emit(TransportEvent.EndpointFound(advertiser.id, checkNotNull(advertiser.advertisedName)))
        }
    }

    internal fun withdraw(advertiser: FakeTransport) {
        transports.filter { it.discovering && it.online && it !== advertiser }.forEach {
            it.emit(TransportEvent.EndpointLost(advertiser.id))
        }
    }

    internal fun announceAdvertisersTo(discoverer: FakeTransport) {
        visibleAdvertisers(discoverer).forEach {
            discoverer.emit(TransportEvent.EndpointFound(it.id, checkNotNull(it.advertisedName)))
        }
    }

    /** Both ends hear that the link broke. */
    internal fun breakLink(a: FakeTransport, b: FakeTransport) {
        if (b !in a.connections) return
        a.connections -= b
        b.connections -= a
        a.emit(TransportEvent.Disconnected(b.id))
        b.emit(TransportEvent.Disconnected(a.id))
    }

    private fun visibleAdvertisers(viewer: FakeTransport) =
        transports.filter { it !== viewer && it.online && it.advertisedName != null }

    private fun newId() = "endpoint-${nextId++}"
}

class FakeTransport internal constructor(private val network: FakeNetwork, id: String) : Transport {
    var id: String = id
        internal set
    internal var online = true
    internal var muted = false
    internal var advertisedName: String? = null
    internal var discovering = false
    internal val connections = mutableSetOf<FakeTransport>()

    /** Connection attempts in progress: the other side, and whether each side has accepted yet. */
    private val pending = mutableMapOf<FakeTransport, Boolean>()

    private val channel = Channel<TransportEvent>(Channel.UNLIMITED)
    override val events: Flow<TransportEvent> = channel.receiveAsFlow()

    internal fun emit(event: TransportEvent) {
        channel.trySend(event)
    }

    override suspend fun startAdvertising(endpointName: String): TransportResult {
        if (!online) return TransportResult.Failed(TransportFailure.RADIO_UNAVAILABLE)
        advertisedName = endpointName
        network.announce(this)
        return TransportResult.Ok
    }

    override suspend fun startDiscovery(): TransportResult {
        if (!online) return TransportResult.Failed(TransportFailure.RADIO_UNAVAILABLE)
        if (!discovering) {
            discovering = true
            network.announceAdvertisersTo(this)
        }
        return TransportResult.Ok
    }

    override fun stopAdvertising() {
        if (advertisedName != null && online) network.withdraw(this)
        advertisedName = null
    }

    override fun stopDiscovery() {
        discovering = false
    }

    override suspend fun requestConnection(endpointId: String, localName: String): TransportResult {
        val target = network.find(endpointId)
        if (!online || target == null || target.advertisedName == null) {
            return TransportResult.Failed(TransportFailure.ENDPOINT_UNAVAILABLE)
        }
        pending[target] = false
        target.pending[this] = false
        emit(TransportEvent.ConnectionRequested(target.id, checkNotNull(target.advertisedName)))
        target.emit(TransportEvent.ConnectionRequested(id, localName))
        return TransportResult.Ok
    }

    override suspend fun acceptConnection(endpointId: String): TransportResult {
        val other = pending.keys.firstOrNull { it.id == endpointId }
            ?: return TransportResult.Failed(TransportFailure.ENDPOINT_UNAVAILABLE)
        pending[other] = true
        if (other.pending[this] == true) {
            pending -= other
            other.pending -= this
            connections += other
            other.connections += this
            emit(TransportEvent.Connected(other.id))
            other.emit(TransportEvent.Connected(id))
        }
        return TransportResult.Ok
    }

    override fun rejectConnection(endpointId: String) {
        val other = pending.keys.firstOrNull { it.id == endpointId } ?: return
        pending -= other
        other.pending -= this
        emit(TransportEvent.ConnectionFailed(other.id))
        other.emit(TransportEvent.ConnectionFailed(id))
    }

    override fun send(endpointId: String, bytes: ByteArray) {
        val peer = connections.firstOrNull { it.id == endpointId } ?: return
        if (muted || peer.muted) return
        peer.emit(TransportEvent.Received(id, bytes))
    }

    /** As with Nearby, only the other side hears about a disconnect we asked for. */
    override fun disconnect(endpointId: String) {
        val peer = connections.firstOrNull { it.id == endpointId } ?: return
        connections -= peer
        peer.connections -= this
        peer.emit(TransportEvent.Disconnected(id))
    }

    override fun stopAll() {
        stopAdvertising()
        stopDiscovery()
        connections.toList().forEach { disconnect(it.id) }
        pending.clear()
    }
}
