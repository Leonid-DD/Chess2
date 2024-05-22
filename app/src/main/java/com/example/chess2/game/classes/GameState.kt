package com.example.chess2.game.classes

import com.example.chess2.game.figures.Figure

data class GameState (
    val state: MutableList<MutableList<Figure?>> = mutableListOf()
) {
}