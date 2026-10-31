package com.example.minorityvault.net

import kotlinx.coroutines.flow.Flow

/**
 * Moves bytes between phones. [NearbyTransport] does it over Bluetooth and Wi-Fi; [FakeNetwork] does it in memory
 * for tests. Sessions only ever talk to this interface.
 */
interface Transport {
    /** Everything that happens on the radio, in order. Collect it from one place only. */
    val events: Flow<TransportEvent>

    /** Lets nearby phones find this one under [endpointName]. */
    suspend fun startAdvertising(endpointName: String): TransportResult

    suspend fun startDiscovery(): TransportResult

    fun stopAdvertising()

    fun stopDiscovery()

    /** Asks [endpointId] to connect. Both sides then get [TransportEvent.ConnectionRequested] and must accept. */
    suspend fun requestConnection(endpointId: String, localName: String): TransportResult

    suspend fun acceptConnection(endpointId: String): TransportResult

    fun rejectConnection(endpointId: String)

    /** Delivers [bytes] whole and in order. */
    fun send(endpointId: String, bytes: ByteArray)

    fun disconnect(endpointId: String)

    /** Stops advertising and discovery and drops every connection. */
    fun stopAll()
}

sealed interface TransportEvent {
    data class EndpointFound(val endpointId: String, val name: String) : TransportEvent

    data class EndpointLost(val endpointId: String) : TransportEvent

    data class ConnectionRequested(val endpointId: String, val name: String) : TransportEvent

    data class Connected(val endpointId: String) : TransportEvent

    data class ConnectionFailed(val endpointId: String) : TransportEvent

    data class Disconnected(val endpointId: String) : TransportEvent

    data class Received(val endpointId: String, val bytes: ByteArray) : TransportEvent {
        override fun equals(other: Any?) =
            other is Received && other.endpointId == endpointId && other.bytes.contentEquals(bytes)

        override fun hashCode() = 31 * endpointId.hashCode() + bytes.contentHashCode()
    }
}

sealed interface TransportResult {
    data object Ok : TransportResult

    data class Failed(val failure: TransportFailure) : TransportResult
}

enum class TransportFailure {
    /** A runtime permission is missing; see [NearbyPermissions]. */
    MISSING_PERMISSION,

    /** Bluetooth or Wi-Fi is off, or this device can't do it (e.g. an emulator). */
    RADIO_UNAVAILABLE,

    /** Older Android versions need location services switched on to find nearby phones. */
    LOCATION_OFF,

    /** The endpoint went away before we could reach it. */
    ENDPOINT_UNAVAILABLE,
    OTHER,
}
