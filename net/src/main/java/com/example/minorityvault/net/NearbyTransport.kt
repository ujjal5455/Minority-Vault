package com.example.minorityvault.net

import android.content.Context
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * [Transport] over Google Play services Nearby Connections, using [Strategy.P2P_STAR]: the host is the hub and
 * every other phone connects to it. Works offline over Bluetooth and Wi-Fi, and Nearby encrypts every connection.
 */
class NearbyTransport(context: Context) : Transport {
    private val client: ConnectionsClient = Nearby.getConnectionsClient(context)
    private val channel = Channel<TransportEvent>(Channel.UNLIMITED)

    override val events: Flow<TransportEvent> = channel.receiveAsFlow()

    private val payloads = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val bytes = payload.asBytes()
            if (payload.type == Payload.Type.BYTES && bytes != null) {
                channel.trySend(TransportEvent.Received(endpointId, bytes))
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) = Unit
    }

    private val connections = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            channel.trySend(TransportEvent.ConnectionRequested(endpointId, info.endpointName))
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            val event = if (resolution.status.isSuccess) {
                TransportEvent.Connected(endpointId)
            } else {
                TransportEvent.ConnectionFailed(endpointId)
            }
            channel.trySend(event)
        }

        override fun onDisconnected(endpointId: String) {
            channel.trySend(TransportEvent.Disconnected(endpointId))
        }
    }

    private val discovery = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            channel.trySend(TransportEvent.EndpointFound(endpointId, info.endpointName))
        }

        override fun onEndpointLost(endpointId: String) {
            channel.trySend(TransportEvent.EndpointLost(endpointId))
        }
    }

    override suspend fun startAdvertising(endpointName: String): TransportResult = client.startAdvertising(
        endpointName,
        SERVICE_ID,
        connections,
        AdvertisingOptions.Builder().setStrategy(Strategy.P2P_STAR).build(),
    ).result()

    override suspend fun startDiscovery(): TransportResult = client.startDiscovery(
        SERVICE_ID,
        discovery,
        DiscoveryOptions.Builder().setStrategy(Strategy.P2P_STAR).build(),
    ).result()

    override fun stopAdvertising() = client.stopAdvertising()

    override fun stopDiscovery() = client.stopDiscovery()

    override suspend fun requestConnection(endpointId: String, localName: String): TransportResult =
        client.requestConnection(localName, endpointId, connections).result()

    override suspend fun acceptConnection(endpointId: String): TransportResult =
        client.acceptConnection(endpointId, payloads).result()

    override fun rejectConnection(endpointId: String) {
        client.rejectConnection(endpointId)
    }

    override fun send(endpointId: String, bytes: ByteArray) {
        if (bytes.size > ConnectionsClient.MAX_BYTES_DATA_SIZE) {
            Log.e(TAG, "Dropped a ${bytes.size}-byte message: over Nearby's limit")
            return
        }
        client.sendPayload(endpointId, Payload.fromBytes(bytes))
    }

    override fun disconnect(endpointId: String) = client.disconnectFromEndpoint(endpointId)

    override fun stopAll() {
        client.stopAdvertising()
        client.stopDiscovery()
        client.stopAllEndpoints()
    }
}

private suspend fun Task<Void>.result(): TransportResult = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        val error = task.exception
        continuation.resume(if (error == null) TransportResult.Ok else error.toTransportResult())
    }
}

private fun Exception.toTransportResult(): TransportResult {
    val code = (this as? ApiException)?.statusCode
    return when (code) {
        // Asking twice for something already running is not a failure.
        ConnectionsStatusCodes.STATUS_ALREADY_ADVERTISING,
        ConnectionsStatusCodes.STATUS_ALREADY_DISCOVERING,
        ConnectionsStatusCodes.STATUS_ALREADY_CONNECTED_TO_ENDPOINT,
        -> TransportResult.Ok

        in MISSING_PERMISSION_CODES -> TransportResult.Failed(TransportFailure.MISSING_PERMISSION)

        ConnectionsStatusCodes.MISSING_SETTING_LOCATION_MUST_BE_ON ->
            TransportResult.Failed(TransportFailure.LOCATION_OFF)

        ConnectionsStatusCodes.STATUS_RADIO_ERROR -> TransportResult.Failed(TransportFailure.RADIO_UNAVAILABLE)

        ConnectionsStatusCodes.STATUS_ENDPOINT_UNKNOWN,
        ConnectionsStatusCodes.STATUS_ENDPOINT_IO_ERROR,
        ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED,
        -> TransportResult.Failed(TransportFailure.ENDPOINT_UNAVAILABLE)

        else -> {
            Log.w(TAG, "Nearby call failed with status $code", this)
            TransportResult.Failed(TransportFailure.OTHER)
        }
    }
}

private const val TAG = "NearbyTransport"

/** Only phones running this game see each other. */
private const val SERVICE_ID = "com.example.minorityvault"

private val MISSING_PERMISSION_CODES = setOf(
    ConnectionsStatusCodes.MISSING_PERMISSION_NEARBY_WIFI_DEVICES,
    ConnectionsStatusCodes.MISSING_PERMISSION_BLUETOOTH,
    ConnectionsStatusCodes.MISSING_PERMISSION_BLUETOOTH_ADMIN,
    ConnectionsStatusCodes.MISSING_PERMISSION_ACCESS_WIFI_STATE,
    ConnectionsStatusCodes.MISSING_PERMISSION_CHANGE_WIFI_STATE,
    ConnectionsStatusCodes.MISSING_PERMISSION_ACCESS_COARSE_LOCATION,
    ConnectionsStatusCodes.MISSING_PERMISSION_ACCESS_FINE_LOCATION,
    ConnectionsStatusCodes.MISSING_PERMISSION_BLUETOOTH_SCAN,
    ConnectionsStatusCodes.MISSING_PERMISSION_BLUETOOTH_ADVERTISE,
    ConnectionsStatusCodes.MISSING_PERMISSION_BLUETOOTH_CONNECT,
)
