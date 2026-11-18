package com.example.domain

import com.example.data.QuestionRepository
import com.example.model.AnswerRecord
import com.example.model.DuelRole
import com.example.model.DuelUiState
import com.example.model.GameUiState
import com.example.model.Question
import com.example.model.QuizCategory
import com.example.model.RoundFeedback
import com.example.network.ConnState
import com.example.network.Msg
import com.example.network.Transport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Pure Kotlin two-player duel coordinator built on [GameEngine] and [ScoreCalculator].
 * Only the HOST acts as the authoritative source of truth for timing, scoring, and progression.
 * The GUEST displays synchronized state and sends its own [Msg.Answer].
 */
class DuelEngine(
    private val transport: Transport,
    private val questionRepository: QuestionRepository,
    private val scope: CoroutineScope,
    private val clockMs: () -> Long = { System.currentTimeMillis() },
    private val questionDurationMs: Long = ScoreCalculator.QUESTION_DURATION_MS,
    private val roundResultDelayMs: Long = 2_000L,
    private val timerTickMs: Long = 100L,
    private val seedGenerator: () -> Long = { clockMs() },
    private val onMatchFinished: (List<AnswerRecord>) -> Unit = {}
) {
    private val hostGameEngine = GameEngine(questionDurationMs = questionDurationMs)

    private val _state = MutableStateFlow<DuelUiState>(DuelUiState.Lobby())
    val state: StateFlow<DuelUiState> = _state.asStateFlow()

    private var role: DuelRole? = null
    private var localPlayerName: String = ""
    private var opponentName: String? = null
    private var selectedCategory: QuizCategory = QuizCategory.VOCABULARY
    private var selectedQuestionCount: Int = 10

    private var activeQuestions: List<Question> = emptyList()
    private var currentQuestionIndex: Int = 0
    private var questionShownAtMs: Long = 0L

    private var hostScore: Int = 0
    private var guestScore: Int = 0
    private var myCorrectCount: Int = 0
    private val myWrongRecords = mutableListOf<AnswerRecord>()
    private val myChoicesByRound = mutableMapOf<Int, Int>()

    private var hostRoundSubmission: Pair<Int, Long>? = null
    private var guestRoundSubmission: Pair<Int, Long>? = null
    private var roundResolved: Boolean = false

    private var roundTimerJob: Job? = null
    private var roundAdvanceJob: Job? = null
    private var helloSentForCurrentConnection: Boolean = false
    private var intentionalLeave: Boolean = false

    init {
        scope.launch {
            transport.state.collect { connState ->
                handleConnectionState(connState)
            }
        }
        scope.launch {
            transport.incoming.collect { msg ->
                handleIncomingMessage(msg)
            }
        }
    }

    suspend fun startHosting(playerName: String) {
        intentionalLeave = false
        helloSentForCurrentConnection = false
        cancelRoundJobs()
        role = DuelRole.HOST
        localPlayerName = playerName
        opponentName = null
        emitLobby(ConnState.Advertising)
        transport.startHosting(playerName)
    }

    suspend fun startJoining(playerName: String) {
        intentionalLeave = false
        helloSentForCurrentConnection = false
        cancelRoundJobs()
        role = DuelRole.GUEST
        localPlayerName = playerName
        opponentName = null
        emitLobby(ConnState.Discovering)
        transport.startJoining(playerName)
    }

    fun updateCategory(category: QuizCategory) {
        selectedCategory = category
        val current = _state.value
        if (current is DuelUiState.Lobby) {
            _state.value = current.copy(selectedCategory = category)
        }
    }

    fun updateQuestionCount(count: Int) {
        selectedQuestionCount = count.coerceIn(5, 15)
        val current = _state.value
        if (current is DuelUiState.Lobby) {
            _state.value = current.copy(selectedQuestionCount = selectedQuestionCount)
        }
    }

    fun startMatch() {
        if (role != DuelRole.HOST) return
        if (transport.state.value != ConnState.Connected) return

        scope.launch {
            val seed = seedGenerator()
            val questions = questionRepository.getQuestionsForRound(
                category = selectedCategory,
                count = selectedQuestionCount,
                seed = seed
            )
            if (questions.isEmpty()) return@launch

            prepareMatchState(questions)
            hostGameEngine.start(questions)

            transport.send(
                Msg.StartGame(
                    category = selectedCategory.id,
                    questionIds = questions.map { it.id },
                    mathSeed = seed,
                    totalQuestions = questions.size
                )
            )
            hostShowQuestion(0)
        }
    }

    fun submitAnswer(choice: Int) {
        val current = _state.value as? DuelUiState.Playing ?: return
        if (current.myChoice != null || current.roundResult != null) return

        val elapsedMs = (clockMs() - questionShownAtMs).coerceIn(0L, questionDurationMs)
        myChoicesByRound[currentQuestionIndex] = choice

        _state.value = current.copy(
            myChoice = choice,
            waitingForOpponent = true
        )

        if (role == DuelRole.GUEST) {
            roundTimerJob?.cancel()
            scope.launch {
                transport.send(
                    Msg.Answer(
                        index = currentQuestionIndex,
                        choice = choice,
                        elapsedMs = elapsedMs
                    )
                )
            }
        } else if (role == DuelRole.HOST) {
            hostGameEngine.submitAnswer(choice, elapsedMs)
            hostRoundSubmission = choice to elapsedMs
            if (guestRoundSubmission != null && !roundResolved) {
                resolveHostRound(currentQuestionIndex)
            }
        }
    }

    fun requestRematch() {
        val current = _state.value as? DuelUiState.Finished ?: return
        if (current.rematchRequestedByMe || current.opponentLeft) return

        val updated = current.copy(rematchRequestedByMe = true)
        _state.value = updated
        scope.launch {
            transport.send(Msg.Rematch)
            if (updated.rematchRequestedByOpponent && role == DuelRole.HOST) {
                startMatch()
            }
        }
    }

    fun reconnect() {
        val previousRole = role ?: return
        val name = localPlayerName
        scope.launch {
            when (previousRole) {
                DuelRole.HOST -> startHosting(name)
                DuelRole.GUEST -> startJoining(name)
            }
        }
    }

    fun leave() {
        intentionalLeave = true
        cancelRoundJobs()
        val wasConnected = transport.state.value == ConnState.Connected
        if (wasConnected) {
            scope.launch {
                runCatching { transport.send(Msg.Leave) }
                transport.close()
            }
        } else {
            transport.close()
        }
        resetToLobby()
    }

    fun resetToLobby() {
        cancelRoundJobs()
        role = null
        opponentName = null
        activeQuestions = emptyList()
        hostRoundSubmission = null
        guestRoundSubmission = null
        roundResolved = false
        helloSentForCurrentConnection = false
        _state.value = DuelUiState.Lobby(
            connState = ConnState.Idle,
            role = null,
            localPlayerName = localPlayerName,
            opponentName = null,
            selectedCategory = selectedCategory,
            selectedQuestionCount = selectedQuestionCount
        )
    }

    private suspend fun handleConnectionState(connState: ConnState) {
        when (connState) {
            ConnState.Idle -> {
                helloSentForCurrentConnection = false
                if (_state.value is DuelUiState.Lobby) {
                    emitLobby(ConnState.Idle)
                }
            }
            ConnState.Advertising,
            ConnState.Discovering,
            ConnState.Connecting -> {
                if (_state.value is DuelUiState.Lobby || _state.value is DuelUiState.Disconnected) {
                    emitLobby(connState)
                }
            }
            ConnState.Connected -> {
                if (!helloSentForCurrentConnection) {
                    helloSentForCurrentConnection = true
                    transport.send(Msg.Hello(localPlayerName))
                }
                if (_state.value is DuelUiState.Lobby || _state.value is DuelUiState.Disconnected) {
                    emitLobby(ConnState.Connected)
                }
            }
            is ConnState.Lost -> {
                helloSentForCurrentConnection = false
                cancelRoundJobs()
                if (!intentionalLeave && role != null) {
                    val wasInMatch = _state.value is DuelUiState.Playing
                    _state.value = DuelUiState.Disconnected(
                        reason = connState.reason,
                        wasInMatch = wasInMatch,
                        previousRole = role
                    )
                }
            }
        }
    }

    private suspend fun handleIncomingMessage(msg: Msg) {
        when (msg) {
            is Msg.Hello -> {
                opponentName = msg.name
                val current = _state.value
                if (current is DuelUiState.Lobby) {
                    _state.value = current.copy(opponentName = msg.name)
                } else if (current is DuelUiState.Playing) {
                    _state.value = current.copy(opponentName = msg.name)
                }
            }

            is Msg.StartGame -> {
                if (role != DuelRole.GUEST) return
                val category = QuizCategory.fromId(msg.category)
                val questions = if (category == QuizCategory.MATH) {
                    questionRepository.getQuestionsForRound(
                        category = QuizCategory.MATH,
                        count = msg.totalQuestions,
                        seed = msg.mathSeed
                    )
                } else {
                    questionRepository.getQuestionsByIds(msg.questionIds)
                }
                if (questions.isNotEmpty()) {
                    prepareMatchState(questions)
                }
            }

            is Msg.ShowQuestion -> {
                if (role != DuelRole.GUEST) return
                val question = activeQuestions.getOrNull(msg.index) ?: return
                currentQuestionIndex = msg.index
                questionShownAtMs = clockMs()

                _state.value = DuelUiState.Playing(
                    questionIndex = msg.index,
                    totalQuestions = activeQuestions.size,
                    question = question,
                    myScore = guestScore,
                    opponentScore = hostScore,
                    myName = localPlayerName,
                    opponentName = opponentName.orEmpty(),
                    timeRemainingMs = questionDurationMs,
                    totalTimeMs = questionDurationMs,
                    myChoice = null,
                    waitingForOpponent = false,
                    roundResult = null
                )
                startVisualCountdownTimer()
            }

            is Msg.Answer -> {
                if (role != DuelRole.HOST) return
                if (msg.index != currentQuestionIndex || roundResolved) return
                if (guestRoundSubmission != null) return

                guestRoundSubmission = msg.choice to msg.elapsedMs
                if (hostRoundSubmission != null) {
                    resolveHostRound(currentQuestionIndex)
                }
            }

            is Msg.RoundResult -> {
                if (role != DuelRole.GUEST) return
                roundTimerJob?.cancel()

                val myPoints = (msg.guestScore - guestScore).coerceAtLeast(0)
                val oppPoints = (msg.hostScore - hostScore).coerceAtLeast(0)
                guestScore = msg.guestScore
                hostScore = msg.hostScore

                if (msg.guestCorrect) {
                    myCorrectCount++
                } else {
                    activeQuestions.getOrNull(msg.index)?.let { question ->
                        myWrongRecords.add(
                            AnswerRecord(
                                question = question,
                                selectedIndex = myChoicesByRound[msg.index],
                                isCorrect = false,
                                pointsEarned = 0,
                                elapsedMs = questionDurationMs
                            )
                        )
                    }
                }

                val current = _state.value as? DuelUiState.Playing ?: return
                _state.value = current.copy(
                    myScore = guestScore,
                    opponentScore = hostScore,
                    waitingForOpponent = false,
                    roundResult = RoundFeedback(
                        correctIndex = msg.correctIndex,
                        myCorrect = msg.guestCorrect,
                        opponentCorrect = msg.hostCorrect,
                        myPointsEarned = myPoints,
                        opponentPointsEarned = oppPoints
                    )
                )
            }

            is Msg.GameOver -> {
                if (role != DuelRole.GUEST) return
                roundTimerJob?.cancel()
                hostScore = msg.hostScore
                guestScore = msg.guestScore
                finishMatch(myFinalScore = guestScore, opponentFinalScore = hostScore)
            }

            Msg.Rematch -> {
                val current = _state.value as? DuelUiState.Finished ?: return
                val updated = current.copy(rematchRequestedByOpponent = true)
                _state.value = updated
                if (updated.rematchRequestedByMe && role == DuelRole.HOST) {
                    startMatch()
                }
            }

            Msg.Leave -> {
                cancelRoundJobs()
                when (val current = _state.value) {
                    is DuelUiState.Finished -> {
                        _state.value = current.copy(opponentLeft = true)
                    }
                    is DuelUiState.Playing,
                    is DuelUiState.Lobby -> {
                        _state.value = DuelUiState.Disconnected(
                            reason = "opponent_left",
                            wasInMatch = current is DuelUiState.Playing,
                            previousRole = role
                        )
                    }
                    is DuelUiState.Disconnected -> Unit
                }
            }
        }
    }

    private fun prepareMatchState(questions: List<Question>) {
        cancelRoundJobs()
        activeQuestions = questions
        currentQuestionIndex = 0
        hostScore = 0
        guestScore = 0
        myCorrectCount = 0
        myWrongRecords.clear()
        myChoicesByRound.clear()
        hostRoundSubmission = null
        guestRoundSubmission = null
        roundResolved = false
    }

    private suspend fun hostShowQuestion(index: Int) {
        val question = activeQuestions.getOrNull(index) ?: return
        currentQuestionIndex = index
        questionShownAtMs = clockMs()
        hostRoundSubmission = null
        guestRoundSubmission = null
        roundResolved = false

        _state.value = DuelUiState.Playing(
            questionIndex = index,
            totalQuestions = activeQuestions.size,
            question = question,
            myScore = hostScore,
            opponentScore = guestScore,
            myName = localPlayerName,
            opponentName = opponentName.orEmpty(),
            timeRemainingMs = questionDurationMs,
            totalTimeMs = questionDurationMs,
            myChoice = null,
            waitingForOpponent = false,
            roundResult = null
        )

        transport.send(Msg.ShowQuestion(index))
        startHostAuthoritativeTimer(index)
    }

    private fun startHostAuthoritativeTimer(index: Int) {
        roundTimerJob?.cancel()
        roundTimerJob = scope.launch {
            while (!roundResolved && currentQuestionIndex == index) {
                val elapsed = clockMs() - questionShownAtMs
                val remaining = (questionDurationMs - elapsed).coerceAtLeast(0L)
                val current = _state.value
                if (current is DuelUiState.Playing && current.roundResult == null) {
                    _state.value = current.copy(timeRemainingMs = remaining)
                }
                if (remaining <= 0L) {
                    resolveHostRound(index)
                    break
                }
                delay(timerTickMs)
            }
        }
    }

    private fun startVisualCountdownTimer() {
        roundTimerJob?.cancel()
        roundTimerJob = scope.launch {
            while (true) {
                val elapsed = clockMs() - questionShownAtMs
                val remaining = (questionDurationMs - elapsed).coerceAtLeast(0L)
                val current = _state.value
                if (current !is DuelUiState.Playing || current.roundResult != null || current.myChoice != null) {
                    break
                }
                _state.value = current.copy(timeRemainingMs = remaining)
                if (remaining <= 0L) break
                delay(timerTickMs)
            }
        }
    }

    private fun resolveHostRound(index: Int) {
        if (roundResolved || index != currentQuestionIndex) return
        roundResolved = true
        roundTimerJob?.cancel()

        val question = activeQuestions.getOrNull(index) ?: return

        val hostRecord = if (hostRoundSubmission == null) {
            hostGameEngine.submitAnswer(choice = -1, elapsedMs = questionDurationMs)
        } else {
            null
        }

        val hostSubmission = hostRoundSubmission
        val hostCorrect: Boolean
        val hostPoints: Int
        if (hostSubmission != null) {
            hostCorrect = hostSubmission.first == question.correctIndex && hostSubmission.second < questionDurationMs
            hostPoints = ScoreCalculator.calculate(hostCorrect, hostSubmission.second)
        } else {
            hostCorrect = hostRecord?.isCorrect == true
            hostPoints = hostRecord?.pointsEarned ?: 0
        }

        if (hostCorrect) {
            myCorrectCount++
        } else {
            myWrongRecords.add(
                AnswerRecord(
                    question = question,
                    selectedIndex = hostSubmission?.first,
                    isCorrect = false,
                    pointsEarned = 0,
                    elapsedMs = hostSubmission?.second ?: questionDurationMs
                )
            )
        }

        val engineScore = (hostGameEngine.state.value as? GameUiState.ActiveQuestion)?.score
        hostScore = engineScore ?: (hostScore + hostPoints)

        val guestSub = guestRoundSubmission
        val guestChoice = guestSub?.first ?: -1
        val guestElapsed = guestSub?.second ?: questionDurationMs
        val guestCorrect = guestChoice == question.correctIndex && guestElapsed < questionDurationMs
        val guestPoints = ScoreCalculator.calculate(guestCorrect, guestElapsed)
        guestScore += guestPoints

        val current = _state.value as? DuelUiState.Playing
        if (current != null) {
            _state.value = current.copy(
                myScore = hostScore,
                opponentScore = guestScore,
                waitingForOpponent = false,
                roundResult = RoundFeedback(
                    correctIndex = question.correctIndex,
                    myCorrect = hostCorrect,
                    opponentCorrect = guestCorrect,
                    myPointsEarned = hostPoints,
                    opponentPointsEarned = guestPoints
                )
            )
        }

        roundAdvanceJob?.cancel()
        roundAdvanceJob = scope.launch {
            transport.send(
                Msg.RoundResult(
                    index = index,
                    correctIndex = question.correctIndex,
                    hostScore = hostScore,
                    guestScore = guestScore,
                    hostCorrect = hostCorrect,
                    guestCorrect = guestCorrect
                )
            )

            delay(roundResultDelayMs)

            hostGameEngine.nextQuestion()
            if (index + 1 < activeQuestions.size) {
                hostShowQuestion(index + 1)
            } else {
                transport.send(
                    Msg.GameOver(
                        hostScore = hostScore,
                        guestScore = guestScore
                    )
                )
                finishMatch(myFinalScore = hostScore, opponentFinalScore = guestScore)
            }
        }
    }

    private fun finishMatch(myFinalScore: Int, opponentFinalScore: Int) {
        val wrongCopy = myWrongRecords.toList()
        _state.value = DuelUiState.Finished(
            myScore = myFinalScore,
            opponentScore = opponentFinalScore,
            myName = localPlayerName,
            opponentName = opponentName.orEmpty(),
            wrongAnswers = wrongCopy,
            totalQuestions = activeQuestions.size,
            myCorrectCount = myCorrectCount
        )
        onMatchFinished(wrongCopy)
    }

    private fun emitLobby(connState: ConnState) {
        _state.value = DuelUiState.Lobby(
            connState = connState,
            role = role,
            localPlayerName = localPlayerName,
            opponentName = opponentName,
            selectedCategory = selectedCategory,
            selectedQuestionCount = selectedQuestionCount
        )
    }

    private fun cancelRoundJobs() {
        roundTimerJob?.cancel()
        roundAdvanceJob?.cancel()
    }
}
