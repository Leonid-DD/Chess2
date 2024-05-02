package com.example.chess2.game

import com.example.chess2.game.figures.Figure
import com.example.chess2.user.User

data class GameFB(
    val gameId: String,
    val gameState: MutableList<Figure?>,
    val wPlayer: User,
    val bPlayer: User
)
