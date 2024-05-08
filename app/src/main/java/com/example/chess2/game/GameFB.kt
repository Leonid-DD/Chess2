package com.example.chess2.game

import com.example.chess2.game.figures.Figure
import com.example.chess2.user.User
import com.example.chess2.user.UserQueue

data class GameFB(
    val gameId: String = "",
    val gameState: MutableList<Figure> = mutableListOf(),
    //Change to User
    val wPlayer: UserQueue? = null,
    val bPlayer: UserQueue? = null
) {
    constructor() : this("", mutableListOf(), null, null)
}

