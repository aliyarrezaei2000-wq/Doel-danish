package com.example.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
sealed interface Msg {

    @Serializable
    data class Hello(val name: String) : Msg

    @Serializable
    data class StartGame(
        val category: String,
        val questionIds: List<String>,
        val mathSeed: Long,
        val totalQuestions: Int
    ) : Msg

    @Serializable
    data class ShowQuestion(val index: Int) : Msg

    @Serializable
    data class Answer(
        val index: Int,
        val choice: Int,
        val elapsedMs: Long
    ) : Msg

    @Serializable
    data class RoundResult(
        val index: Int,
        val correctIndex: Int,
        val hostScore: Int,
        val guestScore: Int,
        val hostCorrect: Boolean,
        val guestCorrect: Boolean
    ) : Msg

    @Serializable
    data class GameOver(
        val hostScore: Int,
        val guestScore: Int
    ) : Msg

    @Serializable
    data object Rematch : Msg

    @Serializable
    data object Leave : Msg
}

object ProtocolJson {
    val serializer: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
    }

    fun encode(msg: Msg): ByteArray =
        serializer.encodeToString(Msg.serializer(), msg).encodeToByteArray()

    fun decode(bytes: ByteArray): Msg =
        serializer.decodeFromString(Msg.serializer(), bytes.decodeToString())
}
