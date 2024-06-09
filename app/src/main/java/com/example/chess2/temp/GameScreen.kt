package com.example.chess2.temp

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.TypedArrayUtils.getResourceId
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chess2.R
import com.example.chess2.auth.google.UserData
import com.example.chess2.game.GameStateViewModel
import com.example.chess2.game.figures.Figure
import com.example.chess2.game.figures.FigureName
import com.example.chess2.game.figures.PlayerColor
import com.example.chess2.ui.theme.Chess2Theme
import com.example.chess2.user.UserQueue
import com.google.firebase.auth.FirebaseAuth
import java.text.FieldPosition
import java.util.Locale


@Composable
fun GameScreen(
    gameStateViewModel: GameStateViewModel = viewModel()
) {

    val gameState by gameStateViewModel.gameState.collectAsState()
    val highlightedSquares by gameStateViewModel.highlightState.collectAsState()

    val whitePlayerTime by gameStateViewModel.whitePlayerTime.collectAsState()
    val blackPlayerTime by gameStateViewModel.blackPlayerTime.collectAsState()
    val isWhitePlayerTurn by gameStateViewModel.whiteMove.collectAsState()

    val currentUserId = FirebaseAuth.getInstance().uid
    val whitePlayer = remember { gameStateViewModel.getWhitePlayer() }
    val isWhitePlayer = currentUserId == whitePlayer.userId

    val currentUserMoveText = if (isWhitePlayerTurn == isWhitePlayer) "Ваш ход" else ""
    val opponentMoveText = if (isWhitePlayerTurn == isWhitePlayer) "" else "Ход противника"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFD9E8D9)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PlayerTimer(timeInSeconds = if (isWhitePlayer) blackPlayerTime else whitePlayerTime)
            Text(
                text = opponentMoveText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }

        Chessboard(
            gameState = gameState.state,
            highlightState = highlightedSquares.state,
            currentPlayerColor =
            if (currentUserId == whitePlayer.userId)
                PlayerColor.WHITE
            else
                PlayerColor.BLACK,
            onPieceSelected = { coordinates -> gameStateViewModel.selectChessPiece(coordinates) }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = currentUserMoveText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            PlayerTimer(timeInSeconds = if (isWhitePlayer) whitePlayerTime else blackPlayerTime)
        }
    }
}

@Composable
fun Chessboard(
    gameState: List<List<Figure?>>,
    highlightState: List<Pair<Int, Int>>,
    currentPlayerColor: PlayerColor,
    onPieceSelected: (Pair<Int, Int>) -> Unit
) {
    Column {
        val rows =
            if (currentPlayerColor == PlayerColor.WHITE) gameState.indices else gameState.indices.reversed()
        for (row in rows) {
            Row {
                val cols =
                    if (currentPlayerColor == PlayerColor.WHITE) gameState[row].indices else gameState[row].indices.reversed()
                for (col in cols) {
                    val figure = gameState[row][col]
                    val isHighlighted = highlightState.contains(Pair(row, col))
                    ChessSquare(
                        figure = figure,
                        position = Pair(row, col),
                        isHighlighted = isHighlighted,
                        onClick = {
                            onPieceSelected(Pair(row, col))
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
    position: Pair<Int, Int>,
    isHighlighted: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    val drawableId = when (figure) {
        is Figure -> getDrawableResourceId(
            context,
            "${figure.color.name.toLowerCase()}_${figure.name.name.toLowerCase()}"
        )

        else -> R.drawable.empty
    }

    val squareColor = if (isHighlighted) {
        Color.Green
    } else if (((position.first ?: 0) + (position.second ?: 0)) % 2 == 0) {
        Color.LightGray
    } else {
        Color.Gray
    }

    val configuration = LocalConfiguration.current
    val cellSide = configuration.screenWidthDp.dp / 8

    Box(
        modifier = Modifier
            .size(cellSide)
            .background(squareColor)
            .border(BorderStroke(if (isHighlighted) 1.dp else 0.dp, Color.Gray))
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

@Composable
fun PlayerTimer(timeInSeconds: Int) {
    val minutes = timeInSeconds / 60
    val seconds = timeInSeconds % 60

    Text(
        text = String.format("%02d:%02d", minutes, seconds),
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Black
    )
}