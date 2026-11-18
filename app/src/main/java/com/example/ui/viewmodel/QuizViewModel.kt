package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.MistakeRepository
import com.example.data.QuestionRepository
import com.example.domain.GameEngine
import com.example.domain.ScoreCalculator
import com.example.model.GameErrorReason
import com.example.model.GameMode
import com.example.model.GameUiState
import com.example.model.MistakeItem
import com.example.model.QuizCategory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class QuizViewModel(
    private val questionRepository: QuestionRepository,
    private val mistakeRepository: MistakeRepository,
    private val engine: GameEngine = GameEngine()
) : ViewModel() {

    val gameState: StateFlow<GameUiState> = engine.state

    private val _selectedCategory = MutableStateFlow(QuizCategory.VOCABULARY)
    val selectedCategory: StateFlow<QuizCategory> = _selectedCategory.asStateFlow()

    private val _activeMode = MutableStateFlow(GameMode.SOLO)
    val activeMode: StateFlow<GameMode> = _activeMode.asStateFlow()

    private val bankQuestionsFlow = flow {
        emit(questionRepository.getAllBankQuestions().associateBy { it.id })
    }

    val mistakes: StateFlow<List<MistakeItem>> = combine(
        mistakeRepository.observeMistakes(),
        bankQuestionsFlow
    ) { entities, questionMap ->
        entities.mapNotNull { entity ->
            questionMap[entity.questionId]?.let { question ->
                MistakeItem(entity = entity, question = question)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private var timerJob: Job? = null
    private var advanceJob: Job? = null
    private var questionStartTimeMs: Long = 0L

    fun selectCategory(category: QuizCategory) {
        _selectedCategory.value = category
    }

    fun startSoloRound(
        category: QuizCategory = _selectedCategory.value,
        seed: Long = System.currentTimeMillis()
    ) {
        cancelTimers()
        _selectedCategory.value = category
        _activeMode.value = GameMode.SOLO
        engine.setLoading()

        viewModelScope.launch {
            runCatching {
                questionRepository.getQuestionsForRound(
                    category = category,
                    count = QuestionRepository.DEFAULT_ROUND_SIZE,
                    seed = seed
                )
            }.onSuccess { questions ->
                if (questions.isEmpty()) {
                    engine.setError(GameErrorReason.EMPTY_QUESTIONS)
                } else {
                    engine.start(questions)
                    startQuestionCountdown()
                }
            }.onFailure {
                engine.setError(GameErrorReason.LOAD_FAILED)
            }
        }
    }

    fun startMistakesReviewRound() {
        cancelTimers()
        _activeMode.value = GameMode.MISTAKES_REVIEW
        engine.setLoading()

        viewModelScope.launch {
            val reviewQuestions = mistakes.value.map { it.question }.shuffled()
            if (reviewQuestions.isEmpty()) {
                engine.setError(GameErrorReason.EMPTY_QUESTIONS)
            } else {
                engine.start(reviewQuestions)
                startQuestionCountdown()
            }
        }
    }

    fun submitAnswer(choiceIndex: Int) {
        val current = engine.state.value as? GameUiState.ActiveQuestion ?: return
        if (current.isAnswerSubmitted) return

        timerJob?.cancel()
        val elapsedMs = (System.currentTimeMillis() - questionStartTimeMs)
            .coerceIn(0L, ScoreCalculator.QUESTION_DURATION_MS)
        handleSubmission(choiceIndex = choiceIndex, elapsedMs = elapsedMs)
    }

    fun replayCurrentMode() {
        when (_activeMode.value) {
            GameMode.MISTAKES_REVIEW -> startMistakesReviewRound()
            else -> startSoloRound(_selectedCategory.value)
        }
    }

    fun exitGame() {
        cancelTimers()
        engine.reset()
    }

    fun removeMistake(questionId: String) {
        viewModelScope.launch {
            mistakeRepository.removeMistake(questionId)
        }
    }

    fun clearAllMistakes() {
        viewModelScope.launch {
            mistakeRepository.clearAllMistakes()
        }
    }

    private fun startQuestionCountdown() {
        timerJob?.cancel()
        questionStartTimeMs = System.currentTimeMillis()
        timerJob = viewModelScope.launch {
            while (true) {
                val elapsed = System.currentTimeMillis() - questionStartTimeMs
                val remaining = (ScoreCalculator.QUESTION_DURATION_MS - elapsed).coerceAtLeast(0L)
                engine.updateRemainingTime(remaining)
                if (remaining <= 0L) {
                    handleSubmission(
                        choiceIndex = TIMEOUT_CHOICE,
                        elapsedMs = ScoreCalculator.QUESTION_DURATION_MS
                    )
                    break
                }
                delay(TIMER_TICK_MS)
            }
        }
    }

    private fun handleSubmission(choiceIndex: Int, elapsedMs: Long) {
        val record = engine.submitAnswer(choiceIndex, elapsedMs) ?: return
        viewModelScope.launch {
            if (!record.isCorrect) {
                mistakeRepository.recordMistake(record.question)
            } else if (_activeMode.value == GameMode.MISTAKES_REVIEW) {
                mistakeRepository.removeMistake(record.question.id)
            }
        }
        advanceJob?.cancel()
        advanceJob = viewModelScope.launch {
            delay(FEEDBACK_DELAY_MS)
            engine.nextQuestion()
            if (engine.state.value is GameUiState.ActiveQuestion) {
                startQuestionCountdown()
            }
        }
    }

    private fun cancelTimers() {
        timerJob?.cancel()
        advanceJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        cancelTimers()
    }

    companion object {
        private const val TIMER_TICK_MS = 50L
        private const val FEEDBACK_DELAY_MS = 1_500L
        private const val TIMEOUT_CHOICE = -1

        fun provideFactory(
            questionRepository: QuestionRepository,
            mistakeRepository: MistakeRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return QuizViewModel(
                    questionRepository = questionRepository,
                    mistakeRepository = mistakeRepository
                ) as T
            }
        }
    }
}
