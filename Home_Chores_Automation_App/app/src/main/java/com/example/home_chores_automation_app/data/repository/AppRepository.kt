package com.example.home_chores_automation_app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.home_chores_automation_app.data.model.User
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AppRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("home_chores_data", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        @Volatile
        private var INSTANCE: AppRepository? = null

        fun getInstance(context: Context): AppRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppRepository(context.applicationContext).also { INSTANCE = it }
            }
    }

    private val userListType = object : TypeToken<MutableList<User>>() {}.type

    private fun getString(key: String): String? = prefs.getString(key, null)
    private fun putString(key: String, value: String) = prefs.edit().putString(key, value).apply()

    // ── USERS ────────────────────────────────────────────────────────────────

    private fun loadUsers(): MutableList<User> {
        val json = getString("users") ?: return mutableListOf()
        return gson.fromJson(json, userListType)
    }

    private fun saveUsers(list: MutableList<User>) = putString("users", gson.toJson(list))

    fun createUser(user: User) {
        val list = loadUsers()
        list.add(user)
        saveUsers(list)
    }

    fun findUserByEmail(email: String): User? =
        loadUsers().find { it.email.equals(email, ignoreCase = true) }

    fun findUserById(id: String): User? =
        loadUsers().find { it.id == id }

    fun getAllUsers(): List<User> = loadUsers()

    fun updateUser(user: User) {
        val list = loadUsers()
        val idx = list.indexOfFirst { it.id == user.id }
        if (idx >= 0) { list[idx] = user; saveUsers(list) }
    }
}