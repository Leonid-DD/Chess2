package com.example.chess2.game

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.chess2.game.figures.Figure
import com.example.chess2.game.figures.FigureName
import com.example.chess2.game.figures.PlayerColor
import com.example.chess2.user.UserQueue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.protobuf.Internal.BooleanList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID


class GameStateViewModel : ViewModel() {

    //game.gameState -> _gameState.value.state

    //Game State
    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    //Highlight State
    private val _highlightState = MutableStateFlow(ValidMoves())
    val highlightState: StateFlow<ValidMoves> = _highlightState.asStateFlow()

    //Other game properties
    //Change to User
    private lateinit var wPlayer: UserQueue
    private lateinit var bPlayer: UserQueue
    private lateinit var gameId: String

    var initDone: Boolean = false
    val db = FirebaseFirestore.getInstance()
    private var gameStateListener: ListenerRegistration? = null
    private lateinit var users: Pair<UserQueue, UserQueue>

    private var selectedPiecePosition: Pair<Int, Int>? = null
    private var whiteMove: Boolean = true

    //Change to User
    fun initPlayers(user1: UserQueue, user2: UserQueue) {
        users = Pair(user1, user2)
        gameId = createGameId(user1, user2)
        Log.d("INIT", "Players initialized")
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

        val user1 = if ((0..1).random() == 0) users.first else users.second
        val user2 = if (user1 == users.first) users.second else users.first

        _gameState.value = GameState(boardState)

        gameDetails().set(GameFB(gameId, convertToFB(boardState), user1, user2))

        Log.d("INIT", "Game initialized")

        initDone = true

    }

    suspend fun getPlayersFromFirestore() {

        val docRef = gameDetails()
        try {
            val documentSnapshot = docRef.get().await()
            val deserializer = GameFBDeserializer()
            val gameState = deserializer.deserialize(documentSnapshot)
            wPlayer = gameState?.wPlayer!!
            bPlayer = gameState?.bPlayer!!
            Log.d("WPLAYER", wPlayer.toString())
            Log.d("BPLAYER", bPlayer.toString())

            if (FirebaseAuth.getInstance().uid == bPlayer.userId) {
                startListeningForGameState()
            }
        } catch (e: Exception) {
            println("Error getting game state: $e")
        }
    }

    private fun gameDetails(): DocumentReference {
        return db.collection("games").document(gameId)
    }

