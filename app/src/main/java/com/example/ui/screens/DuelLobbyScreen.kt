package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.model.DuelRole
import com.example.model.DuelUiState
import com.example.model.QuizCategory
import com.example.network.ConnState
import com.example.ui.components.DuelDisconnectDialog
import com.example.ui.components.NearbyPermissionsGate
import com.example.ui.components.presentation
import com.example.ui.theme.LocalSemanticQuizColors
import com.example.util.toPersianDigits

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuelLobbyScreen(
    duelState: DuelUiState,
    playerName: String,
    onPlayerNameChange: (String) -> Unit,
    onStartHosting: (String) -> Unit,
    onStartJoining: (String) -> Unit,
    onCancelConnection: () -> Unit,
    onSelectCategory: (QuizCategory) -> Unit,
    onSelectQuestionCount: (Int) -> Unit,
    onStartMatch: () -> Unit,
    onReconnect: () -> Unit,
    onNavigateToDuelGame: () -> Unit,
    onNavigateBackHome: () -> Unit
) {
    BackHandler {
        onNavigateBackHome()
    }

    LaunchedEffect(duelState) {
        if (duelState is DuelUiState.Playing) {
            onNavigateToDuelGame()
        }
    }

    val defaultHostName = stringResource(R.string.duel_default_host_name)
    val defaultGuestName = stringResource(R.string.duel_default_guest_name)

    if (duelState is DuelUiState.Disconnected) {
        DuelDisconnectDialog(
            onReconnect = onReconnect,
            onReturnHome = onNavigateBackHome
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.duel_lobby_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBackHome,
                        modifier = Modifier.testTag("duel_lobby_back_button")
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
        NearbyPermissionsGate(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val lobby = duelState as? DuelUiState.Lobby ?: DuelUiState.Lobby()
            val connState = lobby.connState
            val isConnectingOrSearching = connState is ConnState.Advertising ||
                connState is ConnState.Discovering ||
                connState is ConnState.Connecting
            val isConnected = connState is ConnState.Connected

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    ConnectionStatusCard(
                        connState = connState,
                        opponentName = lobby.opponentName
                    )

                    if (!isConnected) {
                        OutlinedTextField(
                            value = playerName,
                            onValueChange = onPlayerNameChange,
                            enabled = !isConnectingOrSearching,
                            singleLine = true,
                            label = { Text(text = stringResource(R.string.duel_player_name_label)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("duel_player_name_input")
                        )

                        if (!isConnectingOrSearching) {
                            DuelRoleChoiceButton(
                                title = stringResource(R.string.duel_host_button),
                                subtitle = stringResource(R.string.duel_host_button_desc),
                                icon = Icons.Rounded.WifiTethering,
                                isPrimary = true,
                                testTag = "duel_host_button",
                                onClick = { onStartHosting(defaultHostName) }
                            )

                            DuelRoleChoiceButton(
                                title = stringResource(R.string.duel_join_button),
                                subtitle = stringResource(R.string.duel_join_button_desc),
                                icon = Icons.Rounded.Radar,
                                isPrimary = false,
                                testTag = "duel_join_button",
                                onClick = { onStartJoining(defaultGuestName) }
                            )
                        } else {
                            OutlinedButton(
                                onClick = onCancelConnection,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("duel_cancel_connection_button"),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(imageVector = Icons.Rounded.Close, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.duel_cancel_connection),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        Text(
                            text = stringResource(R.string.duel_hardware_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (lobby.role == DuelRole.HOST) {
                        HostMatchConfigurationCard(
                            selectedCategory = lobby.selectedCategory,
                            selectedQuestionCount = lobby.selectedQuestionCount,
                            onSelectCategory = onSelectCategory,
                            onSelectQuestionCount = onSelectQuestionCount,
                            onStartMatch = onStartMatch
                        )
                    } else {
                        GuestWaitingCard()
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    connState: ConnState,
    opponentName: String?
) {
    val semanticColors = LocalSemanticQuizColors.current
    val isConnected = connState is ConnState.Connected
    val isBusy = connState is ConnState.Advertising ||
        connState is ConnState.Discovering ||
        connState is ConnState.Connecting

    val containerColor = when {
        isConnected -> semanticColors.correctContainer
        connState is ConnState.Lost -> semanticColors.wrongContainer
        isBusy -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val contentColor = when {
        isConnected -> semanticColors.onCorrectContainer
        connState is ConnState.Lost -> semanticColors.onWrongContainer
        isBusy -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val statusText = when (connState) {
        ConnState.Idle -> stringResource(R.string.duel_status_idle)
        ConnState.Advertising -> stringResource(R.string.duel_status_advertising)
        ConnState.Discovering -> stringResource(R.string.duel_status_discovering)
        ConnState.Connecting -> stringResource(R.string.duel_status_connecting)
        ConnState.Connected -> {
            if (!opponentName.isNullOrBlank()) {
                stringResource(R.string.duel_status_connected_with_peer, opponentName)
            } else {
                stringResource(R.string.duel_status_connected)
            }
        }
        is ConnState.Lost -> stringResource(R.string.duel_status_lost)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("duel_connection_status_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 3.dp,
                    color = contentColor
                )
            } else {
                Icon(
                    imageVector = if (isConnected) Icons.Rounded.BluetoothConnected else Icons.Rounded.Devices,
                    contentDescription = null,
                    tint = if (isConnected) semanticColors.correct else contentColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.titleSmall,
                color = contentColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DuelRoleChoiceButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPrimary: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val containerColor = if (isPrimary) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (isPrimary) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (isPrimary) null else BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isPrimary) contentColor else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun HostMatchConfigurationCard(
    selectedCategory: QuizCategory,
    selectedQuestionCount: Int,
    onSelectCategory: (QuizCategory) -> Unit,
    onSelectQuestionCount: (Int) -> Unit,
    onStartMatch: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.duel_host_setup_title),
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = stringResource(R.string.home_choose_category),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                QuizCategory.entries.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { category ->
                            val presentation = category.presentation()
                            FilterChip(
                                selected = category == selectedCategory,
                                onClick = { onSelectCategory(category) },
                                label = { Text(text = stringResource(presentation.titleRes)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = presentation.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("duel_category_${category.id}")
                            )
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.duel_question_count_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(5, 10, 15).forEach { count ->
                    FilterChip(
                        selected = selectedQuestionCount == count,
                        onClick = { onSelectQuestionCount(count) },
                        label = {
                            Text(
                                text = stringResource(
                                    R.string.duel_question_count_option,
                                    count.toPersianDigits()
                                )
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("duel_count_$count")
                    )
                }
            }

            Button(
                onClick = onStartMatch,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("duel_start_match_button"),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.duel_start_match_button),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun GuestWaitingCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("duel_guest_waiting_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
            Text(
                text = stringResource(R.string.duel_waiting_for_host_start),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}
