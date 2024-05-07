package com.example.chess2.user

data class MatchingState (
    val isMatchingSuccessful: Boolean = false,
    val matchingError: String? = null,
    val users: List<UserQueue>? = null
) {}