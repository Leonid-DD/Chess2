package com.example.chess2.game

import com.example.chess2.game.figures.Figure
import com.example.chess2.game.figures.FigureName
import com.example.chess2.game.figures.PlayerColor

class Logic {

    var piecesBox = mutableSetOf<Figure>()

    init {
        reset()
    }

    private fun reset() {
        piecesBox.removeAll(piecesBox)
        for (i in 0..1) {

            //Rook
            piecesBox.add(Figure(0 + i * 7,0, PlayerColor.WHITE, FigureName.ROOK))
            piecesBox.add(Figure(0 + i * 7,7, PlayerColor.BLACK, FigureName.ROOK))

            //Knight
            piecesBox.add(Figure(1 + i * 5,0, PlayerColor.WHITE, FigureName.KNIGHT))
            piecesBox.add(Figure(1 + i * 5,7, PlayerColor.BLACK, FigureName.KNIGHT))

            //Bishop
            piecesBox.add(Figure(2 + i * 3,0, PlayerColor.WHITE, FigureName.BISHOP))
            piecesBox.add(Figure(2 + i * 3,7, PlayerColor.BLACK, FigureName.BISHOP))

        }
        //Pawns
        for (i in 0..7) {

            piecesBox.add(Figure(i,1, PlayerColor.WHITE, FigureName.PAWN))
            piecesBox.add(Figure(i,6, PlayerColor.BLACK, FigureName.PAWN))

        }

        //Kings and Queens
        for (i in 0..1) {

            val white = if (i == 0) PlayerColor.WHITE else PlayerColor.BLACK

            piecesBox.add(Figure(3,0 + i * 7,white, FigureName.KING))
            piecesBox.add(Figure(4,0 + i * 7,white, FigureName.QUEEN))

        }
    }

    override fun toString(): String {
        var desc = " \n"
        for (row in 0..7) {
            val r = 7 - row
            desc += "${r + 1}"
            for (col in 0..7) {
                val piece = pieceAt(col, row)
                if (piece == null)
                    desc += "  ."
                else {
                    val white = piece.color == PlayerColor.WHITE
                    desc += when (piece.name) {
                        FigureName.PAWN -> if (white) " wP" else " bP"
                        FigureName.KNIGHT -> if (white) " wK" else " bK"
                        FigureName.ROOK -> if (white) " wR" else " bR"
                        FigureName.BISHOP -> if (white) " wB" else " bB"
                        FigureName.QUEEN -> if (white) " wQ" else " bQ"
                        FigureName.KING -> if (white) " w*" else " b*"
                    }
                }

            }
            desc += "\n"
        }
        desc += "   a  b  c  d  e  f  g  h"

        return desc
    }

    private fun pieceAt(col: Int, row: Int): Figure? {
        for (piece in piecesBox) {
            if (col == piece.col && row == piece.row) return piece
        }
        return null
    }

}