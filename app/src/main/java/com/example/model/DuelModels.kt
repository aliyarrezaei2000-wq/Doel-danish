package com.example.model

import com.example.network.ConnState

enum class DuelRole {
    HOST,
    GUEST
}

data class RoundFeedback(
    val correctIndex: Int,
    val myCorrect: Boolean,
    val opponentCorrect: Boolean,
    val myPointsEarned: Int,
    val opponentPointsEarned: Int
)

sealed interface DuelUiState {

    data class Lobby(
        val connState: ConnState = ConnState.Idle,
        val role: DuelRole? = null,
        val localPlayerName: String = "",
        val opponentName: String? = null,
        val selectedCategory: QuizCategory = QuizCategory.VOCABULARY,
        val selectedQuestionCount: Int = 10
    ) : DuelUiState

    data class Playing(
        val questionIndex: Int,
        val totalQuestions: Int,
        val question: Question,
        val myScore: Int,
        val opponentScore: Int,
        val myName: String,
        val opponentName: String,
        val timeRemainingMs: Long,
        val totalTimeMs: Long,
        val myChoice: Int? = null,
        val waitingForOpponent: Boolean = false,
        val roundResult: RoundFeedback? = null
    ) : DuelUiState

    data class Finished(
        val myScore: Int,
        val opponentScore: Int,
        val myName: String,
        val opponentName: String,
        val wrongAnswers: List<AnswerRecord>,
        val totalQuestions: Int,
        val myCorrectCount: Int,
        val rematchRequestedByMe: Boolean = false,
        val rematchRequestedByOpponent: Boolean = false,
        val opponentLeft: Boolean = false
    ) : DuelUiState

    data class Disconnected(
        val reason: String,
        val wasInMatch: Boolean,
        val previousRole: DuelRole?
    ) : DuelUiState
}
