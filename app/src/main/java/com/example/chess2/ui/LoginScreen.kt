package com.example.chess2.ui

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.chess2.R

@Composable
fun LoginScreen() {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFD9E8D9))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Log in", fontSize = 24.sp, color = Color(0xFF4D774E))

        Spacer(modifier = Modifier.height(24.dp))

        BasicTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, shape = MaterialTheme.shapes.small)
                .padding(16.dp),
            singleLine = true,
            cursorBrush = SolidColor(Color.Black),
            decorationBox = { innerTextField ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_dialog_email),
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    if (email.isEmpty()) {
                        Text(
                            text = "Email",
                            color = Color.Gray,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    innerTextField()
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        BasicTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, shape = MaterialTheme.shapes.small)
                .padding(16.dp),
            singleLine = true,
            cursorBrush = SolidColor(Color.Black),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            decorationBox = { innerTextField ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_lock_24),
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    if (password.isEmpty()) {
                        Text(
                            text = "Password",
                            color = Color.Gray,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    innerTextField()
                    Icon(
                        painter = painterResource(id = if (passwordVisible) android.R.drawable.ic_menu_view else android.R.drawable.ic_menu_close_clear_cancel),
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clickable { passwordVisible = !passwordVisible }
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Forgot password?",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier
                .align(Alignment.End)
                .padding(end = 16.dp)
                .clickable { /* Handle forgot password */ }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { /* Handle login */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(text = "Log in")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Divider(modifier = Modifier.weight(1f), color = Color.Gray)
            Text(text = " OR ", color = Color.Gray, modifier = Modifier.padding(horizontal = 8.dp))
            Divider(modifier = Modifier.weight(1f), color = Color.Gray)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { /* Handle login with Google */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.google_logo), // Replace with Google icon
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(text = "Continue with Google")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Don’t have an account? Sign up",
            color = Color(0xFF4D774E),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clickable { /* Handle sign up */ }
        )
    }
}

@Preview
@Composable
fun PreviewLogin(modifier: Modifier = Modifier) {
    LoginScreen()
}