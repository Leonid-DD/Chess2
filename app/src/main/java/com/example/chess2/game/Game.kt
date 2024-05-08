package com.example.chess2.game

import androidx.lifecycle.MutableLiveData
import com.example.chess2.game.figures.Figure
import com.example.chess2.user.User
import com.example.chess2.user.UserQueue
import java.util.Objects

data class Game(
    val gameId: String,
    var gameState: MutableList<MutableList<Figure?>>,
    //Change to User
    val wPlayer: UserQueue,
    val bPlayer: UserQueue
) {}
