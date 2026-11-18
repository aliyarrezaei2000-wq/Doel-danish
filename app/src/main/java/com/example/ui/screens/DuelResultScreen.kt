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
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Handshake
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AnswerRecord
import com.example.model.DuelUiState
import com.example.ui.components.DuelDisconnectDialog
import com.example.ui.theme.LocalSemanticQuizColors
import com.example.util.toPersianDigits

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuelResultScreen(
    duelState: DuelUiState,
    onRequestRematch: () -> Unit,
    onReconnect: () -> Unit,
    onNavigateToDuelGame: () -> Unit,
    onLeaveDuel: () -> Unit
) {
    BackHandler {
        onLeaveDuel()
    }

    LaunchedEffect(duelState) {
        if (duelState is DuelUiState.Playing) {
            onNavigateToDuelGame()
        }
    }

    if (duelState is DuelUiState.Disconnected) {
        DuelDisconnectDialog(
            onReconnect = onReconnect,
            onReturnHome = onLeaveDuel
        )
    }

    val finished = duelState as? DuelUiState.Finished

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.duel_result_title),
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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(onClick = onLeaveDuel) {
                        Text(text = stringResource(R.string.duel_result_leave))
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
                            DuelOutcomeHeaderCard(finished = finished)
                        }

                        if (finished.opponentLeft) {
                            item {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = LocalSemanticQuizColors.current.wrongContainer
                                ) {
                                    Text(
                                        text = stringResource(R.string.duel_result_opponent_left),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = LocalSemanticQuizColors.current.onWrongContainer,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else if (finished.rematchRequestedByMe && !finished.rematchRequestedByOpponent) {
                            item {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = stringResource(R.string.duel_result_rematch_waiting),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else if (finished.rematchRequestedByOpponent && !finished.rematchRequestedByMe) {
                            item {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = LocalSemanticQuizColors.current.correctContainer
                                ) {
                                    Text(
                                        text = stringResource(R.string.duel_result_opponent_wants_rematch),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = LocalSemanticQuizColors.current.onCorrectContainer,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        if (finished.wrongAnswers.isEmpty()) {
                            item {
                                DuelAllCorrectCard()
                            }
                        } else {
                            item {
                                Text(
                                    text = stringResource(R.string.result_wrong_list_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            items(
                                items = finished.wrongAnswers,
                                key = { it.question.id }
                            ) { record ->
                                DuelWrongQuestionCard(record = record)
                            }
                        }
                    }

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
                                onClick = onRequestRematch,
                                enabled = !finished.rematchRequestedByMe && !finished.opponentLeft,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("duel_rematch_button"),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.duel_result_rematch),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            OutlinedButton(
                                onClick = onLeaveDuel,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("duel_leave_button"),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(imageVector = Icons.Rounded.ExitToApp, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.duel_result_leave),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DuelOutcomeHeaderCard(finished: DuelUiState.Finished) {
    val semanticColors = LocalSemanticQuizColors.current
    val outcome = when {
        finished.myScore > finished.opponentScore -> 1
        finished.myScore < finished.opponentScore -> -1
        else -> 0
    }

    val containerColor = when (outcome) {
        1 -> semanticColors.correctContainer
        -1 -> semanticColors.wrongContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when (outcome) {
        1 -> semanticColors.onCorrectContainer
        -1 -> semanticColors.onWrongContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    val titleText = when (outcome) {
        1 -> stringResource(R.string.duel_result_win)
        -1 -> stringResource(R.string.duel_result_lose)
        else -> stringResource(R.string.duel_result_draw)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("duel_outcome_card"),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (outcome == 0) Icons.Rounded.Handshake else Icons.Rounded.EmojiEvents,
                contentDescription = null,
                tint = if (outcome == 1) semanticColors.correct else contentColor,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = titleText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = finished.myName.ifBlank { stringResource(R.string.duel_me_label) },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = finished.myScore.toPersianDigits(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = finished.opponentName.ifBlank { stringResource(R.string.duel_opponent_label) },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = finished.opponentScore.toPersianDigits(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Text(
                text = stringResource(
                    R.string.result_correct_answers_format,
                    finished.myCorrectCount.toPersianDigits(),
                    finished.totalQuestions.toPersianDigits()
                ),
                style = MaterialTheme.typography.labelLarge,
                color = contentColor
            )
        }
    }
}

@Composable
private fun DuelAllCorrectCard() {
    val semanticColors = LocalSemanticQuizColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = semanticColors.correctContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = semanticColors.correct,
                modifier = Modifier.size(34.dp)
            )
            Column {
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
private fun DuelWrongQuestionCard(record: AnswerRecord) {
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
