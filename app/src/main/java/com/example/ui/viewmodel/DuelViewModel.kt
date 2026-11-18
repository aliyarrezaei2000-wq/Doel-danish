package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.MistakeRepository
import com.example.data.QuestionRepository
import com.example.domain.DuelEngine
import com.example.model.AnswerRecord
import com.example.model.DuelUiState
import com.example.model.QuizCategory
import com.example.network.Transport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DuelViewModel(
    private val transport: Transport,
    questionRepository: QuestionRepository,
    private val mistakeRepository: MistakeRepository,
    defaultPlayerName: String
) : ViewModel() {

    private val _playerName = MutableStateFlow(defaultPlayerName)
    val playerName: StateFlow<String> = _playerName.asStateFlow()

    private val engine: DuelEngine = DuelEngine(
        transport = transport,
        questionRepository = questionRepository,
        scope = viewModelScope,
        onMatchFinished = { wrongAnswers ->
            persistWrongAnswers(wrongAnswers)
        }
    )

    val duelState: StateFlow<DuelUiState> = engine.state

    fun updatePlayerName(name: String) {
        _playerName.value = name
    }

    fun startHosting(fallbackName: String) {
        val chosenName = _playerName.value.trim().ifEmpty { fallbackName }
        viewModelScope.launch {
            engine.startHosting(chosenName)
        }
    }

    fun startJoining(fallbackName: String) {
        val chosenName = _playerName.value.trim().ifEmpty { fallbackName }
        viewModelScope.launch {
            engine.startJoining(chosenName)
        }
    }

    fun selectCategory(category: QuizCategory) {
        engine.updateCategory(category)
    }

    fun selectQuestionCount(count: Int) {
        engine.updateQuestionCount(count)
    }

    fun startMatch() {
        engine.startMatch()
    }

    fun submitAnswer(choice: Int) {
        engine.submitAnswer(choice)
    }

    fun requestRematch() {
        engine.requestRematch()
    }

    fun reconnect() {
        engine.reconnect()
    }

    fun cancelOrLeaveDuel() {
        engine.leave()
    }

    private fun persistWrongAnswers(wrongAnswers: List<AnswerRecord>) {
        if (wrongAnswers.isEmpty()) return
        viewModelScope.launch {
            wrongAnswers.forEach { record ->
                mistakeRepository.recordMistake(record.question)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        transport.close()
    }

    companion object {
        fun provideFactory(
            transport: Transport,
            questionRepository: QuestionRepository,
            mistakeRepository: MistakeRepository,
            defaultPlayerName: String
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DuelViewModel(
                    transport = transport,
                    questionRepository = questionRepository,
                    mistakeRepository = mistakeRepository,
                    defaultPlayerName = defaultPlayerName
                ) as T
            }
        }
    }
}
