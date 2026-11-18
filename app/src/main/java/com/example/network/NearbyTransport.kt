package com.example.network

import android.content.Context
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
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class NearbyTransport(
    context: Context
) : Transport {

    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context.applicationContext)

    private val _incoming = MutableSharedFlow<Msg>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val incoming: Flow<Msg> = _incoming.asSharedFlow()

    private val _state = MutableStateFlow<ConnState>(ConnState.Idle)
    override val state: StateFlow<ConnState> = _state.asStateFlow()

    private var activeEndpointId: String? = null
    private var pendingEndpointId: String? = null
    private var localPlayerName: String = ""
    private var isClosingIntentionally = false

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type != Payload.Type.BYTES) return
            val bytes = payload.asBytes() ?: return
            runCatching { ProtocolJson.decode(bytes) }
                .onSuccess { msg -> _incoming.tryEmit(msg) }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) = Unit
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            // Strictly enforce 1-to-1 duel (exactly two devices).
            val busy = activeEndpointId != null || (pendingEndpointId != null && pendingEndpointId != endpointId)
            if (busy || isClosingIntentionally) {
                connectionsClient.rejectConnection(endpointId)
                return
            }
            pendingEndpointId = endpointId
            _state.value = ConnState.Connecting
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            pendingEndpointId = null
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    activeEndpointId = endpointId
                    connectionsClient.stopAdvertising()
                    connectionsClient.stopDiscovery()
                    _state.value = ConnState.Connected
                    sendHelloBestEffort()
                }
                else -> {
                    if (!isClosingIntentionally) {
                        _state.value = ConnState.Lost(
                            result.status.statusMessage ?: "connection_rejected"
                        )
                    }
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            if (endpointId == activeEndpointId || endpointId == pendingEndpointId) {
                activeEndpointId = null
                pendingEndpointId = null
                if (!isClosingIntentionally) {
                    _state.value = ConnState.Lost("disconnected")
                }
            }
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            if (activeEndpointId != null || pendingEndpointId != null || isClosingIntentionally) {
                return
            }
            pendingEndpointId = endpointId
            connectionsClient.stopDiscovery()
            _state.value = ConnState.Connecting
            connectionsClient.requestConnection(
                localPlayerName,
                endpointId,
                connectionLifecycleCallback
            ).addOnFailureListener { err ->
                pendingEndpointId = null
                if (!isClosingIntentionally) {
                    _state.value = ConnState.Lost(err.localizedMessage ?: "request_failed")
                }
            }
        }

        override fun onEndpointLost(endpointId: String) = Unit
    }

    override suspend fun startHosting(playerName: String) {
        resetInternalState()
        localPlayerName = playerName
        _state.value = ConnState.Advertising

        val options = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_POINT_TO_POINT)
            .build()

        suspendCancellableCoroutine { cont ->
            connectionsClient.startAdvertising(
                playerName,
                SERVICE_ID,
                connectionLifecycleCallback,
                options
            ).addOnSuccessListener {
                if (cont.isActive) cont.resume(Unit)
            }.addOnFailureListener { err ->
                if (!isClosingIntentionally) {
                    _state.value = ConnState.Lost(err.localizedMessage ?: "advertising_failed")
                }
                if (cont.isActive) cont.resume(Unit)
            }
        }
    }

    override suspend fun startJoining(playerName: String) {
        resetInternalState()
        localPlayerName = playerName
        _state.value = ConnState.Discovering

        val options = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_POINT_TO_POINT)
            .build()

        suspendCancellableCoroutine { cont ->
            connectionsClient.startDiscovery(
                SERVICE_ID,
                endpointDiscoveryCallback,
                options
            ).addOnSuccessListener {
                if (cont.isActive) cont.resume(Unit)
            }.addOnFailureListener { err ->
                if (!isClosingIntentionally) {
                    _state.value = ConnState.Lost(err.localizedMessage ?: "discovery_failed")
                }
                if (cont.isActive) cont.resume(Unit)
            }
        }
    }

    override suspend fun send(msg: Msg) {
        val endpointId = activeEndpointId ?: return
        val bytes = ProtocolJson.encode(msg)
        val payload = Payload.fromBytes(bytes)
        connectionsClient.sendPayload(endpointId, payload)
    }

    override fun close() {
        isClosingIntentionally = true
        activeEndpointId = null
        pendingEndpointId = null
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        _state.value = ConnState.Idle
    }

    private fun resetInternalState() {
        isClosingIntentionally = false
        activeEndpointId = null
        pendingEndpointId = null
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
    }

    private fun sendHelloBestEffort() {
        val endpointId = activeEndpointId ?: return
        val bytes = ProtocolJson.encode(Msg.Hello(localPlayerName))
        connectionsClient.sendPayload(endpointId, Payload.fromBytes(bytes))
    }

    companion object {
        const val SERVICE_ID = "com.aistudio.dueldanesh.duel.v1"
    }
}
