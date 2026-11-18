package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.GameUiState
import com.example.model.QuizCategory
import com.example.ui.components.presentation
import com.example.ui.theme.LocalSemanticQuizColors
import com.example.util.toPersianDigits
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    gameState: GameUiState,
    onSubmitAnswer: (Int) -> Unit,
    onNavigateToResult: () -> Unit,
    onNavigateBackHome: () -> Unit
) {
    BackHandler {
        onNavigateBackHome()
    }

    LaunchedEffect(gameState) {
        if (gameState is GameUiState.Finished) {
            onNavigateToResult()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    val titleText = when (gameState) {
                        is GameUiState.ActiveQuestion -> {
                            val cat = QuizCategory.fromId(gameState.question.category)
                            stringResource(cat.presentation().titleRes)
                        }
                        else -> stringResource(R.string.app_name)
                    }
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBackHome,
                        modifier = Modifier.testTag("game_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.game_back_cd)
                        )
                    }
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
            when (gameState) {
                is GameUiState.Loading, GameUiState.Idle -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.game_loading),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                is GameUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.game_error_empty),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateBackHome) {
                            Text(text = stringResource(R.string.game_error_retry))
                        }
                    }
                }

                is GameUiState.ActiveQuestion -> {
                    ActiveQuestionContent(
                        state = gameState,
                        onSubmitAnswer = onSubmitAnswer
                    )
                }

                is GameUiState.Finished -> {
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun ActiveQuestionContent(
    state: GameUiState.ActiveQuestion,
    onSubmitAnswer: (Int) -> Unit
) {
    val semanticColors = LocalSemanticQuizColors.current
    val timerFraction = (state.timeRemainingMs.toFloat() / state.totalTimeMs.toFloat())
        .coerceIn(0f, 1f)
    val remainingSeconds = ceil(state.timeRemainingMs / 1000.0).toInt().coerceAtLeast(0)
    val timerColor by animateColorAsState(
        targetValue = when {
            timerFraction > 0.5f -> MaterialTheme.colorScheme.primary
            timerFraction > 0.25f -> MaterialTheme.colorScheme.secondary
            else -> semanticColors.wrong
        },
        label = "timer_bar_color"
    )

    Column(
        modifier = Modifier
            .widthIn(max = 640.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Score & Question Counter Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = stringResource(
                        R.string.game_question_counter,
                        (state.questionIndex + 1).toPersianDigits(),
                        state.totalQuestions.toPersianDigits()
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("question_counter_text")
                )
            }

            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Stars,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(
                            R.string.game_score_label,
                            state.score.toPersianDigits()
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.testTag("live_score_text")
                    )
                }
            }
        }

        // Countdown Timer Bar
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = timerColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(
                            R.string.game_timer_seconds,
                            remainingSeconds.toPersianDigits()
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = timerColor,
                        modifier = Modifier.testTag("timer_seconds_text")
                    )
                }

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = stringResource(
                            R.string.game_level_badge,
                            state.question.level.toPersianDigits()
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { timerFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .testTag("countdown_timer_bar"),
                color = timerColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
        }

        // Question Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Text(
                text = state.question.text.toPersianDigits(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .testTag("question_text")
            )
        }

        // 4 Answer Options
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.question.options.forEachIndexed { index, optionText ->
                AnswerOptionCard(
                    index = index,
                    text = optionText.toPersianDigits(),
                    state = state,
                    onClick = { onSubmitAnswer(index) }
                )
            }
        }

        // Feedback Banner after answer
        AnimatedVisibility(
            visible = state.isAnswerSubmitted,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val isCorrect = state.isCorrect == true
            val isTimeout = state.selectedIndex == null
            val bannerColor = if (isCorrect) {
                semanticColors.correctContainer
            } else {
                semanticColors.wrongContainer
            }
            val bannerTextColor = if (isCorrect) {
                semanticColors.onCorrectContainer
            } else {
                semanticColors.onWrongContainer
            }
            val feedbackText = when {
                isCorrect -> stringResource(
                    R.string.game_feedback_correct,
                    state.lastPointsEarned.toPersianDigits()
                )
                isTimeout -> stringResource(R.string.game_feedback_timeout)
                else -> stringResource(R.string.game_feedback_wrong)
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("answer_feedback_banner"),
                shape = MaterialTheme.shapes.medium,
                color = bannerColor
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (isCorrect) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                        contentDescription = null,
                        tint = if (isCorrect) semanticColors.correct else semanticColors.wrong
                    )
                    Text(
                        text = feedbackText,
                        style = MaterialTheme.typography.titleSmall,
                        color = bannerTextColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AnswerOptionCard(
    index: Int,
    text: String,
    state: GameUiState.ActiveQuestion,
    onClick: () -> Unit
) {
    val semanticColors = LocalSemanticQuizColors.current
    val isSubmitted = state.isAnswerSubmitted
    val isSelected = state.selectedIndex == index
    val isCorrectOption = state.question.correctIndex == index

    val targetContainerColor = when {
        !isSubmitted -> MaterialTheme.colorScheme.surface
        isCorrectOption -> semanticColors.correctContainer
        isSelected -> semanticColors.wrongContainer
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
    }
    val targetBorderColor = when {
        !isSubmitted -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        isCorrectOption -> semanticColors.correct
        isSelected -> semanticColors.wrong
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    }
    val targetContentColor = when {
        !isSubmitted -> MaterialTheme.colorScheme.onSurface
        isCorrectOption -> semanticColors.onCorrectContainer
        isSelected -> semanticColors.onWrongContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val containerColor by animateColorAsState(targetContainerColor, label = "opt_bg_$index")
    val borderColor by animateColorAsState(targetBorderColor, label = "opt_border_$index")

    Card(
        onClick = onClick,
        enabled = !isSubmitted,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("answer_option_$index"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            disabledContainerColor = containerColor
        ),
        border = BorderStroke(
            width = if (isSubmitted && (isCorrectOption || isSelected)) 2.dp else 1.dp,
            color = borderColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = when {
                    isSubmitted && isCorrectOption -> semanticColors.correct
                    isSubmitted && isSelected -> semanticColors.wrong
                    else -> MaterialTheme.colorScheme.primaryContainer
                },
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = (index + 1).toPersianDigits(),
                        style = MaterialTheme.typography.labelLarge,
                        color = when {
                            isSubmitted && (isCorrectOption || isSelected) -> androidx.compose.ui.graphics.Color.White
                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                }
            }

            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = targetContentColor,
                modifier = Modifier.weight(1f)
            )

            if (isSubmitted && (isCorrectOption || isSelected)) {
                Icon(
                    imageVector = if (isCorrectOption) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                    contentDescription = null,
                    tint = if (isCorrectOption) semanticColors.correct else semanticColors.wrong
                )
            }
        }
    }
}
