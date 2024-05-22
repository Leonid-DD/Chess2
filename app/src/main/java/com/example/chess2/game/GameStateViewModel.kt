package com.example.chess2.game

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
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
        val figure = _gameState.value.state[row][col]
        Log.d("FIGURE", figure.toString())
        if (!isPlayerTurn()) {
            return
        }
        if (selectedPiecePosition != null) {
            if (figure != null && figure.color == if (FirebaseAuth.getInstance().uid == wPlayer.userId) PlayerColor.WHITE else PlayerColor.BLACK) {
                selectedPiecePosition = Pair(figure.row, figure.col)
                calculateValidMoves()
            } else moveChessPiece(row, col)
        } else if (figure != null && figure.color == if (FirebaseAuth.getInstance().uid == wPlayer.userId) PlayerColor.WHITE else PlayerColor.BLACK) {
            selectedPiecePosition = Pair(figure.row, figure.col)
            calculateValidMoves()
        }
    }

    fun moveChessPiece(toRow: Int, toCol: Int) {
        val selectedPiece = selectedPiecePosition
        if (selectedPiece != null) {
            val (fromRow, fromCol) = selectedPiece
            val pieceToMove = _gameState.value.state[fromRow][fromCol]
            Log.d("PIECETOMOVE", pieceToMove.toString())
            Log.d("FROMCOORDINATE", fromRow.toString() + " / " + fromCol.toString())
            Log.d("TOCOORDINATE", toRow.toString() + " / " + toCol.toString())
            Log.d("VALIDMOVES", highlightState.value.state.toString())

            // Check if the move is valid
            if (isValidMove(fromRow, fromCol, toRow, toCol)) {
                // Update local gameState
                updateGameState(fromRow, fromCol, toRow, toCol)
                //Clear highlight
                updateHighlight()
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
                Log.d("INVALIDTURN", "Piece was not moved")
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
        fromRow: Int,
        fromCol: Int,
        toRow: Int,
        toCol: Int
    ): Boolean {
        Log.d("VALIDATION", _highlightState.value.state.contains(Pair(toRow, toCol)).toString())
        return Pair(fromRow, fromCol) != Pair(toRow, toCol) && _highlightState.value.state.contains(Pair(toRow, toCol))
    }

    private fun updateGameState(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int) {
        val currentState = _gameState.value.state.map { it.toMutableList() }
            .toMutableList() // Create a deep copy of the state
        val pieceToMove = currentState[fromRow][fromCol]
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

    private fun updateHighlight() {
        _highlightState.value = ValidMoves(mutableListOf())
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
    fun createGameId(user1: UserQueue, user2: UserQueue): String {
        return if (user1.userId.hashCode() > user2.userId.hashCode()) {
            user1.userId + "_" + user2.userId
        } else {
            user2.userId + "_" + user1.userId
        }
    }

    fun calculateValidMoves() {
        if (selectedPiecePosition != null) {
            val selectedPiece =
                gameState.value.state[selectedPiecePosition!!.first][selectedPiecePosition!!.second]
            val coordinates: Pair<Int, Int> = Pair(selectedPiece?.row!!, selectedPiece?.col!!)
            val isWhite = selectedPiece?.color == PlayerColor.WHITE
            val validMoves = mutableListOf<Pair<Int, Int>>()
            validMoves.add(selectedPiecePosition!!)

            when (selectedPiece?.name) {
                FigureName.PAWN -> {
                    validMoves.addAll(pawnFirstMove(coordinates, 2, isWhite))
                    validMoves.addAll(pawnBaseMove(coordinates, 1, isWhite))
                    validMoves.addAll(pawnTake(coordinates, 1, isWhite))
                }

                FigureName.BISHOP -> {
                    validMoves.addAll(diagonalLineMove(coordinates, 7))
                }

                FigureName.ROOK -> {
                    validMoves.addAll(straightLineMove(coordinates, 7))
                }

                FigureName.KNIGHT -> {
                    validMoves.addAll(knightFront(coordinates, isWhite, false))
                    validMoves.addAll(knightUpMid(coordinates, isWhite, false))
                    validMoves.addAll(knightDownMid(coordinates, isWhite))
                    validMoves.addAll(knightBack(coordinates, isWhite))
                }

                FigureName.KING -> {
                    validMoves.addAll(straightLineMove(coordinates, 1))
                    validMoves.addAll(diagonalLineMove(coordinates, 1))
                }

                FigureName.QUEEN -> {
                    validMoves.addAll(straightLineMove(coordinates, 7))
                    validMoves.addAll(diagonalLineMove(coordinates, 7))
                }

                null -> TODO()
            }

            _highlightState.value = ValidMoves(validMoves)
        }

    }

    fun pawnFirstMove(
        coordinates: Pair<Int, Int>,
        length: Int,
        isWhite: Boolean
    ): List<Pair<Int, Int>> {
        val gameState = _gameState.value.state
        val (startRow, startCol) = coordinates
        val moves = mutableListOf<Pair<Int, Int>>()

        val direction = if (isWhite) -1 else 1
        val startRowForTwoStep = if (isWhite) 6 else 1

        // First move double step
        if (startRow == startRowForTwoStep) {
            val twoStepsForward = Pair(startRow + length * direction, startCol)
            var validMove = true
            for (i in 1..length) {
                val copy = Pair(startRow + i * direction, startCol)
                validMove = gameState[copy.first][copy.second] == null
                if (!validMove) return emptyList()
            }
            if (isValidPosition(
                    twoStepsForward.first,
                    twoStepsForward.second
                ) && gameState[twoStepsForward.first][twoStepsForward.second] == null
            ) {
                moves.add(twoStepsForward)
            }
        }

        return moves
    }

    fun pawnBaseMove(
        coordinates: Pair<Int, Int>,
        length: Int,
        isWhite: Boolean
    ): List<Pair<Int, Int>> {
        val gameState = _gameState.value.state
        val (startRow, startCol) = coordinates
        val moves = mutableListOf<Pair<Int, Int>>()

        val direction = if (isWhite) -1 else 1
        val startRowForTwoStep = if (isWhite) 6 else 1

        // Standard forward move
        for (i in 1..length) {
            val oneStepForward = Pair(startRow + length * direction, startCol)
            if (isValidPosition(
                    oneStepForward.first,
                    oneStepForward.second
                ) && gameState[oneStepForward.first][oneStepForward.second] == null
            ) {
                moves.add(oneStepForward)
            }
        }

        return moves
    }

    fun pawnTake(coordinates: Pair<Int, Int>, length: Int, isWhite: Boolean): List<Pair<Int, Int>> {
        val gameState = _gameState.value.state
        val (startRow, startCol) = coordinates
        val moves = mutableListOf<Pair<Int, Int>>()

        val direction = if (isWhite) -1 else 1
        for (i in 1..length) {
            val captureMoves = listOf(
                Pair(startRow + direction, startCol - 1),
                Pair(startRow + direction, startCol + 1)
            )
            for (captureMove in captureMoves) {
                if (isValidPosition(captureMove.first, captureMove.second)) {
                    val targetPiece = gameState[captureMove.first][captureMove.second]
                    if (targetPiece != null && targetPiece.color != gameState[startRow][startCol]?.color) {
                        moves.add(captureMove)
                    }
                }
            }
        }

        return moves
    }

    fun straightLineMove(coordinates: Pair<Int, Int>, length: Int): List<Pair<Int, Int>> {
        val gameState = _gameState.value.state
        val (startRow, startCol) = coordinates
        val moves = mutableListOf<Pair<Int, Int>>()
        val directions = listOf(
            Pair(1, 0),  // Down
            Pair(-1, 0), // Up
            Pair(0, 1),  // Right
            Pair(0, -1)  // Left
        )
        for (direction in directions) {
            for (i in 1..length) {
                val newRow = startRow + i * direction.first
                val newCol = startCol + i * direction.second
                if (!isValidPosition(newRow, newCol)) break
                val targetPiece = gameState[newRow][newCol]
                if (targetPiece == null) {
                    moves.add(Pair(newRow, newCol))
                } else {
                    if (targetPiece.color != gameState[startRow][startCol]?.color) {
                        moves.add(Pair(newRow, newCol)) // Capture
                    }
                    break
                }
            }
        }
        return moves
    }

    fun diagonalLineMove(coordinates: Pair<Int, Int>, length: Int): List<Pair<Int, Int>> {
        val gameState = _gameState.value.state
        val (startRow, startCol) = coordinates
        val moves = mutableListOf<Pair<Int, Int>>()
        val directions = listOf(
            Pair(1, 1),  // Down-right
            Pair(1, -1), // Down-left
            Pair(-1, 1), // Up-right
            Pair(-1, -1) // Up-left
        )
        for (direction in directions) {
            for (i in 1..length) {
                val newRow = startRow + i * direction.first
                val newCol = startCol + i * direction.second
                if (!isValidPosition(newRow, newCol)) break
                val targetPiece = gameState[newRow][newCol]
                if (targetPiece == null) {
                    moves.add(Pair(newRow, newCol))
                } else {
                    if (targetPiece.color != gameState[startRow][startCol]?.color) {
                        moves.add(Pair(newRow, newCol)) // Capture
                    }
                    break
                }
            }
        }
        return moves
    }

    fun knightFront(
        coordinates: Pair<Int, Int>,
        isWhite: Boolean,
        isPawn: Boolean
    ): List<Pair<Int, Int>> {
        val gameState = _gameState.value.state
        val (startRow, startCol) = coordinates
        val moves = mutableListOf<Pair<Int, Int>>()

        val direction = if (isWhite) -1 else 1
        val knightMoves = listOf(
            Pair(2 * direction, 1), Pair(2 * direction, -1)
        )
        for (move in knightMoves) {
            val newRow = startRow + move.first
            val newCol = startCol + move.second
            if (isValidPosition(newRow, newCol)) {
                val targetPiece = gameState[newRow][newCol]
                if (isPawn) {
                    if (targetPiece != null && targetPiece.color != gameState[startRow][startCol]?.color) {
                        moves.add(Pair(newRow, newCol))
                    }
                } else {
                    if (targetPiece == null || targetPiece.color != gameState[startRow][startCol]?.color) {
                        moves.add(Pair(newRow, newCol))
                    }
                }
            }
        }
        return moves
    }

    fun knightUpMid(
        coordinates: Pair<Int, Int>,
        isWhite: Boolean,
        isPawn: Boolean
    ): List<Pair<Int, Int>> {
        val gameState = _gameState.value.state
        val (startRow, startCol) = coordinates
        val moves = mutableListOf<Pair<Int, Int>>()

        val direction = if (isWhite) -1 else 1
        val knightMoves = listOf(
            Pair(direction, 2), Pair(direction, -2)
        )
        for (move in knightMoves) {
            val newRow = startRow + move.first
            val newCol = startCol + move.second
            if (isValidPosition(newRow, newCol)) {
                val targetPiece = gameState[newRow][newCol]
                if (isPawn) {
                    if (targetPiece != null && targetPiece.color != gameState[startRow][startCol]?.color) {
                        moves.add(Pair(newRow, newCol))
                    }
                } else {
                    if (targetPiece == null || targetPiece.color != gameState[startRow][startCol]?.color) {
                        moves.add(Pair(newRow, newCol))
                    }
                }
            }
        }
        return moves
    }

    fun knightDownMid(coordinates: Pair<Int, Int>, isWhite: Boolean): List<Pair<Int, Int>> {
        val gameState = _gameState.value.state
        val (startRow, startCol) = coordinates
        val moves = mutableListOf<Pair<Int, Int>>()

        val direction = if (isWhite) -1 else 1
        val knightMoves = listOf(
            Pair(-1 * direction, 2), Pair(-1 * direction, -2)
        )
        for (move in knightMoves) {
            val newRow = startRow + move.first
            val newCol = startCol + move.second
            if (isValidPosition(newRow, newCol)) {
                val targetPiece = gameState[newRow][newCol]
                if (targetPiece == null || targetPiece.color != gameState[startRow][startCol]?.color) {
                    moves.add(Pair(newRow, newCol))
                }
            }
        }
        return moves
    }

    fun knightBack(coordinates: Pair<Int, Int>, isWhite: Boolean): List<Pair<Int, Int>> {
        val gameState = _gameState.value.state
        val (startRow, startCol) = coordinates
        val moves = mutableListOf<Pair<Int, Int>>()

        val direction = if (isWhite) -1 else 1
        val knightMoves = listOf(
            Pair(-2 * direction, 1), Pair(-2 * direction, -1)
        )
        for (move in knightMoves) {
            val newRow = startRow + move.first
            val newCol = startCol + move.second
            if (isValidPosition(newRow, newCol)) {
                val targetPiece = gameState[newRow][newCol]
                if (targetPiece == null || targetPiece.color != gameState[startRow][startCol]?.color) {
                    moves.add(Pair(newRow, newCol))
                }
            }
        }
        return moves
    }

    fun isValidPosition(row: Int, col: Int): Boolean {
        return row in 0..7 && col in 0..7
    }

}