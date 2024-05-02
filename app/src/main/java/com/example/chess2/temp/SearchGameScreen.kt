package com.example.chess2.temp

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.chess2.ui.theme.Chess2Theme
import com.example.chess2.user.MatchingState

@Composable
fun SearchGame(
    state: MatchingState,
    searchClick: () -> Unit,
    signOutClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    val context = LocalContext.current
    LaunchedEffect(key1 = state.matchingError) {
        state.matchingError?.let { error ->
            Toast.makeText(
                context,
                error,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = {
                isPressed = !isPressed
                searchClick()
            }
        ) {
            if (isPressed) {
                Text("Stop searching")
            } else {
                Text("Start searching")
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = {
                signOutClick()
            }
        ) {
            Text(text = "Sign out")
        }
    }
}