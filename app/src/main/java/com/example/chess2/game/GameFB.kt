package com.example.chess2.game

import com.example.chess2.game.figures.Figure
import com.example.chess2.user.User
import com.example.chess2.user.UserQueue

data class GameFB(
    val gameState: MutableList<Figure>,
    //Change to User
    val wPlayer: UserQueue,
    val bPlayer: UserQueue
) {

}

