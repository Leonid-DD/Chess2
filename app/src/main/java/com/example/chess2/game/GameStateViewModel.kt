package com.example.chess2.game

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.example.chess2.game.figures.Figure
import com.example.chess2.game.figures.FigureName
import com.example.chess2.game.figures.PlayerColor
import com.example.chess2.user.User
import com.example.chess2.user.UserQueue
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

class GameStateViewModel : ViewModel() {

    //Change to User
    private lateinit var wPlayer: UserQueue
    private lateinit var bPlayer: UserQueue

    private lateinit var game: Game

    private var selectedPiecePosition: Pair<Int, Int>? = null

    private var whiteMove: Boolean = true

    val db = FirebaseFirestore.getInstance()

    //Change to User
    fun initGame(user1: UserQueue, user2: UserQueue) {

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

        game = Game(boardState, wPlayer, bPlayer)

        db.collection("games").add(GameFB(convertToFB(boardState), wPlayer, bPlayer))
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

    fun getWhitePlayer(): UserQueue {
        return wPlayer
    }

    fun getBoardState(): MutableList<MutableList<Figure?>> {
        return game.gameState
    }

//    fun getBoardState1() = callbackFlow {
//
//        val collection = db.collection("games")
//
//        val snapshotListener = collection.addSnapshotListener { value, error ->
//            val response = if (error = null) {
//                OnSuccess(value)
//            } else {
//                OnError(error)
//            }
//
//            offer(response)
//        }
//
//        awaitClose {
//            snapshotListener.remove()
//        }
//
//    }

    fun getSelectedPiece(): Pair<Int, Int>? {
        return selectedPiecePosition
    }

    fun isWhiteMove(): Boolean {
        return whiteMove
    }

    fun convertToFB(game: MutableList<MutableList<Figure?>>): MutableList<Figure> {

        val result = mutableListOf<Figure>()

        for (row in 0..7) {
            for (col in 0..7) {
                val figure = game[row][col]
                if (figure != null) {
                    result.add(figure)
                }
            }
        }

        return result
    }

    fun convertFromFB(gameFB: MutableList<Figure>): MutableList<MutableList<Figure?>> {

        val result = MutableList (8) {
            MutableList<Figure?>(8) {
                null
            }
        }

        gameFB.sortBy { figure -> figure.row }

        for (figure in gameFB) {
            for (row in 0..7) {
                for (col in 0..7) {
                    result[row][col] =
                        if (figure.row == row && figure.col == col)
                            figure
                        else
                            null
                }
            }
        }

        return result

    }

}