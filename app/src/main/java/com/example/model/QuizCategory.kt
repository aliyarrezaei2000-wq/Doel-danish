package com.example.model

enum class QuizCategory(
    val id: String,
    val assetFileName: String?
) {
    VOCABULARY(id = "vocab", assetFileName = "questions/vocab.json"),
    MATH(id = "math", assetFileName = null),
    GEOGRAPHY(id = "geo", assetFileName = "questions/geo.json"),
    POETRY(id = "poetry", assetFileName = "questions/poetry.json");

    companion object {
        fun fromId(id: String): QuizCategory =
            entries.firstOrNull { it.id == id } ?: VOCABULARY
    }
}
