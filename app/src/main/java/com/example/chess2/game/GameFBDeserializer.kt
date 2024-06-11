package com.example.chess2.game

import com.example.chess2.game.classes.GameFB
import com.example.chess2.game.figures.Figure
import com.example.chess2.game.figures.FigureName
import com.example.chess2.game.figures.PlayerColor
import com.example.chess2.user.GameMode
import com.example.chess2.user.UserQueue
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException

class GameFBDeserializer {

    @Throws(FirebaseFirestoreException::class)
    fun deserialize(snapshot: DocumentSnapshot): GameFB {
        val gameId = snapshot.id
        val gameState = parseGameState(snapshot)

        val wPlayerData = snapshot.get("wplayer") as? Map<*, *>
        val bPlayerData = snapshot.get("bplayer") as? Map<*, *>

        val wPlayer = wPlayerData?.let { parseUserQueue(it) }
        val bPlayer = bPlayerData?.let { parseUserQueue(it) }

        return GameFB(gameId, gameState, wPlayer, bPlayer)
    }

    private fun parseUserQueue(data: Map<*, *>): UserQueue {
        val userId = data["userId"] as String
        val searching = data["searching"] as Boolean
        val gameMode =
            if (data["gameMode"] != null)
                GameMode.valueOf(data["gameMode"] as String)
            else
                null
        return UserQueue(userId, searching, gameMode)
    }

    private fun parseGameState(snapshot: DocumentSnapshot): MutableList<Figure> {
        val gameStateList = mutableListOf<Figure>()
        val gameStateData = snapshot.get("gameState") as? List<Map<*, *>>

        gameStateData?.forEach { figureData ->
            val row = figureData["row"] as Long
            val col = figureData["col"] as Long
            val color = PlayerColor.valueOf(figureData["color"] as String)
            val name = FigureName.valueOf(figureData["name"] as String)
            val firstMove = figureData["firstMove"] as Boolean
            gameStateList.add(Figure(row.toInt(), col.toInt(), color, name, firstMove))
        }

        return gameStateList
    }
}