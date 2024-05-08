package com.example.chess2.user

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

//class UserViewModel : ViewModel() {
//
//    private val _state = MutableStateFlow(MatchingState())
//    val state = _state.asStateFlow()
//
//    private lateinit var user: UserQueue
//    val db = FirebaseFirestore.getInstance()
//
//    private var searchListener: ListenerRegistration? = null
//
//    fun initUser() {
//        user = UserQueue(
//            FirebaseAuth.getInstance().uid!!,
//            false,
//            false
//        )
//    }
//
//    fun getSearchStatus(): Boolean {
//        return user.searching
//    }
//
//    fun startSearching() {
//        user.searching = true;
//
//        userDetails().set(user)
//    }
//
//    fun stopSearching() {
//        user.searching = false;
//
//        userDetails().delete()
//    }
//
//    private fun userDetails(): DocumentReference {
//        return db.collection("queue").document(user.userId)
//    }
//
//    suspend fun findMatchingUsers(): List<UserQueue>? {
//        val matchingUsers = mutableListOf<UserQueue>()
//
//        try {
//            val documents = db.collection("queue")
//                .whereEqualTo("searching", true)
//                .get()
//                .await()
//
//            for (document in documents) {
//                val userId = document.getString("userId")
//                if (userId != user.userId) {
//                    val searching = document.getBoolean("searching") ?: false
//                    val inGame = document.getBoolean("inGame") ?: false
//                    matchingUsers.add(UserQueue(userId!!, searching, inGame))
//                }
//            }
//
//            if (matchingUsers.isNotEmpty()) {
//                val matchedUser = matchingUsers.random()
//                updateUsersInGame(user, matchedUser)
//                return listOf(user, matchedUser)
//            }
//        } catch (e: Exception) {
//            println("Error finding matching users: $e")
//        }
//        return null
//    }
//
//    private fun updateUsersInGame(user1: UserQueue, user2: UserQueue) {
//        db.collection("queue").document(user1.userId)
//            .delete()
//
//        db.collection("queue").document(user2.userId)
//            .delete()
//    }
//
//    fun onMatchingResult(result: List<UserQueue>?) {
//        _state.update { it.copy(
//            isMatchingSuccessful = result!=null,
//            matchingError = null,
//            users = result
//        ) }
//    }
//
//    fun resetState() {
//        _state.update { MatchingState() }
//    }
//}

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
            false
        )
    }

    fun getSearchStatus(): Boolean {
        return user.searching
    }

    fun startSearching() {
        user.searching = true
        userDetails().set(user)
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
                        if (userId != user.userId) {
                            val searching = document.getBoolean("searching") ?: false
                            val inGame = document.getBoolean("inGame") ?: false
                            matchingUsers.add(UserQueue(userId!!, searching, inGame))
                        }
                    }

                    if (matchingUsers.isNotEmpty()) {
                        val matchedUser = matchingUsers.random()
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
        _state.update { it.copy(
            isMatchingSuccessful = result != null,
            matchingError = null,
            users = result
        ) }
    }

    fun resetState() {
        _state.update { MatchingState() }
    }

    // Clean up resources when ViewModel is no longer needed
    override fun onCleared() {
        super.onCleared()
        searchListener?.remove()
    }
}