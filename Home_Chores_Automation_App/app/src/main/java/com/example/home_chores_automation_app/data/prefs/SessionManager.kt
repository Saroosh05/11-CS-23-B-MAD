package com.example.home_chores_automation_app.data.prefs

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

class SessionManager(context: Context) {

    private val auth  = FirebaseAuth.getInstance()
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    fun logout() = auth.signOut()

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun isLoggedIn(): Boolean = auth.currentUser != null

    // ── Dark mode ─────────────────────────────────────────────────────────────
    fun isDarkMode(): Boolean  = prefs.getBoolean("dark_mode", false)
    fun setDarkMode(on: Boolean) = prefs.edit().putBoolean("dark_mode", on).apply()
}
