package com.example.chess2.game.classes

data class ValidMoves(
    val state: MutableList<Pair<Int, Int>> = mutableListOf()
)