    fun selectChessPiece(coordinates: Pair<Int, Int>) {
        val row = coordinates.first
        val col = coordinates.second
        Log.d("COORDINATEROW", row.toString())
        Log.d("COORDINATECOL", col.toString())
        val figure = _gameState.value.state[row][col]
        Log.d("FIGURE", figure.toString())
        if (selectedPiecePosition != null && isPlayerTurn()) {
            // Piece is already selected, attempt to move it to the new position
            moveChessPiece(row, col)
        } else {
            // No piece is selected, select the piece at the given position
            if (figure != null && isPlayerTurn()) {
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
            Log.d("FROMCOORDINATEROW", fromRow.toString())
            Log.d("FROMCOORDINATECOL", fromCol.toString())
            val pieceToMove = _gameState.value.state[fromRow][fromCol]
            Log.d("PIECETOMOVE", pieceToMove.toString())

            // Check if the move is valid
            if (isValidMove(pieceToMove!!, fromRow, fromCol, toRow, toCol)) {
                // Update local gameState
                updateGameState(fromRow, fromCol, toRow, toCol)
                // Clear selected piece position
                selectedPiecePosition = null
                // Change player turn
                changePlayer()
                // Update the Firestore database
                updateGameStateInFirestore()
                // Start or restart listening for game state updates
                startListeningForGameState()

                Log.d("ISPLAYERTURN", isPlayerTurn().toString())
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

    private fun isValidMove(
        piece: Figure,
        fromRow: Int,
        fromCol: Int,
        toRow: Int,
        toCol: Int
    ): Boolean {
        // Add your logic to validate the move based on the piece type and game rules
        // For now, you can return true to allow any move
        return true
    }

    private fun updateGameState(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int) {
        val currentState = _gameState.value.state.map { it.toMutableList() }
            .toMutableList() // Create a deep copy of the state
        val pieceToMove = currentState[fromRow][fromCol]
//        if (pieceToMove!!.firstMove) {
//            pieceToMove.firstMove = false
//        }
        pieceToMove?.row = toRow
        pieceToMove?.col = toCol
        currentState[toRow][toCol] = pieceToMove
        currentState[fromRow][fromCol] = null

        _gameState.value = GameState(currentState) // Set the new state

        Log.d("ENDSQUARE", _gameState.value.state[toRow][toCol].toString())
        Log.d("STARTSQUARE", _gameState.value.state[fromRow][fromCol].toString())
        Log.d("RESULTSTATE", _gameState.value.state.toString())
    }

    private fun updateGameStateInFirestore() {
        // Update the gameState in Firestore with the new state
        // You can access the Firestore database instance here and update the document accordingly
        db.collection("games").document(gameId)
            .update("gameState", convertToFB(_gameState.value.state))
    }

    fun changePlayer() {
        whiteMove = !whiteMove
    }

    fun getWhitePlayer(): UserQueue {
        return wPlayer
    }

    suspend fun getWhitePlayerFromFB(): UserQueue? {
        var gameState: GameFB? = null
        val docRef = gameDetails()
        Log.d("DOCUMENT", docRef.toString())
        try {
            val documentSnapshot = docRef.get().await()
            Log.d("SNAPSHOT", documentSnapshot.toString())
            val deserializer = GameFBDeserializer()
            gameState = deserializer.deserialize(documentSnapshot)
            Log.d("GAMESTATE", gameState.toString())
        } catch (e: Exception) {
            println("Error getting game state: $e")
        }

        return gameState?.wPlayer
    }

    fun getBoardState(): MutableList<MutableList<Figure?>> {
        return _gameState.value.state
    }

    fun addGameStateListener() {
        val docRef = db.collection("games").document(gameId)
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
                if (updatedGameState != null && _gameState.value != GameState(
                        convertFromFB(
                            updatedGameState
                        )
                    )
                ) {
                    // Update local game state
                    _gameState.value = GameState(convertFromFB(updatedGameState))
                    changePlayer()
                    Log.d("ISPLAYERTURN", isPlayerTurn().toString())
                    // Notify observers or update UI
                    // For example, you can trigger a LiveData update or call a method to update the UI
                }
            } else {
                println("Current data: null")
            }
        }
    }

    private fun isPlayerTurn(): Boolean {
        val currentUser = FirebaseAuth.getInstance().uid!!
        return (whiteMove && wPlayer.userId == currentUser) || (!whiteMove && bPlayer.userId == currentUser)
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
        val result = MutableList(8) {
            MutableList<Figure?>(8) {
                null
            }
        }
        for (figure in gameFB) {
            val row = figure.row
            val col = figure.col
            result[row][col] = figure
        }
        return result
    }

    suspend fun getGameStateFromFB(): MutableList<MutableList<Figure?>> {
        var gameState: GameFB? = null
        val docRef = gameDetails()
        try {
            val documentSnapshot = docRef.get().await()
            val deserializer = GameFBDeserializer()
            gameState = deserializer.deserialize(documentSnapshot)
            Log.d("FIRESTORESTATE", gameState.toString())
        } catch (e: Exception) {

        }
        val result = convertFromFB(gameState!!.gameState)
        Log.d("CONVERTEDSTATE", result.toString())
        return result
    }

    fun createGameId(user1: UserQueue, user2: UserQueue): String {
        return if (user1.userId.hashCode() > user2.userId.hashCode()) {
            user1.userId + "_" + user2.userId
        } else {
            user2.userId + "_" + user1.userId
        }
    }

    fun getCurrentPlayers(): String {
        return "b = {${bPlayer.userId}} : w = {${wPlayer.userId}}"
    }

    fun playersSynced(): Boolean {
        return this::wPlayer.isInitialized && this::bPlayer.isInitialized
    }

    fun calculateValidMoves() {
        if (selectedPiecePosition != null) {
            val selectedPiece =
                gameState.value.state[selectedPiecePosition!!.first][selectedPiecePosition!!.second]
            val playerColor = selectedPiece?.color
            val validMoves = mutableListOf<Pair<Int, Int>>()
            validMoves.add(selectedPiecePosition!!)

            when (selectedPiece?.name) {
                FigureName.PAWN -> {

                }

                FigureName.BISHOP -> {

                }

                FigureName.ROOK -> {

                }

                FigureName.KNIGHT -> {

                }

                FigureName.KING -> {

                }

                FigureName.QUEEN -> {

                }

                null -> TODO()
            }

            _highlightState.value = ValidMoves(validMoves)
        }

    }

    fun squareEmpty(coordinates: Pair<Int, Int>): Boolean {
        val targetSquare = gameState.value.state[coordinates.first][coordinates.second]
        return targetSquare == null
    }

    fun squareEnemyFigure(
        coordinates: Pair<Int, Int>,
        playerColor: PlayerColor
    ): Boolean {
        val targetSquare = gameState.value.state[coordinates.first][coordinates.second]
        return targetSquare?.color != playerColor
    }

    fun pawnFirstMove(coordinates: Pair<Int, Int>, length: Int) {

    }

    fun pawnBaseMove(coordinates: Pair<Int, Int>, length: Int) {

    }

    fun pawnEatLeft(coordinates: Pair<Int, Int>) {

    }

    fun pawnEatRight(coordinates: Pair<Int, Int>) {

    }

    fun straightLineMove(coordinates: Pair<Int, Int>, length: Int) {

    }

    fun diagonalLineMove(coordinates: Pair<Int, Int>, length: Int, isPawn: Boolean) {

    }

    fun knightFront(coordinates: Pair<Int, Int>, isPawn: Boolean) {

    }

    fun knightUpMid(coordinates: Pair<Int, Int>, isPawn: Boolean) {

    }

    fun knightDownMid(coordinates: Pair<Int, Int>) {

    }

    fun knightBack(coordinates: Pair<Int, Int>) {

    }

}