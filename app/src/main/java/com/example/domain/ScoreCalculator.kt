package com.example.domain

object ScoreCalculator {
    const val MAX_POINTS = 1000
    const val PENALTY_PER_SECOND = 80
    const val MIN_POINTS = 400
    const val QUESTION_DURATION_MS = 15_000L

    fun calculate(isCorrect: Boolean, elapsedMs: Long): Int {
        if (!isCorrect) return 0
        if (elapsedMs >= QUESTION_DURATION_MS) return 0
        val clampedElapsedMs = elapsedMs.coerceAtLeast(0L)
        val elapsedSeconds = (clampedElapsedMs / 1000L).toInt()
        val rawScore = MAX_POINTS - (elapsedSeconds * PENALTY_PER_SECOND)
        return rawScore.coerceAtLeast(MIN_POINTS)
    }
}
