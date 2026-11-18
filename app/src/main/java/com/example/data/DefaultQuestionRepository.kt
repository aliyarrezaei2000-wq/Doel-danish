package com.example.data

import com.example.domain.MathQuestionGenerator
import com.example.model.Question
import com.example.model.QuizCategory
import kotlin.random.Random

class DefaultQuestionRepository(
    private val assetDataSource: AssetQuestionDataSource,
    private val mathGenerator: MathQuestionGenerator = MathQuestionGenerator()
) : QuestionRepository {

    override suspend fun getQuestionsForRound(
        category: QuizCategory,
        count: Int,
        seed: Long
    ): List<Question> {
        if (count <= 0) return emptyList()
        return when (category) {
            QuizCategory.MATH -> {
                List(count) { index ->
                    val level = when {
                        index < count / 3 -> 1
                        index < (count * 2) / 3 -> 2
                        else -> 3
                    }
                    val questionSeed = seed * 31L + index + 1L
                    mathGenerator.generate(level = level, seed = questionSeed)
                }
            }
            else -> {
                val bank = assetDataSource.loadCategory(category)
                bank.shuffled(Random(seed)).take(count)
            }
        }
    }

    override suspend fun getQuestionsByIds(ids: List<String>): List<Question> {
        if (ids.isEmpty()) return emptyList()
        val byId = getAllBankQuestions().associateBy { it.id }
        return ids.mapNotNull { byId[it] }
    }

    override suspend fun getAllBankQuestions(): List<Question> {
        return assetDataSource.loadAllAssetQuestions()
    }
}
