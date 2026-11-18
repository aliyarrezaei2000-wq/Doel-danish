package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MistakeDao {
    @Query("SELECT * FROM mistakes ORDER BY lastSeen DESC")
    fun getAllMistakes(): Flow<List<MistakeEntity>>

    @Query("SELECT * FROM mistakes WHERE questionId = :questionId LIMIT 1")
    suspend fun getMistakeById(questionId: String): MistakeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMistake(mistake: MistakeEntity)

    @Transaction
    suspend fun recordWrongAnswer(questionId: String, timestamp: Long) {
        val existing = getMistakeById(questionId)
        val updated = if (existing == null) {
            MistakeEntity(
                questionId = questionId,
                wrongCount = 1,
                lastSeen = timestamp
            )
        } else {
            existing.copy(
                wrongCount = existing.wrongCount + 1,
                lastSeen = timestamp
            )
        }
        upsertMistake(updated)
    }

    @Query("DELETE FROM mistakes WHERE questionId = :questionId")
    suspend fun deleteMistake(questionId: String)

    @Query("DELETE FROM mistakes")
    suspend fun deleteAllMistakes()
}
