package com.example

import com.example.domain.ScoreCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreCalculatorTest {

    @Test
    fun instantCorrectAnswer_awardsMaxPoints() {
        assertEquals(1000, ScoreCalculator.calculate(isCorrect = true, elapsedMs = 0L))
        assertEquals(1000, ScoreCalculator.calculate(isCorrect = true, elapsedMs = 999L))
    }

    @Test
    fun elapsedSeconds_deducts80PerSecond() {
        assertEquals(920, ScoreCalculator.calculate(isCorrect = true, elapsedMs = 1_000L))
        assertEquals(840, ScoreCalculator.calculate(isCorrect = true, elapsedMs = 2_500L))
        assertEquals(600, ScoreCalculator.calculate(isCorrect = true, elapsedMs = 5_000L))
        assertEquals(440, ScoreCalculator.calculate(isCorrect = true, elapsedMs = 7_999L))
    }

    @Test
    fun slowCorrectAnswer_clampsToMinimum400() {
        assertEquals(400, ScoreCalculator.calculate(isCorrect = true, elapsedMs = 8_000L))
        assertEquals(400, ScoreCalculator.calculate(isCorrect = true, elapsedMs = 12_000L))
        assertEquals(400, ScoreCalculator.calculate(isCorrect = true, elapsedMs = 14_999L))
    }

    @Test
    fun timeoutOrWrongAnswer_awardsZeroPoints() {
        assertEquals(0, ScoreCalculator.calculate(isCorrect = false, elapsedMs = 500L))
        assertEquals(0, ScoreCalculator.calculate(isCorrect = false, elapsedMs = 10_000L))
        assertEquals(0, ScoreCalculator.calculate(isCorrect = true, elapsedMs = 15_000L))
        assertEquals(0, ScoreCalculator.calculate(isCorrect = true, elapsedMs = 20_000L))
    }

    @Test
    fun negativeElapsedTime_clampsToZeroSeconds() {
        assertEquals(1000, ScoreCalculator.calculate(isCorrect = true, elapsedMs = -250L))
    }
}
