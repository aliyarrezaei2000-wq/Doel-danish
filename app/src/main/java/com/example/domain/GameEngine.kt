package com.example.domain

import com.example.model.AnswerRecord
import com.example.model.GameErrorReason
import com.example.model.GameUiState
import com.example.model.Question
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Pure Kotlin game state machine with zero Android dependencies.
 * Designed so Stage 2 local two-phone duels can drive or synchronize the exact same engine.
 */
class GameEngine(
    private val questionDurationMs: Long = ScoreCalculator.QUESTION_DURATION_MS
) {
    private val _state = MutableStateFlow<GameUiState>(GameUiState.Idle)
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    private var questions: List<Question> = emptyList()
    private var currentIndex: Int = 0
    private var totalScore: Int = 0
    private val records = mutableListOf<AnswerRecord>()

    fun setLoading() {
        _state.value = GameUiState.Loading
    }

    fun setError(reason: GameErrorReason) {
        _state.value = GameUiState.Error(reason)
    }

    fun reset() {
        questions = emptyList()
        currentIndex = 0
        totalScore = 0
        records.clear()
        _state.value = GameUiState.Idle
    }

    fun start(questions: List<Question>) {
        if (questions.isEmpty()) {
            this.questions = emptyList()
            _state.value = GameUiState.Error(GameErrorReason.EMPTY_QUESTIONS)
            return
        }
        this.questions = questions
        this.currentIndex = 0
        this.totalScore = 0
        this.records.clear()
        emitCurrentQuestion()
    }

    fun updateRemainingTime(remainingMs: Long) {
        val current = _state.value as? GameUiState.ActiveQuestion ?: return
        if (current.isAnswerSubmitted) return
        _state.value = current.copy(
            timeRemainingMs = remainingMs.coerceIn(0L, questionDurationMs)
        )
    }

    fun submitAnswer(choice: Int, elapsedMs: Long): AnswerRecord? {
        val current = _state.value as? GameUiState.ActiveQuestion ?: return null
        if (current.isAnswerSubmitted) return null

        val question = current.question
        val validChoice = choice.takeIf { it in question.options.indices }
        val isTimedOut = validChoice == null || elapsedMs >= questionDurationMs
        val isCorrect = !isTimedOut && validChoice == question.correctIndex
        val points = ScoreCalculator.calculate(
            isCorrect = isCorrect,
            elapsedMs = if (isTimedOut) questionDurationMs else elapsedMs
        )

        totalScore += points
        val record = AnswerRecord(
            question = question,
            selectedIndex = validChoice,
            isCorrect = isCorrect,
            pointsEarned = points,
            elapsedMs = elapsedMs.coerceIn(0L, questionDurationMs)
        )
        records.add(record)

        _state.value = current.copy(
            score = totalScore,
            selectedIndex = validChoice,
            isAnswerSubmitted = true,
            lastPointsEarned = points,
            isCorrect = isCorrect,
            timeRemainingMs = if (isTimedOut) 0L else current.timeRemainingMs
        )
        return record
    }

    fun nextQuestion() {
        val current = _state.value
        if (current !is GameUiState.ActiveQuestion) return

        val nextIdx = currentIndex + 1
        if (nextIdx < questions.size) {
            currentIndex = nextIdx
            emitCurrentQuestion()
        } else {
            val allRecords = records.toList()
            val wrongAnswers = allRecords.filter { !it.isCorrect }
            val correctCount = allRecords.count { it.isCorrect }
            _state.value = GameUiState.Finished(
                totalScore = totalScore,
                correctCount = correctCount,
                totalQuestions = questions.size,
                wrongAnswers = wrongAnswers,
                allRecords = allRecords
            )
        }
    }

    private fun emitCurrentQuestion() {
        _state.value = GameUiState.ActiveQuestion(
            questionIndex = currentIndex,
            totalQuestions = questions.size,
            question = questions[currentIndex],
            score = totalScore,
            timeRemainingMs = questionDurationMs,
            totalTimeMs = questionDurationMs
        )
    }
}
