package com.example.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stub implementation of [Transport] reserved for future local Wi-Fi NSD + TCP socket support.
 * Allows swapping transports in DuelEngine/DuelViewModel without modifying any game logic.
 */
class SocketTransport : Transport {

    private val _incoming = MutableSharedFlow<Msg>()
    override val incoming: Flow<Msg> = _incoming.asSharedFlow()

    private val _state = MutableStateFlow<ConnState>(ConnState.Idle)
    override val state: StateFlow<ConnState> = _state.asStateFlow()

    override suspend fun startHosting(playerName: String) {
        _state.value = ConnState.Lost("SocketTransport (NSD + TCP) is not enabled in this build.")
    }

    override suspend fun startJoining(playerName: String) {
        _state.value = ConnState.Lost("SocketTransport (NSD + TCP) is not enabled in this build.")
    }

    override suspend fun send(msg: Msg) = Unit

    override fun close() {
        _state.value = ConnState.Idle
    }
}
