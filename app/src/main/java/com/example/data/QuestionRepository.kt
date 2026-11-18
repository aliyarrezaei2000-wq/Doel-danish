package com.example.data

import com.example.model.Question
import com.example.model.QuizCategory

interface QuestionRepository {
    suspend fun getQuestionsForRound(
        category: QuizCategory,
        count: Int = DEFAULT_ROUND_SIZE,
        seed: Long = System.currentTimeMillis()
    ): List<Question>

    suspend fun getQuestionsByIds(ids: List<String>): List<Question>

    suspend fun getAllBankQuestions(): List<Question>

    companion object {
        const val DEFAULT_ROUND_SIZE = 10
    }
}
