package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Timer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.DuelUiState
import com.example.model.QuizCategory
import com.example.model.RoundFeedback
import com.example.ui.components.DuelDisconnectDialog
import com.example.ui.components.KeepScreenOnEffect
import com.example.ui.components.presentation
import com.example.ui.theme.LocalSemanticQuizColors
import com.example.util.toPersianDigits
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuelGameScreen(
    duelState: DuelUiState,
    onSubmitAnswer: (Int) -> Unit,
    onReconnect: () -> Unit,
    onNavigateToDuelResult: () -> Unit,
    onLeaveDuel: () -> Unit
) {
    KeepScreenOnEffect()

    BackHandler {
        onLeaveDuel()
    }

    LaunchedEffect(duelState) {
        if (duelState is DuelUiState.Finished) {
            onNavigateToDuelResult()
        }
    }

    if (duelState is DuelUiState.Disconnected) {
        DuelDisconnectDialog(
            onReconnect = onReconnect,
            onReturnHome = onLeaveDuel
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    val title = when (duelState) {
                        is DuelUiState.Playing -> {
                            val cat = QuizCategory.fromId(duelState.question.category)
                            stringResource(cat.presentation().titleRes)
                        }
                        else -> stringResource(R.string.duel_game_title)
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onLeaveDuel,
                        modifier = Modifier.testTag("duel_game_back_button")
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
            when (duelState) {
                is DuelUiState.Playing -> {
                    DuelPlayingContent(
                        state = duelState,
                        onSubmitAnswer = onSubmitAnswer
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = stringResource(R.string.game_loading),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DuelPlayingContent(
    state: DuelUiState.Playing,
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
        label = "duel_timer_color"
    )

    Column(
        modifier = Modifier
            .widthIn(max = 640.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Two score bars (Me vs Opponent)
        DuelScoreHeaderRow(
            myName = state.myName.ifBlank { stringResource(R.string.duel_me_label) },
            opponentName = state.opponentName.ifBlank { stringResource(R.string.duel_opponent_label) },
            myScore = state.myScore,
            opponentScore = state.opponentScore
        )

        // Counter & Timer Row
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                            .testTag("duel_question_counter")
                    )
                }

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
                        modifier = Modifier.testTag("duel_timer_text")
                    )
                }
            }

            LinearProgressIndicator(
                progress = { timerFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .testTag("duel_countdown_bar"),
                color = timerColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
        }

        // Question Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Text(
                text = state.question.text.toPersianDigits(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 22.dp)
                    .testTag("duel_question_text")
            )
        }

        // 4 Option Buttons
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.question.options.forEachIndexed { index, optionText ->
                DuelAnswerOptionCard(
                    index = index,
                    text = optionText.toPersianDigits(),
                    state = state,
                    onClick = { onSubmitAnswer(index) }
                )
            }
        }

        // Waiting for Opponent Banner
        AnimatedVisibility(
            visible = state.waitingForOpponent && state.roundResult == null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("duel_waiting_opponent_banner"),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.HourglassTop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = stringResource(R.string.duel_waiting_opponent),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // Per-round Result Animation Banner
        AnimatedVisibility(
            visible = state.roundResult != null,
            enter = fadeIn() + scaleIn(
                initialScale = 0.94f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            ),
            exit = fadeOut()
        ) {
            state.roundResult?.let { feedback ->
                DuelRoundFeedbackBanner(feedback = feedback)
            }
        }
    }
}

@Composable
private fun DuelScoreHeaderRow(
    myName: String,
    opponentName: String,
    myScore: Int,
    opponentScore: Int
) {
    val animatedMyScore by animateIntAsState(targetValue = myScore, label = "my_score_anim")
    val animatedOppScore by animateIntAsState(targetValue = opponentScore, label = "opp_score_anim")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PlayerScorePill(
            label = myName,
            score = animatedMyScore,
            isLocal = true,
            modifier = Modifier
                .weight(1f)
                .testTag("duel_my_score_card")
        )
        PlayerScorePill(
            label = opponentName,
            score = animatedOppScore,
            isLocal = false,
            modifier = Modifier
                .weight(1f)
                .testTag("duel_opponent_score_card")
        )
    }
}

