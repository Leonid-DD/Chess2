package com.example.chess2.game.figures

data class Figure(
    var row: Int,
    var col: Int,
    val color: PlayerColor,
    val name: FigureName,
    var firstMove: Boolean = true
) {
    constructor() : this(0, 0, PlayerColor.WHITE, FigureName.PAWN)
}
