package com.example.chess2.game

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess2.game.classes.GameFB
import com.example.chess2.game.classes.GameState
import com.example.chess2.game.classes.ValidMoves
import com.example.chess2.game.figures.Figure
import com.example.chess2.game.figures.FigureName
import com.example.chess2.game.figures.PlayerColor
import com.example.chess2.user.UserQueue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


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

    private val _whitePlayerTime = MutableStateFlow(600) // Example: 10 minutes in seconds
    val whitePlayerTime: StateFlow<Int> = _whitePlayerTime

    private val _blackPlayerTime = MutableStateFlow(600) // Example: 10 minutes in seconds
    val blackPlayerTime: StateFlow<Int> = _blackPlayerTime

    var initDone: Boolean = false
    val db = FirebaseFirestore.getInstance()
    private var gameStateListener: ListenerRegistration? = null
    private lateinit var users: Pair<UserQueue, UserQueue>

    private var selectedPiecePosition: Pair<Int, Int>? = null

    private val _whiteMove = MutableStateFlow(true) // Example turn state
    val whiteMove: StateFlow<Boolean> = _whiteMove

    private var lastMove: Pair<Pair<Int, Int>, Pair<Int, Int>>? = null

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

        startTimers()

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
                val validMoves = calculateValidMoves(null)
                _highlightState.value = ValidMoves(validMoves)
            } else moveChessPiece(row, col)
        } else if (figure != null && figure.color == if (FirebaseAuth.getInstance().uid == wPlayer.userId) PlayerColor.WHITE else PlayerColor.BLACK) {
            selectedPiecePosition = Pair(figure.row, figure.col)
            val validMoves = calculateValidMoves(null)
            _highlightState.value = ValidMoves(validMoves)
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

                Log.d("M_ISPLAYERTURN", isPlayerTurn().toString())
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
        return Pair(fromRow, fromCol) != Pair(toRow, toCol) && _highlightState.value.state.contains(
            Pair(toRow, toCol)
        )
    }

    private fun updateGameState(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int) {
        val currentState = _gameState.value.state.map { it.toMutableList() }
            .toMutableList() // Create a deep copy of the state
        val pieceToMove = currentState[fromRow][fromCol]
        pieceToMove?.row = toRow
        pieceToMove?.col = toCol
        pieceToMove?.firstMove = false
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
        _whiteMove.value = !_whiteMove.value
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
                }
            } else {
                println("Current data: null")
            }
        }
    }

    private fun isPlayerTurn(): Boolean {
        val currentUser = FirebaseAuth.getInstance().uid!!
        return (_whiteMove.value && wPlayer.userId == currentUser) || (!_whiteMove.value && bPlayer.userId == currentUser)
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

    fun calculateValidMoves(figureCoordinates: Pair<Int, Int>?): MutableList<Pair<Int, Int>> {

        var selectedPiece: Figure? = null
        if (figureCoordinates != null)
            selectedPiece =
                gameState.value.state[figureCoordinates!!.first][figureCoordinates!!.second]
        else
            selectedPiece =
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
                validMoves.addAll(enPassantMoves(coordinates, lastMove, isWhite))
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
                //validMoves.addAll(castlingMoves(isWhite))
            }

            FigureName.QUEEN -> {
                validMoves.addAll(straightLineMove(coordinates, 7))
                validMoves.addAll(diagonalLineMove(coordinates, 7))
            }

            FigureName.PAWN_ROOK -> {
                validMoves.addAll(pawnFirstMove(coordinates, 3, isWhite))
                validMoves.addAll(pawnBaseMove(coordinates, 2, isWhite))
                validMoves.addAll(pawnTake(coordinates, 1, isWhite))
            }

            FigureName.PAWN_KNIGHT -> {
                validMoves.addAll(pawnFirstMove(coordinates, 2, isWhite))
                validMoves.addAll(pawnBaseMove(coordinates, 1, isWhite))
                validMoves.addAll(pawnTake(coordinates, 1, isWhite))
                validMoves.addAll(knightFront(coordinates, isWhite, true))
            }

            FigureName.PAWN_BISHOP -> {
                validMoves.addAll(pawnFirstMove(coordinates, 2, isWhite))
                validMoves.addAll(pawnBaseMove(coordinates, 1, isWhite))
                validMoves.addAll(pawnTake(coordinates, 2, isWhite))
            }

            FigureName.ROOK_PAWN -> {
                validMoves.addAll(straightLineMove(coordinates, 3))
                validMoves.addAll(pawnTake(coordinates, 1, isWhite))
            }

            FigureName.ROOK_KNIGHT -> {
                validMoves.addAll(straightLineMove(coordinates, 2))
                validMoves.addAll(knightFront(coordinates, isWhite, false))
                validMoves.addAll(knightUpMid(coordinates, isWhite, false))
                validMoves.addAll(knightDownMid(coordinates, isWhite))
                validMoves.addAll(knightBack(coordinates, isWhite))
            }

            FigureName.ROOK_BISHOP -> {
                validMoves.addAll(straightLineMove(coordinates, 2))
                validMoves.addAll(diagonalLineMove(coordinates, 1))
            }

            FigureName.KNIGHT_PAWN -> {
                validMoves.addAll(pawnFirstMove(coordinates, 2, isWhite))
                validMoves.addAll(pawnBaseMove(coordinates, 1, isWhite))
                validMoves.addAll(pawnTake(coordinates, 1, isWhite))
                validMoves.addAll(knightFront(coordinates, isWhite, true))
                validMoves.addAll(knightUpMid(coordinates, isWhite, true))
                validMoves.addAll(knightDownMid(coordinates, isWhite))
                validMoves.addAll(knightBack(coordinates, isWhite))
            }

            FigureName.KNIGHT_ROOK -> {
                validMoves.addAll(knightFront(coordinates, isWhite, false))
                validMoves.addAll(knightBack(coordinates, isWhite))
                validMoves.addAll(straightLineMove(coordinates, 1))
            }

            FigureName.KNIGHT_BISHOP -> {
                validMoves.addAll(knightFront(coordinates, isWhite, false))
                validMoves.addAll(knightBack(coordinates, isWhite))
                validMoves.addAll(diagonalLineMove(coordinates, 1))
            }

            FigureName.BISHOP_PAWN -> {
                validMoves.addAll(diagonalLineMove(coordinates, 3))
                validMoves.addAll(pawnFirstMove(coordinates, 2, isWhite))
                validMoves.addAll(pawnBaseMove(coordinates, 1, isWhite))
            }

            FigureName.BISHOP_ROOK -> {
                validMoves.addAll(diagonalLineMove(coordinates, 2))
                validMoves.addAll(straightLineMove(coordinates, 1))
            }

            FigureName.BISHOP_KNIGHT -> {
                validMoves.addAll(diagonalLineMove(coordinates, 2))
                validMoves.addAll(knightFront(coordinates, isWhite, false))
                validMoves.addAll(knightUpMid(coordinates, isWhite, false))
                validMoves.addAll(knightDownMid(coordinates, isWhite))
                validMoves.addAll(knightBack(coordinates, isWhite))
            }

            else -> {}
        }

        return validMoves

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
        val directions = listOf(
            Pair(direction, 1),
            Pair(direction, -1)
        )
        for (dir in directions) {
            for (i in 1..length) {
                val newRow = startRow + i * dir.first
                val newCol = startCol + i * dir.second
                if (!isValidPosition(newRow, newCol)) break
                val targetPiece = gameState[newRow][newCol]
                if (targetPiece == null) {
                    //moves.add(Pair(newRow, newCol))
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

    fun enPassantMoves(
        coordinates: Pair<Int, Int>,
        lastMove: Pair<Pair<Int, Int>, Pair<Int, Int>>?,
        isWhite: Boolean
    ): List<Pair<Int, Int>> {
        val gameState = _gameState.value.state
        val (startRow, startCol) = coordinates
        val moves = mutableListOf<Pair<Int, Int>>()

        if (lastMove == null) return moves

        val (from, to) = lastMove
        val direction = if (isWhite) -1 else 1
        val enPassantRow = if (isWhite) 3 else 4

        if (startRow == enPassantRow) {
            val potentialEnPassantCols = listOf(startCol - 1, startCol + 1)
            for (col in potentialEnPassantCols) {
                if (isValidPosition(
                        startRow,
                        col
                    ) && gameState[startRow][col]?.name == FigureName.PAWN && gameState[startRow][col]?.color != (if (isWhite) PlayerColor.WHITE else PlayerColor.BLACK)
                ) {
                    if (to == Pair(enPassantRow, col) && from == Pair(
                            enPassantRow + direction * 2,
                            col
                        )
                    ) {
                        moves.add(Pair(startRow + direction, col))
                    }
                }
            }
        }

        return moves
    }

//    fun castlingMoves(isWhite: Boolean): List<Pair<Int, Int>> {
//        val gameState = _gameState.value.state
//        val row = if (isWhite) 7 else 0
//        val moves = mutableListOf<Pair<Int, Int>>()
//
//        val kingFigure = gameState[row][4]
//
//        if (kingFigure != null && kingFigure.firstMove) {
//            val rookKing = gameState[row][7]
//            val rookQueen = gameState[row][0]
//
//            if (rookKing != null && rookKing.name == FigureName.ROOK && rookKing.firstMove) {
//                var valid = true
//                for (col in 5..6) {
//                    if (gameState[row][col] == null && !isSquareAttacked(Pair(row, col), gameState, isWhite)) {
//                        valid = true
//                    } else {
//                        valid = false
//                        break
//                    }
//                }
//                if (isSquareAttacked(Pair(kingFigure.row, kingFigure.col), gameState, isWhite)) {
//                    valid = false
//                }
//                if (isSquareAttacked(Pair(rookKing.row, rookKing.col), gameState, isWhite)) {
//                    valid = false
//                }
//                if (valid) {
//                    moves.add(Pair(row, 6))
//                }
//            }
//
//            if (rookQueen != null && rookQueen.name == FigureName.ROOK && rookQueen.firstMove) {
//                var valid = true
//                for (col in 1..3) {
//                    if (gameState[row][col] == null && !isSquareAttacked(Pair(row, col), gameState, isWhite)) {
//                        valid = true
//                    } else {
//                        valid = false
//                        break
//                    }
//                }
//                if (isSquareAttacked(Pair(kingFigure.row, kingFigure.col), gameState, isWhite)) {
//                    valid = false
//                }
//                if (isSquareAttacked(Pair(rookQueen.row, rookQueen.col), gameState, isWhite)) {
//                    valid = false
//                }
//                if (valid) {
//                    moves.add(Pair(row, 2))
//                }
//            }
//        }
//
//        return moves
//    }

//    fun castlingMoves(
//        isWhite: Boolean
//    ): List<Pair<Int, Int>> {
//        val gameState = _gameState.value.state
//        val moves = mutableListOf<Pair<Int, Int>>()
//
//        val row = if (isWhite) 7 else 0
//        val kingFigure = gameState[row][4]
//
//        if (kingFigure == null || !kingFigure.firstMove) return moves
//
//        val castlingConditions = listOf(
//            Pair(
//                Pair(row, 0),
//                listOf(Pair(row, 1), Pair(row, 2), Pair(row, 3))
//            ), // Queen side castling
//            Pair(
//                Pair(row, 7),
//                listOf(Pair(row, 5), Pair(row, 6))
//            ) // King side castling
//        )
//
//        for ((rookPos, positionsBetween) in castlingConditions) {
//            val (rookRow, rookCol) = rookPos
//
//            if (!rookMoved[rookCol] && gameState[rookRow][rookCol]?.name == FigureName.ROOK && gameState[rookRow][rookCol]?.color == (if (isWhite) PlayerColor.WHITE else PlayerColor.BLACK)) {
//                if (positionsBetween.all { gameState[it.first][it.second] == null } && positionsBetween.none {
//                        isSquareAttacked(
//                            it,
//                            gameState,
//                            isWhite
//                        )
//                    }) {
//                    moves.add(Pair(startRow, if (rookCol == 0) startCol - 2 else startCol + 2))
//                }
//            }
//        }
//
//        return moves
//    }

//    fun castlingMoves(kingCoordinates: Pair<Int, Int>, castlingRights: BooleanArray): List<Pair<Int, Int>> {
//        val gameState = _gameState.value.state
//        val (kingRow, kingCol) = kingCoordinates
//        val moves = mutableListOf<Pair<Int, Int>>()
//        val currentPiece = gameState[kingRow][kingCol] ?: return moves
//        val isWhite = currentPiece.color == PlayerColor.WHITE
//
//        // King side castling (short castling)
//        if (castlingRights[0] && canCastleKingSide(kingCoordinates, gameState, isWhite)) {
//            moves.add(Pair(kingRow, kingCol + 2))
//        }
//
//        // Queen side castling (long castling)
//        if (castlingRights[1] && canCastleQueenSide(kingCoordinates, gameState, isWhite)) {
//            moves.add(Pair(kingRow, kingCol - 2))
//        }
//
//        return moves
//    }
//
//    fun canCastleKingSide(kingCoordinates: Pair<Int, Int>, gameState: List<List<Figure?>>, isWhite: Boolean): Boolean {
//        val (kingRow, kingCol) = kingCoordinates
//        // Check the squares between the king and the rook
//        if (gameState[kingRow][kingCol + 1] != null || gameState[kingRow][kingCol + 2] != null) return false
//
//        // Check if any of the squares the king moves through or lands on are under attack
//        val positionsToCheck = listOf(Pair(kingRow, kingCol), Pair(kingRow, kingCol + 1), Pair(kingRow, kingCol + 2))
//        if (positionsToCheck.any { isSquareAttacked(it, gameState, isWhite) }) return false
//
//        // Check the rook
//        val rook = gameState[kingRow][kingCol + 3]
//        if (rook == null || rook.name != FigureName.ROOK || rook.color != currentPiece.color) return false
//
//        return true
//    }
//
//    fun canCastleQueenSide(kingCoordinates: Pair<Int, Int>, gameState: List<List<Figure?>>, isWhite: Boolean): Boolean {
//        val (kingRow, kingCol) = kingCoordinates
//        // Check the squares between the king and the rook
//        if (gameState[kingRow][kingCol - 1] != null || gameState[kingRow][kingCol - 2] != null || gameState[kingRow][kingCol - 3] != null) return false
//
//        // Check if any of the squares the king moves through or lands on are under attack
//        val positionsToCheck = listOf(Pair(kingRow, kingCol), Pair(kingRow, kingCol - 1), Pair(kingRow, kingCol - 2))
//        if (positionsToCheck.any { isSquareAttacked(it, gameState, isWhite) }) return false
//
//        // Check the rook
//        val rook = gameState[kingRow][kingCol - 4]
//        if (rook == null || rook.type != PieceType.ROOK || rook.color != currentPiece.color) return false
//
//        return true
//    }

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

    fun isMoveSafe(start: Pair<Int, Int>, end: Pair<Int, Int>): Boolean {
        val gameState = _gameState.value.state
        // Create a deep copy of the game state
        val tempGameState = gameState.map { it.toMutableList() }.toMutableList()

        // Simulate the move
        val piece = tempGameState[start.first][start.second]
        tempGameState[start.first][start.second] = null
        tempGameState[end.first][end.second] = piece

        // Find the king's position
        val kingPosition = tempGameState.flatten()
            .firstOrNull { it?.name == FigureName.KING && it?.color == piece?.color }
            ?.let { Pair(it.row, it.col) }

        return kingPosition != null && !isSquareAttacked(
            kingPosition,
            tempGameState,
            piece?.color == PlayerColor.WHITE
        )
    }

    fun isSquareAttacked(
        square: Pair<Int, Int>,
        gameState: List<List<Figure?>>,
        isWhite: Boolean
    ): Boolean {

        val (targetRow, targetCol) = square
        val opponentColor = if (isWhite) PlayerColor.BLACK else PlayerColor.WHITE

        for (row in gameState.indices) {
            for (col in gameState[row].indices) {
                val piece = gameState[row][col]
                if (piece != null && piece.color == opponentColor) {
                    val possibleMoves = calculateValidMoves(Pair(row, col))
                    if (possibleMoves.contains(square)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    // Check if the king is in check in the given game state
    fun isKingInCheck(gameState: List<List<Figure?>>, isWhite: Boolean): Boolean {
        val kingPosition = findKingPosition(gameState, isWhite) ?: return true
        return isSquareAttacked(kingPosition, gameState, isWhite)
    }

    // Find the position of the king
    fun findKingPosition(gameState: List<List<Figure?>>, isWhite: Boolean): Pair<Int, Int>? {
        for (row in gameState.indices) {
            for (col in gameState[row].indices) {
                val piece = gameState[row][col]
                if (piece != null && piece.name == FigureName.KING && piece.color == if (isWhite) PlayerColor.WHITE else PlayerColor.BLACK) {
                    return Pair(row, col)
                }
            }
        }
        return null
    }

    fun moveRookForCastling(kingCoordinates: Pair<Int, Int>, targetCoordinates: Pair<Int, Int>, gameState: MutableList<MutableList<Figure?>>) {
        val (kingRow, kingCol) = kingCoordinates
        val (targetRow, targetCol) = targetCoordinates
        if (targetCol == kingCol + 2) {
            // King side castling
            val rook = gameState[kingRow][kingCol + 3]
            gameState[kingRow][kingCol + 3] = null
            gameState[kingRow][kingCol + 1] = rook
        } else if (targetCol == kingCol - 2) {
            // Queen side castling
            val rook = gameState[kingRow][kingCol - 4]
            gameState[kingRow][kingCol - 4] = null
            gameState[kingRow][kingCol - 1] = rook
        }
    }

    private fun startTimers() {
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                if (_whiteMove.value) {
                    _whitePlayerTime.value -= 1
                    if (_whitePlayerTime.value <= 0) {
                        // Handle white player's time expiry
                    }
                } else {
                    _blackPlayerTime.value -= 1
                    if (_blackPlayerTime.value <= 0) {
                        // Handle black player's time expiry
                    }
                }
            }
        }
    }
}