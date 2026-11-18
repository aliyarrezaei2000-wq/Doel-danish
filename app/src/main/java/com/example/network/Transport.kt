package com.example.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

sealed interface ConnState {
    data object Idle : ConnState
    data object Advertising : ConnState
    data object Discovering : ConnState
    data object Connecting : ConnState
    data object Connected : ConnState
    data class Lost(val reason: String) : ConnState
}

interface Transport {
    val incoming: Flow<Msg>
    val state: StateFlow<ConnState>
    suspend fun startHosting(playerName: String)
    suspend fun startJoining(playerName: String)
    suspend fun send(msg: Msg)
    fun close()
}
