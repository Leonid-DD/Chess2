package com.example.chess2.game

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.chess2.game.figures.Figure
import com.example.chess2.game.figures.FigureName
import com.example.chess2.game.figures.PlayerColor
import com.example.chess2.user.UserQueue
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await


class GameStateViewModel : ViewModel() {

    //Change to User
    private lateinit var wPlayer: UserQueue
    private lateinit var bPlayer: UserQueue

    private lateinit var game: Game

    private var selectedPiecePosition: Pair<Int, Int>? = null

    private var whiteMove: Boolean = true

    val db = FirebaseFirestore.getInstance()

    private var gameStateListener: ListenerRegistration? = null

    //Change to User
    fun initPlayers(user1: UserQueue, user2: UserQueue) {
        val coin = (0..1).random()
        if (coin == 0) {
            wPlayer = user1
            bPlayer = user2
        } else {
            wPlayer = user2
            bPlayer = user1
        }
    }

    fun initGame() {
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

        game = Game(createGameId(wPlayer, bPlayer), boardState, wPlayer, bPlayer)

        gameDetails(wPlayer, bPlayer).set(GameFB(createGameId(wPlayer, bPlayer), convertToFB(boardState), wPlayer, bPlayer))
    }

    private fun gameDetails(user1: UserQueue, user2: UserQueue): DocumentReference {
        return db.collection("games").document(createGameId(user1, user2))
    }

    fun selectChessPiece(coordinates: Pair<Int, Int>) {
        val row = coordinates.first
        val col = coordinates.second
        val figure = game.gameState[row][col]
        if (selectedPiecePosition != null) {
            // Piece is already selected, attempt to move it to the new position
            moveChessPiece(row, col)
        } else {
            // No piece is selected, select the piece at the given position
            if (figure != null && isPlayerTurn(figure.color)) {
                selectedPiecePosition = Pair(figure.row, figure.col)
            } else {
                selectedPiecePosition = null
            }
        }
    }

    fun moveChessPiece(toRow: Int, toCol: Int) {
        val selectedPiece = selectedPiecePosition
        if (selectedPiece != null) {
            val (fromRow, fromCol) = selectedPiece
            val pieceToMove = game.gameState[fromRow][fromCol]

            // Check if the move is valid
            if (isValidMove(pieceToMove!!, fromRow, fromCol, toRow, toCol)) {
                // Update local gameState
                game.gameState[toRow][toCol] = pieceToMove
                game.gameState[fromRow][fromCol] = null

                // Clear selected piece position
                selectedPiecePosition = null

                // Change player turn
                changePlayer()

                // Update the Firestore database
                updateGameStateInFirestore()

                // Start or restart listening for game state updates
                startListeningForGameState()
            } else {
                // Invalid move, handle accordingly (e.g., show error message)
            }
        }
    }

    private fun startListeningForGameState() {
        // Remove existing listener if it exists
        removeGameStateListener()

        // Add new listener
        addGameStateListener()
    }

    private fun removeGameStateListener() {
        gameStateListener?.remove()
        gameStateListener = null
    }

    private fun isValidMove(piece: Figure, fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Boolean {
        // Add your logic to validate the move based on the piece type and game rules
        // For now, you can return true to allow any move
        return true
    }

    private fun updateGameStateInFirestore() {
        // Update the gameState in Firestore with the new state
        // You can access the Firestore database instance here and update the document accordingly
        db.collection("games").document(game.gameId)
            .update("gameState", convertToFB(game.gameState))
    }

    fun changePlayer() {
        whiteMove = !whiteMove
    }

    suspend fun getWhitePlayer(): UserQueue? {
        var gameState: GameFB? = null
        val docRef = db.collection("games").document(game.gameId)
        try {
            val documentSnapshot = docRef.get().await()
            val deserializer = GameFBDeserializer()
            gameState = deserializer.deserialize(documentSnapshot)
        } catch (e: Exception) {
            println("Error getting game state: $e")
        }

        return gameState?.wPlayer
    }

    fun getBoardState(): MutableList<MutableList<Figure?>> {
        return game.gameState
    }

    fun addGameStateListener() {
        val docRef = db.collection("games").document(game.gameId)
        docRef.addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                // Handle any errors
                println("Error fetching game state: $exception")
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                // Document exists, update game state
                val deserializer = GameFBDeserializer()
                val updatedGameState = deserializer.deserialize(snapshot).gameState
                if (updatedGameState != null) {
                    // Update local game state
                    game.gameState = convertFromFB(updatedGameState)
                    changePlayer()
                    // Notify observers or update UI
                    // For example, you can trigger a LiveData update or call a method to update the UI
                }
            } else {
                println("Current data: null")
            }
        }
    }

    private fun isPlayerTurn(color: PlayerColor?): Boolean {
        return if (whiteMove && color == PlayerColor.WHITE) {
            true
        } else if (!whiteMove && color == PlayerColor.BLACK) {
            true
        } else {
            false
        }
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

    fun createGameId(user1: UserQueue, user2: UserQueue): String {
        return if (user1.userId.hashCode() > user2.userId.hashCode()) {
            user1.userId+"_"+user2.userId
        } else {
            user2.userId+"_"+user1.userId
        }
    }

}