package com.example.chess2.game

import androidx.lifecycle.MutableLiveData
import com.example.chess2.game.figures.Figure
import com.example.chess2.user.User
import java.util.Objects

data class Game(
    val gameId: String,
    val gameState: MutableList<MutableList<Figure?>>,
    val wPlayer: User,
    val bPlayer: User
) {

}
