package com.example.home_chores_automation_app.data.prefs

import android.content.Context

class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences("session_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID = "current_user_id"
    }

    fun login(userId: String) = prefs.edit().putString(KEY_USER_ID, userId).apply()

    fun logout() = prefs.edit().remove(KEY_USER_ID).apply()

    fun getCurrentUserId(): String? = prefs.getString(KEY_USER_ID, null)

    fun isLoggedIn(): Boolean = getCurrentUserId() != null
}
