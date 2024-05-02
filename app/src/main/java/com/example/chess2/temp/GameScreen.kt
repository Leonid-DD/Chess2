package com.example.chess2.temp

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.res.TypedArrayUtils.getResourceId
import com.example.chess2.R
import com.example.chess2.game.figures.Figure
import com.example.chess2.game.figures.FigureName
import com.example.chess2.game.figures.PlayerColor
import com.example.chess2.ui.theme.Chess2Theme
import java.text.FieldPosition
import java.util.Locale

@Composable
fun Chessboard(
    boardState: List<List<Figure?>>,
    selectedPiece: Pair<Int, Int>?,
    //possibleMoves: List<Pair<Int, Int>>,
    currentPlayerColor: PlayerColor,
    onPieceSelected: (Figure?) -> Unit
) {
    Column {
        val rows = if (currentPlayerColor == PlayerColor.WHITE) boardState.indices else boardState.indices.reversed()
        for (row in rows) {
            Row {
                val cols = if (currentPlayerColor == PlayerColor.WHITE) boardState[row].indices else boardState[row].indices.reversed()
                for (col in cols) {
                    val figure = boardState[row][col]
                    //val isHighlighted = selectedPiece != null && (row to col) in possibleMoves
                    ChessSquare(
                        figure = figure,
                        position = Pair(row, col),
                        isSelected = selectedPiece?.first == row && selectedPiece.second == col,
                        //isHighlighted = isHighlighted,
                        onClick = {
                            onPieceSelected(figure)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ChessSquare(
    figure: Figure?,
    position: Pair<Int,Int>,
    isSelected: Boolean,
    //isHighlighted: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    val drawableId = when (figure) {
        is Figure -> getDrawableResourceId(context,"${figure.color.name.toLowerCase()}_${figure.name.name.toLowerCase()}")
        else -> R.drawable.empty
    }

    val squareColor = if (((position.first ?: 0) + (position.second ?: 0)) % 2 == 0) {
        Color.LightGray
    } else {
        Color.Gray
    }

    Box(
        modifier = Modifier
            .size(50.dp)
            .background(squareColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Draw chess piece if present
        if (figure != null) {
            DrawChessPiece(drawableId)
        }
    }
}

@Composable
fun DrawChessPiece(@DrawableRes resourceId: Int) {
    Image(
        painter = painterResource(id = resourceId),
        contentDescription = null,
        modifier = Modifier.size(50.dp)
    )
}

@SuppressLint("DiscouragedApi")
fun getDrawableResourceId(context: Context, resourceName: String): Int {
    return context.resources.getIdentifier(resourceName, "drawable", context.packageName)
}

@Preview
@Composable
fun ChessboardPreview() {
    val boardState = List(8) { row ->
        List(8) { col ->
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

    val selectedPiece = remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val possibleMoves = remember { mutableStateOf<List<Pair<Int, Int>>>(emptyList()) }

    Chessboard(
        boardState,
        selectedPiece.value,
        PlayerColor.WHITE,
        onPieceSelected = { }
    )
}