package com.example

import com.example.data.QuestionRepository
import com.example.domain.DuelEngine
import com.example.domain.MathQuestionGenerator
import com.example.model.DuelUiState
import com.example.model.Question
import com.example.model.QuizCategory
import com.example.network.ConnState
import com.example.network.Msg
import com.example.network.Transport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class DuelEngineTest {

    private class FakeTransportPair {
        private val hostIncoming = MutableSharedFlow<Msg>(extraBufferCapacity = 64)
        private val guestIncoming = MutableSharedFlow<Msg>(extraBufferCapacity = 64)

        private val hostState = MutableStateFlow<ConnState>(ConnState.Idle)
        private val guestState = MutableStateFlow<ConnState>(ConnState.Idle)

        private var hostingStarted = false
        private var joiningStarted = false

        val host: Transport = object : Transport {
            override val incoming: Flow<Msg> = hostIncoming.asSharedFlow()
            override val state: StateFlow<ConnState> = hostState.asStateFlow()

            override suspend fun startHosting(playerName: String) {
                hostingStarted = true
                hostState.value = ConnState.Advertising
                tryConnect()
            }

            override suspend fun startJoining(playerName: String) = Unit

            override suspend fun send(msg: Msg) {
                guestIncoming.emit(msg)
            }

            override fun close() {
                hostingStarted = false
                hostState.value = ConnState.Idle
            }
        }

        val guest: Transport = object : Transport {
            override val incoming: Flow<Msg> = guestIncoming.asSharedFlow()
            override val state: StateFlow<ConnState> = guestState.asStateFlow()

            override suspend fun startHosting(playerName: String) = Unit

            override suspend fun startJoining(playerName: String) {
                joiningStarted = true
                guestState.value = ConnState.Discovering
                tryConnect()
            }

            override suspend fun send(msg: Msg) {
                hostIncoming.emit(msg)
            }

            override fun close() {
                joiningStarted = false
                guestState.value = ConnState.Idle
            }
        }

        private fun tryConnect() {
            if (hostingStarted && joiningStarted) {
                hostState.value = ConnState.Connected
                guestState.value = ConnState.Connected
            }
        }

        fun simulateConnectionDrop(reason: String = "link_lost") {
            hostState.value = ConnState.Lost(reason)
            guestState.value = ConnState.Lost(reason)
        }
    }

    private class FakeQuestionRepository : QuestionRepository {
        private val mathGenerator = MathQuestionGenerator()
        private val bank = listOf(
            Question("q1", "vocab", 1, "Q1", listOf("A", "B", "C", "D"), 0, "Exp 1"),
            Question("q2", "vocab", 1, "Q2", listOf("A", "B", "C", "D"), 1, "Exp 2"),
            Question("q3", "vocab", 2, "Q3", listOf("A", "B", "C", "D"), 2, "Exp 3"),
            Question("q4", "vocab", 2, "Q4", listOf("A", "B", "C", "D"), 3, "Exp 4"),
            Question("q5", "vocab", 3, "Q5", listOf("A", "B", "C", "D"), 1, "Exp 5")
        )

        override suspend fun getQuestionsForRound(
            category: QuizCategory,
            count: Int,
            seed: Long
        ): List<Question> {
            return if (category == QuizCategory.MATH) {
                List(count) { idx ->
                    mathGenerator.generate(level = (idx % 3) + 1, seed = seed * 31L + idx + 1L)
                }
            } else {
                bank.shuffled(Random(seed)).take(count)
            }
        }

        override suspend fun getQuestionsByIds(ids: List<String>): List<Question> {
            val map = bank.associateBy { it.id }
            return ids.mapNotNull { map[it] }
        }

        override suspend fun getAllBankQuestions(): List<Question> = bank
    }

    private fun TestScope.createPair(
        repo: QuestionRepository = FakeQuestionRepository()
    ): Triple<FakeTransportPair, DuelEngine, DuelEngine> {
        val pair = FakeTransportPair()
        val hostEngine = DuelEngine(
            transport = pair.host,
            questionRepository = repo,
            scope = backgroundScope,
            clockMs = { testScheduler.currentTime },
            roundResultDelayMs = 2_000L,
            timerTickMs = 100L,
            seedGenerator = { 12345L }
        )
        val guestEngine = DuelEngine(
            transport = pair.guest,
            questionRepository = repo,
            scope = backgroundScope,
            clockMs = { testScheduler.currentTime },
            roundResultDelayMs = 2_000L,
            timerTickMs = 100L,
            seedGenerator = { 12345L }
        )
        return Triple(pair, hostEngine, guestEngine)
    }

    @Test
    fun roundCompletionAndScoring_bothEnginesStaySynchronized() = runTest {
        val (_, host, guest) = createPair()

        backgroundScope.launch { host.startHosting("Ali") }
        backgroundScope.launch { guest.startJoining("Sara") }
        runCurrent()

        val hostLobby = host.state.value as DuelUiState.Lobby
        assertEquals(ConnState.Connected, hostLobby.connState)
        assertEquals("Sara", hostLobby.opponentName)

        host.updateCategory(QuizCategory.VOCABULARY)
        host.updateQuestionCount(5)
        // Use 2-question match via custom fake or 5 questions; let's verify 5-question round progression
        host.startMatch()
        runCurrent()

        val hPlay0 = host.state.value as DuelUiState.Playing
        val gPlay0 = guest.state.value as DuelUiState.Playing
        assertEquals(hPlay0.question, gPlay0.question)
        assertEquals(0, hPlay0.questionIndex)

        // Host answers correctly at 1s -> 920 pts
        advanceTimeBy(1_000L)
        runCurrent()
        host.submitAnswer(hPlay0.question.correctIndex)
        runCurrent()

        val hWaiting = host.state.value as DuelUiState.Playing
        assertTrue(hWaiting.waitingForOpponent)

        // Guest answers correctly at 3s -> 760 pts
        advanceTimeBy(2_000L)
        runCurrent()
        guest.submitAnswer(gPlay0.question.correctIndex)
        runCurrent()

        val hResult0 = host.state.value as DuelUiState.Playing
        val gResult0 = guest.state.value as DuelUiState.Playing
        assertNotNull(hResult0.roundResult)
        assertNotNull(gResult0.roundResult)
        assertEquals(920, hResult0.myScore)
        assertEquals(760, hResult0.opponentScore)
        assertEquals(760, gResult0.myScore)
        assertEquals(920, gResult0.opponentScore)
        assertFalse(hResult0.waitingForOpponent)

        // After 2s delay, advances to question index 1
        advanceTimeBy(2_000L)
        runCurrent()
        val hPlay1 = host.state.value as DuelUiState.Playing
        val gPlay1 = guest.state.value as DuelUiState.Playing
        assertEquals(1, hPlay1.questionIndex)
        assertEquals(1, gPlay1.questionIndex)
    }

    @Test
    fun guestTimeout_awardsZeroPointsAndAdvancesRound() = runTest {
        val (_, host, guest) = createPair()
        backgroundScope.launch { host.startHosting("Host") }
        backgroundScope.launch { guest.startJoining("Guest") }
        runCurrent()

        host.updateCategory(QuizCategory.MATH)
        host.updateQuestionCount(5)
        host.startMatch()
        runCurrent()

        val hPlay = host.state.value as DuelUiState.Playing
        val gPlay = guest.state.value as DuelUiState.Playing
        // Deterministic math question must match on both devices
        assertEquals(hPlay.question, gPlay.question)

        // Host answers correctly at 0ms -> 1000 pts; guest never answers
        host.submitAnswer(hPlay.question.correctIndex)
        runCurrent()

        // Advance past the 15s authoritative timer
        advanceTimeBy(15_000L)
        runCurrent()

        val hResolved = host.state.value as DuelUiState.Playing
        val gResolved = guest.state.value as DuelUiState.Playing
        val feedback = hResolved.roundResult
        assertNotNull(feedback)
        assertTrue(feedback!!.myCorrect)
        assertFalse(feedback.opponentCorrect)
        assertEquals(1000, hResolved.myScore)
        assertEquals(0, hResolved.opponentScore)
        assertEquals(0, gResolved.myScore)
        assertEquals(1000, gResolved.opponentScore)
    }

    @Test
    fun rematchAfterGameOver_startsNewMatchWhenBothConfirm() = runTest {
        val (_, host, guest) = createPair()
        backgroundScope.launch { host.startHosting("Host") }
        backgroundScope.launch { guest.startJoining("Guest") }
        runCurrent()

        host.updateCategory(QuizCategory.VOCABULARY)
        host.updateQuestionCount(5)
        host.startMatch()
        runCurrent()

        // Complete all 5 rounds
        repeat(5) {
            val hQ = host.state.value as DuelUiState.Playing
            val gQ = guest.state.value as DuelUiState.Playing
            host.submitAnswer(hQ.question.correctIndex)
            guest.submitAnswer((gQ.question.correctIndex + 1) % 4)
            runCurrent()
            advanceTimeBy(2_000L)
            runCurrent()
        }

        val hFinished = host.state.value as DuelUiState.Finished
        val gFinished = guest.state.value as DuelUiState.Finished
        assertEquals(5000, hFinished.myScore)
        assertEquals(0, hFinished.opponentScore)
        assertEquals(0, gFinished.myScore)
        assertEquals(5, gFinished.wrongAnswers.size)

        // Guest requests rematch first, then Host confirms
        guest.requestRematch()
        runCurrent()
        assertTrue((host.state.value as DuelUiState.Finished).rematchRequestedByOpponent)

        host.requestRematch()
        runCurrent()

        // Both engines should be back in Playing at question 0 with reset scores
        val hRematch = host.state.value as DuelUiState.Playing
        val gRematch = guest.state.value as DuelUiState.Playing
        assertEquals(0, hRematch.questionIndex)
        assertEquals(0, gRematch.questionIndex)
        assertEquals(0, hRematch.myScore)
        assertEquals(0, gRematch.myScore)
    }

    @Test
    fun connectionDropMidGame_transitionsToDisconnected() = runTest {
        val (pair, host, guest) = createPair()
        backgroundScope.launch { host.startHosting("Host") }
        backgroundScope.launch { guest.startJoining("Guest") }
        runCurrent()

        host.updateQuestionCount(5)
        host.startMatch()
        runCurrent()

        pair.simulateConnectionDrop("bluetooth_off")
        runCurrent()

        val hDisc = host.state.value as DuelUiState.Disconnected
        val gDisc = guest.state.value as DuelUiState.Disconnected
        assertTrue(hDisc.wasInMatch)
        assertTrue(gDisc.wasInMatch)
    }
}
