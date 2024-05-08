package com.example.chess2

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.composable
import com.example.chess2.auth.SignInScreen
import com.example.chess2.auth.SignInViewModel
import com.example.chess2.auth.google.GoogleAuthUIClient
import com.example.chess2.auth.google.UserData
import com.example.chess2.game.Game
import com.example.chess2.game.GameFB
import com.example.chess2.game.GameStateViewModel
import com.example.chess2.game.figures.PlayerColor
import com.example.chess2.temp.Chessboard
import com.example.chess2.temp.SearchGame
import com.example.chess2.ui.theme.Chess2Theme
import com.example.chess2.user.UserQueue
import com.example.chess2.user.UserViewModel
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {

    private val googleAuthUiClient by lazy {
        GoogleAuthUIClient(
            context = applicationContext,
            oneTapClient = Identity.getSignInClient(applicationContext)
        )
    }

    private lateinit var firestoreListener: ListenerRegistration

    val db = FirebaseFirestore.getInstance()

    val signInViewModel = SignInViewModel()
    val userViewModel = UserViewModel()
    val gameViewModel = GameStateViewModel()

    @SuppressLint("CoroutineCreationDuringComposition")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Chess2Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "sign_in") {
                        composable("sign_in") {
                            val state by signInViewModel.state.collectAsStateWithLifecycle()

                            LaunchedEffect(key1 = Unit) {
                                if (googleAuthUiClient.getSignedInUser() != null) {
                                    navController.navigate("search_game")
                                }
                            }

                            val launcher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.StartIntentSenderForResult(),
                                onResult = { result ->
                                    if (result.resultCode == RESULT_OK) {
                                        lifecycleScope.launch {
                                            val signInResult = googleAuthUiClient.signInWithIntent(
                                                intent = result.data ?: return@launch
                                            )
                                            signInViewModel.onSignInResult(signInResult)
                                        }
                                    }
                                }
                            )

                            LaunchedEffect(key1 = state.isSignInSuccessful) {
                                if (state.isSignInSuccessful) {
                                    Toast.makeText(
                                        applicationContext,
                                        "Sign in successful",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    signInViewModel.resetState()

                                    userViewModel.initUser()

                                    navController.navigate("search_game")

                                }
                            }

                            SignInScreen(
                                state = state,
                                onSignInClick = {
                                    lifecycleScope.launch {
                                        val signInIntentSender = googleAuthUiClient.signIn()
                                        launcher.launch(
                                            IntentSenderRequest.Builder(
                                                signInIntentSender ?: return@launch
                                            ).build()
                                        )
                                    }
                                }
                            )
                        }
                        composable("search_game") {

                            userViewModel.initUser()
                            val state by userViewModel.state.collectAsStateWithLifecycle()

                            LaunchedEffect(key1 = state.isMatchingSuccessful) {
                                if (state.isMatchingSuccessful) {
                                    Toast.makeText(
                                        applicationContext,
                                        "Matchmaking is successful",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    navController.navigate("game")
                                    userViewModel.resetState()

                                    //Получить доски пользователей из БД по ID из поиска

                                    gameViewModel.initPlayers(state.users?.get(0)!!, state.users?.get(1)!!)
                                }
                            }

                            SearchGame(
                                state = state,
                                searchClick = {
                                    if (userViewModel.getSearchStatus()) {
                                        userViewModel.stopSearching()
                                    } else {
                                        userViewModel.startSearching()
                                    }
                                },
                                signOutClick = {
                                    lifecycleScope.launch {
                                        googleAuthUiClient.signOut()
                                        Toast.makeText(
                                            applicationContext,
                                            "Signed out",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    //navController.popBackStack()
                                }
                            )
                        }
                        composable("game") {

                            gameViewModel.initGame()

                            var currentUser: UserData? by remember { mutableStateOf(null) }
                            var whitePlayer: UserQueue? by remember { mutableStateOf(null) }
                            var currentPlayerColor by remember { mutableStateOf(PlayerColor.BLACK) }

                            val coroutineScope = rememberCoroutineScope()

                            LaunchedEffect(Unit) {
                                // Fetch current user data
                                currentUser = googleAuthUiClient.getSignedInUser()

                                // Fetch white player data in a coroutine
                                try {
                                    val player = gameViewModel.getWhitePlayer()
                                    whitePlayer = player
                                    currentPlayerColor = if (currentUser?.userId == whitePlayer?.userId)
                                        PlayerColor.WHITE
                                    else
                                        PlayerColor.BLACK
                                } catch (e: Exception) {
                                    Log.d("Error", e.toString())
                                }
                            }

                            if (currentUser != null && whitePlayer != null) {
                                Chessboard(
                                    boardState = gameViewModel.getBoardState(),
                                    selectedPiece = null,
                                    currentPlayerColor = currentPlayerColor,
                                    onPieceSelected = { figure -> gameViewModel.selectChessPiece(figure) },
                                    userId = currentUser?.userId,
                                    whiteUserId = whitePlayer?.userId
                                )
                            } else {
                                // Show a loading indicator or placeholder while data is being fetched
                                // Alternatively, you can show an empty UI or handle this case according to your app's design
                                Text("Loading...")
                            }

                        }
                    }
                }
            }
        }
    }
}