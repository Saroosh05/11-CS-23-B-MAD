package com.example.home_chores_automation_app.data.prefs

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

class SessionManager(context: Context) {

    private val auth = FirebaseAuth.getInstance()

    fun logout() = auth.signOut()

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun isLoggedIn(): Boolean = auth.currentUser != null
}
