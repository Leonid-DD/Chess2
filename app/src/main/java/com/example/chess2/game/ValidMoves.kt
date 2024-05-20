package com.example.chess2.game

data class ValidMoves(
    val state: MutableList<Pair<Int, Int>> = mutableListOf()
)
