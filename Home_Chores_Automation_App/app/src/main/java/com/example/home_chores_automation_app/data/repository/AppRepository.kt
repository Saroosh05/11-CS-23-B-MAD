package com.example.home_chores_automation_app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.home_chores_automation_app.data.model.AppNotification
import com.example.home_chores_automation_app.data.model.Group
import com.example.home_chores_automation_app.data.model.Task
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

    // ── GROUPS ───────────────────────────────────────────────────────────────

    private val groupListType = object : TypeToken<MutableList<Group>>() {}.type

    private fun loadGroups(): MutableList<Group> {
        val json = getString("groups") ?: return mutableListOf()
        return gson.fromJson(json, groupListType)
    }

    private fun saveGroups(list: MutableList<Group>) = putString("groups", gson.toJson(list))

    fun createGroup(group: Group) {
        val list = loadGroups()
        list.add(group)
        saveGroups(list)
    }

    fun getGroupsForUser(userId: String): List<Group> =
        loadGroups().filter { it.adminId == userId || it.memberIds.contains(userId) }

    fun findGroupById(id: String): Group? =
        loadGroups().find { it.id == id }

    fun findGroupByInviteCode(code: String): Group? =
        loadGroups().find { it.inviteCode.equals(code, ignoreCase = true) }

    fun updateGroup(group: Group) {
        val list = loadGroups()
        val idx = list.indexOfFirst { it.id == group.id }
        if (idx >= 0) { list[idx] = group; saveGroups(list) }
    }

    // ── TASKS ────────────────────────────────────────────────────────────────

    private val taskListType = object : TypeToken<MutableList<Task>>() {}.type

    private fun loadTasks(): MutableList<Task> {
        val json = getString("tasks") ?: return mutableListOf()
        return gson.fromJson(json, taskListType)
    }

    private fun saveTasks(list: MutableList<Task>) = putString("tasks", gson.toJson(list))

    fun createTask(task: Task) {
        val list = loadTasks()
        list.add(task)
        saveTasks(list)
    }

    fun getTasksForGroup(groupId: String): List<Task> =
        loadTasks().filter { it.groupId == groupId }

    fun updateTask(task: Task) {
        val list = loadTasks()
        val idx = list.indexOfFirst { it.id == task.id }
        if (idx >= 0) { list[idx] = task; saveTasks(list) }
    }

    // ── NOTIFICATIONS ────────────────────────────────────────────────────────

    private val notifListType = object : TypeToken<MutableList<AppNotification>>() {}.type

    private fun loadNotifications(): MutableList<AppNotification> {
        val json = getString("notifications") ?: return mutableListOf()
        return gson.fromJson(json, notifListType)
    }

    private fun saveNotifications(list: MutableList<AppNotification>) =
        putString("notifications", gson.toJson(list))

    fun addNotification(notification: AppNotification) {
        val list = loadNotifications()
        list.add(0, notification)
        saveNotifications(list)
    }

    fun getNotificationsForUser(userId: String): List<AppNotification> =
        loadNotifications().filter { it.userId == userId }

    fun markAllRead(userId: String) {
        val list = loadNotifications()
        val updated = list.map { if (it.userId == userId) it.copy(isRead = true) else it }.toMutableList()
        saveNotifications(updated)
    }

    fun getUnreadCount(userId: String): Int =
        loadNotifications().count { it.userId == userId && !it.isRead }
}