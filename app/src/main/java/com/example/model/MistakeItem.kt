package com.example.model

import com.example.data.local.MistakeEntity

data class MistakeItem(
    val entity: MistakeEntity,
    val question: Question
)
