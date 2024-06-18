package com.example.chess2.user

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess2.auth.google.SignInResult
import com.example.chess2.auth.google.SignInState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.auth.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserViewModel : ViewModel() {

    private val _state = MutableStateFlow(MatchingState())
    val state = _state.asStateFlow()

    private lateinit var user: UserQueue
    private val db = FirebaseFirestore.getInstance()
    private var searchListener: ListenerRegistration? = null

    fun initUser() {
        user = UserQueue(
            FirebaseAuth.getInstance().uid!!,
            false,
            GameMode.CLASSIC
        )
    }

    fun getSearchStatus(): Boolean {
        return user.searching
    }

    fun startSearching() {
        user.searching = true
        userDetails().set(user)
        Log.d("USERDETAILS", userDetails().get().toString())
        findMatchingUsers()
    }

    fun stopSearching() {
        user.searching = false
        userDetails().set(user)
        searchListener?.remove()
    }

    private fun userDetails(): DocumentReference {
        return db.collection("queue").document(user.userId)
    }

    private fun findMatchingUsers() {
        searchListener?.remove()

        searchListener = db.collection("queue")
            .whereEqualTo("searching", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("Error finding matching users: $error")
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val matchingUsers = mutableListOf<UserQueue>()
                    for (document in snapshot.documents) {
                        val userId = document.getString("userId")
                        val gameMode = document.getString("gameMode")
                        if (userId != user.userId && gameMode == user.gameMode.toString()) {
                            val searching = false
                            val gameMode =
                                if
                                    (document.getString("gameMode") == null) null
                                else
                                    GameMode.valueOf(document.getString("gameMode").toString())
                            matchingUsers.add(UserQueue(userId!!, searching, gameMode))
                        }
                    }

                    if (matchingUsers.isNotEmpty()) {
                        val matchedUser = matchingUsers.random()
                        user.searching = false
                        updateUsersInGame(user, matchedUser)
                        onMatchingResult(listOf(user, matchedUser))
                    }
                }
            }
    }

    private fun updateUsersInGame(user1: UserQueue, user2: UserQueue) {
        db.collection("queue").document(user1.userId).delete()
        db.collection("queue").document(user2.userId).delete()
    }

    fun onMatchingResult(result: List<UserQueue>?) {
        _state.update {
            it.copy(
                isMatchingSuccessful = result != null,
                matchingError = null,
                users = result
            )
        }
    }

    fun resetState() {
        _state.update { MatchingState() }
    }

    // Clean up resources when ViewModel is no longer needed
    public override fun onCleared() {
        super.onCleared()
        searchListener?.remove()
    }

    fun changeGameMode(gameMode: String) {
        user.gameMode = when (gameMode) {
            "Классика" -> GameMode.CLASSIC
            "Рандом" -> GameMode.RANDOM
            "Шахматы 2.0" -> GameMode.CHESS2
            else -> null
        }
    }

    fun getUser(): UserQueue {
        return user
    }
}