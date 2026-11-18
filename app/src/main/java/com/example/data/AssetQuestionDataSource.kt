package com.example.data

import android.content.Context
import com.example.model.Question
import com.example.model.QuizCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class AssetQuestionDataSource(
    private val context: Context,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    private val mutex = Mutex()
    private val cache = mutableMapOf<String, List<Question>>()

    suspend fun loadCategory(category: QuizCategory): List<Question> {
        val assetPath = category.assetFileName ?: return emptyList()
        return mutex.withLock {
            cache[category.id] ?: withContext(Dispatchers.IO) {
                val rawJson = context.assets.open(assetPath).bufferedReader().use { it.readText() }
                json.decodeFromString<List<Question>>(rawJson)
            }.also { loaded ->
                cache[category.id] = loaded
            }
        }
    }

    suspend fun loadAllAssetQuestions(): List<Question> {
        val categoriesWithAssets = QuizCategory.entries.filter { it.assetFileName != null }
        val all = mutableListOf<Question>()
        for (category in categoriesWithAssets) {
            all.addAll(loadCategory(category))
        }
        return all
    }
}
