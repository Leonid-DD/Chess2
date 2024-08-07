package com.example.chess2

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.composable
import com.example.chess2.auth.SignInViewModel
import com.example.chess2.auth.google.GoogleAuthUIClient
import com.example.chess2.auth.google.UserData
import com.example.chess2.game.GameStateViewModel
import com.example.chess2.temp.GameScreen
import com.example.chess2.temp.SearchScreen
import com.example.chess2.ui.LoginScreen
import com.example.chess2.ui.theme.Chess2Theme
import com.example.chess2.user.UserQueue
import com.example.chess2.user.UserViewModel
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

                                    navController.navigate("search_game")

                                    signInViewModel.resetState()

                                    userViewModel.initUser()

                                }
                            }

                            LoginScreen(
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

                                    gameViewModel.initPlayers(
                                        state.users?.get(0)!!,
                                        state.users?.get(1)!!
                                    )
                                }
                            }

                            SearchScreen(
                                userData = googleAuthUiClient.getSignedInUser(),
                                state = state,
                                searchClick = {
                                    if (userViewModel.getSearchStatus()) {
                                        userViewModel.stopSearching()
                                    } else {
                                        userViewModel.startSearching()
                                    }
                                },
                                signOutClick = {
                                    val builder = AlertDialog.Builder(this@MainActivity)
                                    builder.setMessage("Вы уверены, что хотите выйти?")
                                        .setCancelable(false)
                                        .setPositiveButton("Да") { dialog, id ->
                                            lifecycleScope.launch {
                                                googleAuthUiClient.signOut()
                                                Toast.makeText(
                                                    applicationContext,
                                                    "Signed out",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                finishAndRemoveTask()
                                            }
                                        }
                                        .setNegativeButton("Нет") { dialog, id ->
                                            dialog.dismiss()
                                        }
                                    val alert = builder.create()
                                    alert.show()
                                },
                                changeGameModeClick = {
                                    if (!userViewModel.getSearchStatus()) {
                                        userViewModel.changeGameMode(it)
                                    }
                                    Log.d("SELECTEDMODE", userViewModel.getUser().gameMode.toString())
                                }
                            )
                        }
                        composable("game") {

                            //if (!gameViewModel.initDone) gameViewModel.initGame()

                            var currentUserId: String by remember { mutableStateOf("") }
                            var whitePlayer: UserQueue? by remember { mutableStateOf(null) }

                            LaunchedEffect(Unit) {
                                delay(2000)
                                gameViewModel.getPlayersFromFirestore()
                                currentUserId = FirebaseAuth.getInstance().uid!!
                                whitePlayer = gameViewModel.getWhitePlayerFromFB()
                            }

                            if (currentUserId != "" && whitePlayer != null) {
                                key(whitePlayer) {
                                    GameScreen(
                                        gameViewModel,
                                        navController
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFFD9E8D9)),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "Загрузка...",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}