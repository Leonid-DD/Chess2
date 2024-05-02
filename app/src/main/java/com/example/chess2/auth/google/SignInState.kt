package com.example.chess2.auth.google

data class SignInState(
    val isSignInSuccessful: Boolean = false,
    val signInError: String? = null
)