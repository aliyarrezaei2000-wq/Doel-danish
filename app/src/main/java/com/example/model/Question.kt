package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class Question(
    val id: String,
    val category: String,
    val level: Int,
    val text: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String? = null
)
