package com.example.chess2.user

import androidx.lifecycle.ViewModel
import com.example.chess2.auth.google.SignInResult
import com.example.chess2.auth.google.SignInState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await

class UserViewModel : ViewModel() {

    private val _state = MutableStateFlow(MatchingState())
    val state = _state.asStateFlow()

    private lateinit var user: User
    val db = FirebaseFirestore.getInstance()

    fun initUser() {
        user = User(
            FirebaseAuth.getInstance().uid!!,
            false,
            false
        )
    }

    fun getSearchStatus(): Boolean {
        return user.searching
    }

    fun startSearching() {
        user.searching = true;

        userDetails().set(user)
    }

    fun stopSearching() {
        user.searching = false;

        userDetails().delete()
    }

    private fun userDetails(): DocumentReference {
        return db.collection("users").document(user.userId)
    }

    suspend fun findMatchingUsers(): List<User>? {
        val matchingUsers = mutableListOf<User>()
        try {
            val documents = db.collection("users")
                .whereEqualTo("searching", true)
                .get()
                .await()

            for (document in documents) {
                val userId = document.getString("userId")
                if (userId != user.userId) {
                    val searching = document.getBoolean("searching") ?: false
                    val inGame = document.getBoolean("inGame") ?: false
                    matchingUsers.add(User(userId!!, searching, inGame))
                }
            }

            if (matchingUsers.isNotEmpty()) {
                val matchedUser = matchingUsers.random()
                updateUsersInGame(user, matchedUser)
                return listOf(user, matchedUser)
            }
        } catch (e: Exception) {
            println("Error finding matching users: $e")
        }
        return null
    }

    private fun updateUsersInGame(user1: User, user2: User) {
        db.collection("users").document(user1.userId)
            .delete()

        db.collection("users").document(user2.userId)
            .delete()
    }

    fun onMatchingResult(result: List<User>?) {
        _state.update { it.copy(
            isMatchingSuccessful = result!=null,
            matchingError = null,
            users = result
        ) }
    }

    fun resetState() {
        _state.update { MatchingState() }
    }
}