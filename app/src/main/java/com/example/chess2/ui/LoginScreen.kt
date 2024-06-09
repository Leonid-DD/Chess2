package com.example.chess2.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.chess2.R
import com.example.chess2.auth.google.SignInState

@Composable
fun LoginScreen(
    state: SignInState,
    onSignInClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    LaunchedEffect(key1 = state.signInError) {
        state.signInError?.let { error ->
            Toast.makeText(
                context,
                error,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFD9E8D9))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Авторизация",
            fontSize = 24.sp,
            color = Color(0xFF4D774E)
        )

        Spacer(modifier = Modifier.height(120.dp))

        OutlinedButton(
            onClick = onSignInClick,
            modifier = Modifier
                .height(48.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.google_logo), // Replace with Google icon
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = "Продолжить с Google",
                color = Color(0xFF4D774E)
            )
        }
    }
}

@Preview
@Composable
fun PreviewLogin(modifier: Modifier = Modifier) {
    LoginScreen(SignInState(false, null), {})
}