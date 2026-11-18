package com.example.data

import com.example.data.local.MistakeDao
import com.example.data.local.MistakeEntity
import com.example.model.Question
import com.example.model.QuizCategory
import kotlinx.coroutines.flow.Flow

interface MistakeRepository {
    fun observeMistakes(): Flow<List<MistakeEntity>>
    suspend fun recordMistake(question: Question, timestamp: Long = System.currentTimeMillis())
    suspend fun removeMistake(questionId: String)
    suspend fun clearAllMistakes()
}

class RoomMistakeRepository(
    private val mistakeDao: MistakeDao
) : MistakeRepository {

    override fun observeMistakes(): Flow<List<MistakeEntity>> = mistakeDao.getAllMistakes()

    override suspend fun recordMistake(question: Question, timestamp: Long) {
        // Math questions are generated dynamically and are not saved as mistakes.
        if (question.category == QuizCategory.MATH.id) return
        mistakeDao.recordWrongAnswer(questionId = question.id, timestamp = timestamp)
    }

    override suspend fun removeMistake(questionId: String) {
        mistakeDao.deleteMistake(questionId)
    }

    override suspend fun clearAllMistakes() {
        mistakeDao.deleteAllMistakes()
    }
}
