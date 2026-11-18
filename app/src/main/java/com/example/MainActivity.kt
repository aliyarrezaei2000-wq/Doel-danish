package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.AssetQuestionDataSource
import com.example.data.DefaultQuestionRepository
import com.example.data.RoomMistakeRepository
import com.example.data.local.DuelDaneshDatabase
import com.example.network.NearbyTransport
import com.example.ui.navigation.DuelGameRoute
import com.example.ui.navigation.DuelLobbyRoute
import com.example.ui.navigation.DuelResultRoute
import com.example.ui.navigation.GameRoute
import com.example.ui.navigation.HomeRoute
import com.example.ui.navigation.MistakesRoute
import com.example.ui.navigation.ResultRoute
import com.example.ui.screens.DuelGameScreen
import com.example.ui.screens.DuelLobbyScreen
import com.example.ui.screens.DuelResultScreen
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MistakesScreen
import com.example.ui.screens.ResultScreen
import com.example.ui.theme.DuelDaneshTheme
import com.example.ui.viewmodel.DuelViewModel
import com.example.ui.viewmodel.QuizViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = DuelDaneshDatabase.getInstance(applicationContext)
        val questionRepository = DefaultQuestionRepository(
            assetDataSource = AssetQuestionDataSource(applicationContext)
        )
        val mistakeRepository = RoomMistakeRepository(database.mistakeDao())
        val quizViewModelFactory = QuizViewModel.provideFactory(
            questionRepository = questionRepository,
            mistakeRepository = mistakeRepository
        )
        val duelViewModelFactory = DuelViewModel.provideFactory(
            transport = NearbyTransport(applicationContext),
            questionRepository = questionRepository,
            mistakeRepository = mistakeRepository,
            defaultPlayerName = getString(R.string.duel_default_player_name)
        )

        setContent {
            DuelDaneshTheme {
                val quizViewModel: QuizViewModel = viewModel(factory = quizViewModelFactory)
                val duelViewModel: DuelViewModel = viewModel(factory = duelViewModelFactory)
                DuelDaneshApp(
                    quizViewModel = quizViewModel,
                    duelViewModel = duelViewModel
                )
            }
        }
    }
}

@Composable
fun DuelDaneshApp(
    quizViewModel: QuizViewModel,
    duelViewModel: DuelViewModel
) {
    val navController = rememberNavController()
    val selectedCategory by quizViewModel.selectedCategory.collectAsStateWithLifecycle()
    val activeMode by quizViewModel.activeMode.collectAsStateWithLifecycle()
    val gameState by quizViewModel.gameState.collectAsStateWithLifecycle()
    val mistakes by quizViewModel.mistakes.collectAsStateWithLifecycle()

    val duelState by duelViewModel.duelState.collectAsStateWithLifecycle()
    val duelPlayerName by duelViewModel.playerName.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = HomeRoute
    ) {
        composable<HomeRoute> {
            HomeScreen(
                selectedCategory = selectedCategory,
                mistakeCount = mistakes.size,
                onSelectCategory = quizViewModel::selectCategory,
                onStartSolo = { category ->
                    quizViewModel.startSoloRound(category)
                    navController.navigate(GameRoute)
                },
                onOpenDuel = {
                    navController.navigate(DuelLobbyRoute)
                },
                onOpenMistakes = {
                    navController.navigate(MistakesRoute)
                }
            )
        }

        composable<GameRoute> {
            GameScreen(
                gameState = gameState,
                onSubmitAnswer = quizViewModel::submitAnswer,
                onNavigateToResult = {
                    navController.navigate(ResultRoute) {
                        popUpTo(GameRoute) { inclusive = true }
                    }
                },
                onNavigateBackHome = {
                    quizViewModel.exitGame()
                    navController.popBackStack(HomeRoute, inclusive = false)
                }
            )
        }

        composable<ResultRoute> {
            ResultScreen(
                gameState = gameState,
                activeMode = activeMode,
                onPlayAgain = {
                    quizViewModel.replayCurrentMode()
                    navController.navigate(GameRoute) {
                        popUpTo(ResultRoute) { inclusive = true }
                    }
                },
                onGoHome = {
                    quizViewModel.exitGame()
                    navController.popBackStack(HomeRoute, inclusive = false)
                }
            )
        }

        composable<MistakesRoute> {
            MistakesScreen(
                mistakes = mistakes,
                onStartReview = {
                    quizViewModel.startMistakesReviewRound()
                    navController.navigate(GameRoute)
                },
                onDeleteMistake = quizViewModel::removeMistake,
                onClearAll = quizViewModel::clearAllMistakes,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable<DuelLobbyRoute> {
            DuelLobbyScreen(
                duelState = duelState,
                playerName = duelPlayerName,
                onPlayerNameChange = duelViewModel::updatePlayerName,
                onStartHosting = duelViewModel::startHosting,
                onStartJoining = duelViewModel::startJoining,
                onCancelConnection = duelViewModel::cancelOrLeaveDuel,
                onSelectCategory = duelViewModel::selectCategory,
                onSelectQuestionCount = duelViewModel::selectQuestionCount,
                onStartMatch = duelViewModel::startMatch,
                onReconnect = duelViewModel::reconnect,
                onNavigateToDuelGame = {
                    navController.navigate(DuelGameRoute) {
                        popUpTo(DuelLobbyRoute) { inclusive = true }
                    }
                },
                onNavigateBackHome = {
                    duelViewModel.cancelOrLeaveDuel()
                    navController.popBackStack(HomeRoute, inclusive = false)
                }
            )
        }

        composable<DuelGameRoute> {
            DuelGameScreen(
                duelState = duelState,
                onSubmitAnswer = duelViewModel::submitAnswer,
                onReconnect = duelViewModel::reconnect,
                onNavigateToDuelResult = {
                    navController.navigate(DuelResultRoute) {
                        popUpTo(DuelGameRoute) { inclusive = true }
                    }
                },
                onLeaveDuel = {
                    duelViewModel.cancelOrLeaveDuel()
                    navController.popBackStack(HomeRoute, inclusive = false)
                }
            )
        }

        composable<DuelResultRoute> {
            DuelResultScreen(
                duelState = duelState,
                onRequestRematch = duelViewModel::requestRematch,
                onReconnect = duelViewModel::reconnect,
                onNavigateToDuelGame = {
                    navController.navigate(DuelGameRoute) {
                        popUpTo(DuelResultRoute) { inclusive = true }
                    }
                },
                onLeaveDuel = {
                    duelViewModel.cancelOrLeaveDuel()
                    navController.popBackStack(HomeRoute, inclusive = false)
                }
            )
        }
    }
}
