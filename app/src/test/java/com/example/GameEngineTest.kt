package com.example

import com.example.domain.GameEngine
import com.example.model.GameUiState
import com.example.model.Question
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {

    private val sampleQuestions = listOf(
        Question(
            id = "q1",
            category = "vocab",
            level = 1,
            text = "Q1",
            options = listOf("A", "B", "C", "D"),
            correctIndex = 1,
            explanation = "Exp 1"
        ),
        Question(
            id = "q2",
            category = "vocab",
            level = 2,
            text = "Q2",
            options = listOf("A", "B", "C", "D"),
            correctIndex = 2,
            explanation = "Exp 2"
        )
    )

    @Test
    fun fullRound_tracksScoreAndWrongAnswers() {
        val engine = GameEngine()
        assertTrue(engine.state.value is GameUiState.Idle)

        engine.start(sampleQuestions)
        val firstState = engine.state.value as GameUiState.ActiveQuestion
        assertEquals(0, firstState.questionIndex)
        assertEquals(0, firstState.score)

        // Correct answer after 2 seconds -> 840 points
        val rec1 = engine.submitAnswer(choice = 1, elapsedMs = 2_000L)!!
        assertTrue(rec1.isCorrect)
        assertEquals(840, rec1.pointsEarned)

        val afterFirst = engine.state.value as GameUiState.ActiveQuestion
        assertTrue(afterFirst.isAnswerSubmitted)
        assertEquals(840, afterFirst.score)

        engine.nextQuestion()
        val secondState = engine.state.value as GameUiState.ActiveQuestion
        assertEquals(1, secondState.questionIndex)
        assertFalse(secondState.isAnswerSubmitted)

        // Wrong answer -> 0 points
        val rec2 = engine.submitAnswer(choice = 0, elapsedMs = 1_500L)!!
        assertFalse(rec2.isCorrect)
        assertEquals(0, rec2.pointsEarned)

        engine.nextQuestion()
        val finished = engine.state.value as GameUiState.Finished
        assertEquals(840, finished.totalScore)
        assertEquals(1, finished.correctCount)
        assertEquals(2, finished.totalQuestions)
        assertEquals(1, finished.wrongAnswers.size)
        assertEquals("q2", finished.wrongAnswers.first().question.id)
    }
}
