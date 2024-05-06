package com.example.chess2.user

import com.example.chess2.game.figures.Figure

data class User(
    val userId: String,
    val userBoard: MutableList<Figure?>?,
    var searching: Boolean,
    var inGame: Boolean
)
