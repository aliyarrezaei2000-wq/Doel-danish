package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mistakes")
data class MistakeEntity(
    @PrimaryKey val questionId: String,
    val wrongCount: Int,
    val lastSeen: Long
)
