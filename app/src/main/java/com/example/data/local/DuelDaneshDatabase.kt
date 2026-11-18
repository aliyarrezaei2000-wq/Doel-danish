package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MistakeEntity::class],
    version = 1,
    exportSchema = false
)
abstract class DuelDaneshDatabase : RoomDatabase() {
    abstract fun mistakeDao(): MistakeDao

    companion object {
        @Volatile
        private var instance: DuelDaneshDatabase? = null

        fun getInstance(context: Context): DuelDaneshDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DuelDaneshDatabase::class.java,
                    "duel_danesh.db"
                ).build().also { instance = it }
            }
        }
    }
}
