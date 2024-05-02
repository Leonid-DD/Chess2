package com.example.chess2.auth.email

import android.text.TextUtils
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.Firebase
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.auth

class EmailAuthUIClient {

    private val auth = Firebase.auth

//    private fun signInWithEmailAndPassword(
//        email: String,
//        password: String
//    ) {
//        auth.signInWithEmailAndPassword(email, password)
//            .addOnCompleteListener(this,
//                OnCompleteListener<AuthResult?> { task ->
//                    if (task.isSuccessful) {
//                        // Sign in success, update UI with the signed-in user's information
//                        // You can add code here to proceed after successful sign-in
//
//                    } else {
//                        // If sign in fails, display a message to the user.
//
//                    }
//                })
//    }
//
//    private fun signUpWithEmailAndPassword(
//        email: String,
//        password: String
//    ) {
//        auth.createUserWithEmailAndPassword(email, password)
//            .addOnCompleteListener(this,
//                OnCompleteListener<AuthResult?> { task ->
//                    if (task.isSuccessful) {
//                        // Sign up success, update UI with the signed-up user's information
//                        // You can add code here to proceed after successful sign-up
//
//                    } else {
//                        // If sign up fails, display a message to the user.
//
//                    }
//                })
//    }

}