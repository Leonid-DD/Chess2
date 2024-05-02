package com.example.chess2

import android.os.Bundle
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
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.composable
import com.example.chess2.auth.SignInScreen
import com.example.chess2.auth.SignInViewModel
import com.example.chess2.auth.google.GoogleAuthUIClient
import com.example.chess2.game.GameStateViewModel
import com.example.chess2.temp.Chessboard
import com.example.chess2.temp.ProfileScreen
import com.example.chess2.temp.SearchGame
import com.example.chess2.ui.theme.Chess2Theme
import com.example.chess2.user.UserViewModel
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val googleAuthUiClient by lazy {
        GoogleAuthUIClient(
            context = applicationContext,
            oneTapClient = Identity.getSignInClient(applicationContext)
        )
    }

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
                            val viewModel = viewModel<SignInViewModel>()
                            val state by viewModel.state.collectAsStateWithLifecycle()

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
                                            viewModel.onSignInResult(signInResult)
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
                                    viewModel.resetState()
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

                            val viewModel = viewModel<UserViewModel>()
                            viewModel.initUser()

                            val game = viewModel<GameStateViewModel>()

                            val state by viewModel.state.collectAsStateWithLifecycle()

                            LaunchedEffect(key1 = state.isMatchingSuccessful) {
                                if (state.isMatchingSuccessful) {
                                    Toast.makeText(
                                        applicationContext,
                                        "Matchmaking is successful",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    navController.navigate("game")
                                    viewModel.resetState()

                                    game.initGame(state.users?.get(0)!!, state.users?.get(1)!!)
                                }
                            }

                            SearchGame(
                                state = state,
                                searchClick = {
                                    if (viewModel.getSearchStatus()) {
                                        viewModel.stopSearching()
                                    } else {
                                        lifecycleScope.launch {
                                            viewModel.startSearching()
                                            val match = viewModel.findMatchingUsers()
                                            viewModel.onMatchingResult(match)
                                        }
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
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable("game") {

                            //Chessboard()

                        }
                    }
                }
            }
        }
    }
}