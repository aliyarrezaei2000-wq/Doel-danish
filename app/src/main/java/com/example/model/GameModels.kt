package com.example.model

enum class GameMode {
    SOLO,
    MISTAKES_REVIEW,
    DUEL_STAGE_2_PLACEHOLDER
}

enum class GameErrorReason {
    EMPTY_QUESTIONS,
    LOAD_FAILED
}

data class AnswerRecord(
    val question: Question,
    val selectedIndex: Int?,
    val isCorrect: Boolean,
    val pointsEarned: Int,
    val elapsedMs: Long
)

sealed interface GameUiState {
    data object Idle : GameUiState

    data object Loading : GameUiState

    data class ActiveQuestion(
        val questionIndex: Int,
        val totalQuestions: Int,
        val question: Question,
        val score: Int,
        val timeRemainingMs: Long,
        val totalTimeMs: Long,
        val selectedIndex: Int? = null,
        val isAnswerSubmitted: Boolean = false,
        val lastPointsEarned: Int = 0,
        val isCorrect: Boolean? = null
    ) : GameUiState

    data class Finished(
        val totalScore: Int,
        val correctCount: Int,
        val totalQuestions: Int,
        val wrongAnswers: List<AnswerRecord>,
        val allRecords: List<AnswerRecord>
    ) : GameUiState

    data class Error(
        val reason: GameErrorReason
    ) : GameUiState
}