@Composable
private fun PlayerScorePill(
    label: String,
    score: Int,
    isLocal: Boolean,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isLocal) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = if (isLocal) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = score.toPersianDigits(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

@Composable
private fun DuelAnswerOptionCard(
    index: Int,
    text: String,
    state: DuelUiState.Playing,
    onClick: () -> Unit
) {
    val semanticColors = LocalSemanticQuizColors.current
    val feedback = state.roundResult
    val isSelected = state.myChoice == index
    val isRevealed = feedback != null
    val isCorrectOption = feedback?.correctIndex == index
    val isLocked = state.myChoice != null || isRevealed

    val targetBg = when {
        !isRevealed && isSelected -> MaterialTheme.colorScheme.primaryContainer
        !isRevealed -> MaterialTheme.colorScheme.surface
        isCorrectOption -> semanticColors.correctContainer
        isSelected -> semanticColors.wrongContainer
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
    }
    val targetBorder = when {
        !isRevealed && isSelected -> MaterialTheme.colorScheme.primary
        !isRevealed -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        isCorrectOption -> semanticColors.correct
        isSelected -> semanticColors.wrong
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    }
    val targetTextColor = when {
        !isRevealed && isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        !isRevealed -> MaterialTheme.colorScheme.onSurface
        isCorrectOption -> semanticColors.onCorrectContainer
        isSelected -> semanticColors.onWrongContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val containerColor by animateColorAsState(targetBg, label = "duel_opt_bg_$index")
    val borderColor by animateColorAsState(targetBorder, label = "duel_opt_border_$index")

    Card(
        onClick = onClick,
        enabled = !isLocked,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("duel_option_$index"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            disabledContainerColor = containerColor
        ),
        border = BorderStroke(
            width = if (isSelected || isCorrectOption) 2.dp else 1.dp,
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
                    isRevealed && isCorrectOption -> semanticColors.correct
                    isRevealed && isSelected -> semanticColors.wrong
                    isSelected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.primaryContainer
                },
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = (index + 1).toPersianDigits(),
                        style = MaterialTheme.typography.labelLarge,
                        color = when {
                            isSelected || (isRevealed && isCorrectOption) -> Color.White
                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                }
            }

            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = targetTextColor,
                modifier = Modifier.weight(1f)
            )

            if (isRevealed && (isCorrectOption || isSelected)) {
                Icon(
                    imageVector = if (isCorrectOption) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                    contentDescription = null,
                    tint = if (isCorrectOption) semanticColors.correct else semanticColors.wrong
                )
            }
        }
    }
}

@Composable
private fun DuelRoundFeedbackBanner(feedback: RoundFeedback) {
    val semanticColors = LocalSemanticQuizColors.current
    val containerColor = if (feedback.myCorrect) {
        semanticColors.correctContainer
    } else {
        semanticColors.wrongContainer
    }
    val contentColor = if (feedback.myCorrect) {
        semanticColors.onCorrectContainer
    } else {
        semanticColors.onWrongContainer
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("duel_round_feedback_banner"),
        shape = MaterialTheme.shapes.medium,
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (feedback.myCorrect) {
                    stringResource(
                        R.string.duel_round_you_correct,
                        feedback.myPointsEarned.toPersianDigits()
                    )
                } else {
                    stringResource(R.string.duel_round_you_wrong)
                },
                style = MaterialTheme.typography.titleSmall,
                color = contentColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (feedback.opponentCorrect) {
                    stringResource(
                        R.string.duel_round_opponent_correct,
                        feedback.opponentPointsEarned.toPersianDigits()
                    )
                } else {
                    stringResource(R.string.duel_round_opponent_wrong)
                },
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.85f)
            )
        }
    }
}
