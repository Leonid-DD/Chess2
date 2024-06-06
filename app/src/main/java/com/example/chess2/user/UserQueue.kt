package com.example.chess2.user

import com.example.chess2.game.figures.Figure

data class UserQueue(
    val userId: String,
    var searching: Boolean,
    var gameMode: GameMode?
) {
}