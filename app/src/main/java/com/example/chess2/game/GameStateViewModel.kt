package com.example.chess2.game

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.example.chess2.game.figures.Figure
import com.example.chess2.game.figures.FigureName
import com.example.chess2.game.figures.PlayerColor
import com.example.chess2.user.User

class GameStateViewModel : ViewModel() {

    private lateinit var wPlayer: User
    private lateinit var bPlayer: User

    private lateinit var game: Game

    private var selectedPiecePosition: Pair<Int, Int>? = null

    private var whiteMove: Boolean = true

    val db = FirebaseFirestore.getInstance()

    fun initGame(user1: User, user2: User) {

        val boardState = MutableList(8) { row ->
            MutableList(8) { col ->
                when (row) {
                    0 -> when (col) {
                        0, 7 -> Figure(row, col, PlayerColor.BLACK, FigureName.ROOK)
                        1, 6 -> Figure(row, col, PlayerColor.BLACK, FigureName.KNIGHT)
                        2, 5 -> Figure(row, col, PlayerColor.BLACK, FigureName.BISHOP)
                        3 -> Figure(row, col, PlayerColor.BLACK, FigureName.QUEEN)
                        4 -> Figure(row, col, PlayerColor.BLACK, FigureName.KING)
                        else -> null
                    }

                    1 -> Figure(row, col, PlayerColor.BLACK, FigureName.PAWN)
                    6 -> Figure(row, col, PlayerColor.WHITE, FigureName.PAWN)
                    7 -> when (col) {
                        0, 7 -> Figure(row, col, PlayerColor.WHITE, FigureName.ROOK)
                        1, 6 -> Figure(row, col, PlayerColor.WHITE, FigureName.KNIGHT)
                        2, 5 -> Figure(row, col, PlayerColor.WHITE, FigureName.BISHOP)
                        3 -> Figure(row, col, PlayerColor.WHITE, FigureName.QUEEN)
                        4 -> Figure(row, col, PlayerColor.WHITE, FigureName.KING)
                        else -> null
                    }

                    else -> null
                }
            }
        }

        wPlayer = user1
        bPlayer = user2
        game = Game(user1.userId + user2.userId, boardState, wPlayer, bPlayer)



        db.collection("games").add(game)
    }

    fun selectChessPiece(figure: Figure?) {
        val selectedPiece = figure
        if (selectedPiece != null) {
            selectedPiecePosition = Pair(figure.row, figure.col)
        } else {
            selectedPiecePosition = null
        }
    }

    fun moveChessPiece(toRow: Int, toCol: Int) {
        val selectedPiece = selectedPiecePosition
        if (selectedPiecePosition != null) {

            val (fromRow, fromCol) = selectedPiecePosition!!
            val pieceToMove = game.gameState[fromRow][fromCol]
            game.gameState[toRow][toCol] = pieceToMove
            game.gameState[fromRow][fromCol] = null
            selectedPiecePosition = null

            changePlayer()
        }
    }

    fun changePlayer() {
        whiteMove = !whiteMove
    }

    fun getWhitePlayer(): User {
        return wPlayer
    }

    fun getBoardState(): MutableList<MutableList<Figure?>> {
        return game.gameState
    }

    fun getSelectedPiece(): Pair<Int, Int>? {
        return selectedPiecePosition
    }

    fun isWhiteMove(): Boolean {
        return whiteMove
    }

//    fun convertToFirebase(game: Game): GameFB {
//
//        val stateFb = MutableList<Figure?>(64) {
//            for (row in 0..7) {
//                for (col in 0..7) {
//                    game.gameState[row][col]
//                }
//            }
//        }
//        return GameFB(game.gameId, stateFb, game.wPlayer, game.bPlayer)
//    }
//
//    fun convertFromFirebase(gameFb: GameFB): Game {
//
//
//
//    }
}