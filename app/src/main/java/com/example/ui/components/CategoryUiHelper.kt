package com.example.ui.components

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R
import com.example.model.QuizCategory
import com.example.ui.theme.CategoryGeoAccent
import com.example.ui.theme.CategoryMathAccent
import com.example.ui.theme.CategoryPoetryAccent
import com.example.ui.theme.CategoryVocabAccent

data class CategoryPresentation(
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
    val icon: ImageVector,
    val accentColor: Color
)

fun QuizCategory.presentation(): CategoryPresentation = when (this) {
    QuizCategory.VOCABULARY -> CategoryPresentation(
        titleRes = R.string.category_vocab,
        descriptionRes = R.string.category_vocab_desc,
        icon = Icons.Rounded.Translate,
        accentColor = CategoryVocabAccent
    )
    QuizCategory.MATH -> CategoryPresentation(
        titleRes = R.string.category_math,
        descriptionRes = R.string.category_math_desc,
        icon = Icons.Rounded.Calculate,
        accentColor = CategoryMathAccent
    )
    QuizCategory.GEOGRAPHY -> CategoryPresentation(
        titleRes = R.string.category_geo,
        descriptionRes = R.string.category_geo_desc,
        icon = Icons.Rounded.Public,
        accentColor = CategoryGeoAccent
    )
    QuizCategory.POETRY -> CategoryPresentation(
        titleRes = R.string.category_poetry,
        descriptionRes = R.string.category_poetry_desc,
        icon = Icons.Rounded.MenuBook,
        accentColor = CategoryPoetryAccent
    )
}
