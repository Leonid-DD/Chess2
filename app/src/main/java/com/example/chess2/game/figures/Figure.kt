package com.example.chess2.game.figures

data class Figure(
    val row: Int,
    val col: Int,
    val color: PlayerColor,
    val name: FigureName
) {}
