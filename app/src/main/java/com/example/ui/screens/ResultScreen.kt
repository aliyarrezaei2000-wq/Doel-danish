package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AnswerRecord
import com.example.model.GameMode
import com.example.model.GameUiState
import com.example.ui.theme.LocalSemanticQuizColors
import com.example.util.toPersianDigits

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    gameState: GameUiState,
    activeMode: GameMode,
    onPlayAgain: () -> Unit,
    onGoHome: () -> Unit
) {
    BackHandler {
        onGoHome()
    }

    val finished = gameState as? GameUiState.Finished

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (activeMode == GameMode.MISTAKES_REVIEW) {
                                R.string.result_title_review
                            } else {
                                R.string.result_title
                            }
                        ),
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            if (finished == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(onClick = onGoHome) {
                        Text(text = stringResource(R.string.result_home))
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            ResultSummaryCard(finished = finished)
                        }

                        if (finished.wrongAnswers.isEmpty()) {
                            item {
                                AllCorrectCard()
                            }
                        } else {
                            item {
                                Text(
                                    text = stringResource(R.string.result_wrong_list_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                            items(
                                items = finished.wrongAnswers,
                                key = { it.question.id }
                            ) { record ->
                                WrongAnswerReviewCard(record = record)
                            }
                        }
                    }

                    ResultBottomActions(
                        onPlayAgain = onPlayAgain,
                        onGoHome = onGoHome
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultSummaryCard(finished: GameUiState.Finished) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.EmojiEvents,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = stringResource(R.string.result_final_score_label),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Text(
                text = finished.totalScore.toPersianDigits(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.testTag("result_final_score")
            )
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
            ) {
                Text(
                    text = stringResource(
                        R.string.result_correct_answers_format,
                        finished.correctCount.toPersianDigits(),
                        finished.totalQuestions.toPersianDigits()
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .testTag("result_correct_count")
                )
            }
        }
    }
}

@Composable
private fun AllCorrectCard() {
    val semanticColors = LocalSemanticQuizColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = semanticColors.correctContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = semanticColors.correct,
                modifier = Modifier.size(36.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.result_all_correct_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = semanticColors.onCorrectContainer
                )
                Text(
                    text = stringResource(R.string.result_all_correct_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = semanticColors.onCorrectContainer.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun WrongAnswerReviewCard(record: AnswerRecord) {
    val semanticColors = LocalSemanticQuizColors.current
    val question = record.question
    val correctOption = question.options.getOrElse(question.correctIndex) { "" }.toPersianDigits()
    val userChoiceText = record.selectedIndex
        ?.let { question.options.getOrNull(it)?.toPersianDigits() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = question.text.toPersianDigits(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                shape = MaterialTheme.shapes.small,
                color = semanticColors.wrongContainer
            ) {
                Text(
                    text = if (userChoiceText != null) {
                        stringResource(R.string.result_your_answer_label, userChoiceText)
                    } else {
                        stringResource(R.string.result_timeout_answer)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = semanticColors.onWrongContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Surface(
                shape = MaterialTheme.shapes.small,
                color = semanticColors.correctContainer
            ) {
                Text(
                    text = stringResource(R.string.result_correct_answer_label, correctOption),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = semanticColors.onCorrectContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            question.explanation?.takeIf { it.isNotBlank() }?.let { explanation ->
                Text(
                    text = stringResource(
                        R.string.result_explanation_label,
                        explanation.toPersianDigits()
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ResultBottomActions(
    onPlayAgain: () -> Unit,
    onGoHome: () -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onPlayAgain,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("result_play_again_button"),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.result_play_again),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            OutlinedButton(
                onClick = onGoHome,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("result_home_button"),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(imageVector = Icons.Rounded.Home, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.result_home),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
