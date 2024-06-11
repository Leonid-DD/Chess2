package unit_tests

import com.example.chess2.game.GameStateViewModel
import com.example.chess2.game.classes.GameState
import com.example.chess2.game.figures.Figure
import com.example.chess2.game.figures.FigureName
import com.example.chess2.game.figures.PlayerColor
import com.example.chess2.user.UserQueue
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class UnitTests {

    private lateinit var gameStateViewModel: GameStateViewModel

    @Before
    fun setUp() {
        gameStateViewModel = GameStateViewModel()
        gameStateViewModel.set_gameState(GameState(generateBaseGameState()))
    }

    @Test
    fun calculate_valid_moves() {
        //calculate moves for white pawn

        val expectedResult: List<Pair<Int, Int>> = listOf(Pair(5, 2), Pair(4, 2))

        val actualResult = gameStateViewModel.calculateValidMoves(Pair(6, 2))

        Assert.assertEquals(expectedResult, actualResult)
    }

    @Test
    fun generateGameId_isCorrect() {
        val user1 = UserQueue("user1id", false, null)
        val user2 = UserQueue("user2id", false, null)

        val expectedResult = user1.userId + "_" + user2.userId

        val actualResult = gameStateViewModel.createGameId(user1, user2)

        Assert.assertEquals(expectedResult, actualResult)
    }

    @Test
    fun convertToFB_isCorrect() {
        val expectedResult = generateFbFormatGameState()

        val actualResult = gameStateViewModel.convertToFB(gameStateViewModel.gameState.value.state)

        Assert.assertEquals(expectedResult, actualResult)
    }

    @Test
    fun convertFromFB_isCorrect() {
        val expectedResult = generateBaseGameState()

        val actualResult = gameStateViewModel.convertFromFB(generateFbFormatGameState())

        Assert.assertEquals(expectedResult, actualResult)
    }

    fun generateBaseGameState(): MutableList<MutableList<Figure?>> {
        return MutableList(8) { row ->
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
    }

    fun generateFbFormatGameState(): MutableList<Figure> {
        return MutableList(32) {
            Figure(0, 0, PlayerColor.BLACK, FigureName.ROOK)
            Figure(0, 1, PlayerColor.BLACK, FigureName.KNIGHT)
            Figure(0, 2, PlayerColor.BLACK, FigureName.BISHOP)
            Figure(0, 3, PlayerColor.BLACK, FigureName.QUEEN)
            Figure(0, 4, PlayerColor.BLACK, FigureName.KING)
            Figure(0, 5, PlayerColor.BLACK, FigureName.BISHOP)
            Figure(0, 6, PlayerColor.BLACK, FigureName.KNIGHT)
            Figure(0, 7, PlayerColor.BLACK, FigureName.ROOK)
            Figure(1, 0, PlayerColor.BLACK, FigureName.PAWN)
            Figure(1, 1, PlayerColor.BLACK, FigureName.PAWN)
            Figure(1, 2, PlayerColor.BLACK, FigureName.PAWN)
            Figure(1, 3, PlayerColor.BLACK, FigureName.PAWN)
            Figure(1, 4, PlayerColor.BLACK, FigureName.PAWN)
            Figure(1, 5, PlayerColor.BLACK, FigureName.PAWN)
            Figure(1, 6, PlayerColor.BLACK, FigureName.PAWN)
            Figure(1, 7, PlayerColor.BLACK, FigureName.PAWN)
            Figure(6, 0, PlayerColor.WHITE, FigureName.PAWN)
            Figure(6, 1, PlayerColor.WHITE, FigureName.PAWN)
            Figure(6, 2, PlayerColor.WHITE, FigureName.PAWN)
            Figure(6, 3, PlayerColor.WHITE, FigureName.PAWN)
            Figure(6, 4, PlayerColor.WHITE, FigureName.PAWN)
            Figure(6, 5, PlayerColor.WHITE, FigureName.PAWN)
            Figure(6, 6, PlayerColor.WHITE, FigureName.PAWN)
            Figure(6, 7, PlayerColor.WHITE, FigureName.PAWN)
            Figure(7, 0, PlayerColor.WHITE, FigureName.ROOK)
            Figure(7, 1, PlayerColor.WHITE, FigureName.KNIGHT)
            Figure(7, 2, PlayerColor.WHITE, FigureName.BISHOP)
            Figure(7, 3, PlayerColor.WHITE, FigureName.QUEEN)
            Figure(7, 4, PlayerColor.WHITE, FigureName.KING)
            Figure(7, 5, PlayerColor.WHITE, FigureName.BISHOP)
            Figure(7, 6, PlayerColor.WHITE, FigureName.KNIGHT)
            Figure(7, 7, PlayerColor.WHITE, FigureName.ROOK)
        }
    }
}