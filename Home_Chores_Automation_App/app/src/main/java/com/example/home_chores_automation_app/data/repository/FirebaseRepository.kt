package com.example.home_chores_automation_app.data.repository

import com.example.home_chores_automation_app.data.model.AppNotification
import com.example.home_chores_automation_app.data.model.Group
import com.example.home_chores_automation_app.data.model.Task
import com.example.home_chores_automation_app.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.UUID

class FirebaseRepository private constructor() {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    companion object {
        @Volatile
        private var INSTANCE: FirebaseRepository? = null

        fun getInstance(): FirebaseRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseRepository().also { INSTANCE = it }
            }
    }

    // ── USERS ────────────────────────────────────────────────────────────────

    suspend fun createUser(user: User) {
        db.collection("users").document(user.id).set(user).await()
    }

    suspend fun getUserById(id: String): User? {
        return try {
            db.collection("users").document(id).get().await().toObject(User::class.java)
        } catch (e: Exception) { null }
    }

    suspend fun updateUser(user: User) {
        db.collection("users").document(user.id).set(user).await()
    }

    suspend fun updateProfilePictureUrl(userId: String, url: String?) {
        db.collection("users").document(userId)
            .update("profilePictureUrl", url).await()
    }

    // ── GROUPS ───────────────────────────────────────────────────────────────

    suspend fun createGroup(group: Group) {
        db.collection("groups").document(group.id).set(group).await()
    }

    /** Returns groups where userId is in memberIds (admin is always added to memberIds on creation). */
    suspend fun getGroupsForUser(userId: String): List<Group> {
        return try {
            db.collection("groups")
                .whereArrayContains("memberIds", userId)
                .get().await()
                .toObjects(Group::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getGroupById(id: String): Group? {
        return try {
            db.collection("groups").document(id).get().await().toObject(Group::class.java)
        } catch (e: Exception) { null }
    }

    suspend fun getGroupByInviteCode(code: String): Group? {
        return try {
            db.collection("groups")
                .whereEqualTo("inviteCode", code.uppercase())
                .get().await()
                .toObjects(Group::class.java)
                .firstOrNull()
        } catch (e: Exception) { null }
    }

    suspend fun updateGroup(group: Group) {
        db.collection("groups").document(group.id).set(group).await()
    }

    suspend fun deleteGroup(groupId: String) {
        db.collection("groups").document(groupId).delete().await()
    }

    // ── TASKS ────────────────────────────────────────────────────────────────

    suspend fun createTask(task: Task) {
        db.collection("tasks").document(task.id).set(task).await()
    }

    suspend fun getTasksForGroup(groupId: String): List<Task> {
        return try {
            db.collection("tasks")
                .whereEqualTo("groupId", groupId)
                .get().await()
                .toObjects(Task::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun updateTask(task: Task) {
        db.collection("tasks").document(task.id).set(task).await()
    }

    suspend fun markOverdueNotified(taskId: String) {
        db.collection("tasks").document(taskId)
            .update("overdueNotified", true).await()
    }

    suspend fun deleteTask(taskId: String) {
        db.collection("tasks").document(taskId).delete().await()
    }

    suspend fun deleteTasksForGroup(groupId: String) {
        val snapshot = db.collection("tasks")
            .whereEqualTo("groupId", groupId)
            .get().await()
        val batch = db.batch()
        for (doc in snapshot.documents) batch.delete(doc.reference)
        batch.commit().await()
    }

    fun getNextDeadline(task: Task): Long {
        if (task.dueDate == 0L) return 0L
        val cal = Calendar.getInstance()
        cal.timeInMillis = task.dueDate
        when (task.recurrence) {
            "daily"   -> cal.add(Calendar.DAY_OF_MONTH, 1)
            "weekly"  -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            "monthly" -> cal.add(Calendar.MONTH, 1)
            else      -> return 0L
        }
        return cal.timeInMillis
    }

    fun buildRecurringTask(completedTask: Task): Task {
        return completedTask.copy(
            id = UUID.randomUUID().toString(),
            isCompleted = false,
            createdAt = System.currentTimeMillis(),
            dueDate = getNextDeadline(completedTask),
            overdueNotified = false,
            completedAt = 0L
        )
    }

    // ── NOTIFICATIONS ────────────────────────────────────────────────────────

    suspend fun addNotification(notification: AppNotification) {
        db.collection("notifications").document(notification.id).set(notification).await()
    }

    suspend fun getNotificationsForUser(userId: String): List<AppNotification> {
        return try {
            db.collection("notifications")
                .whereEqualTo("userId", userId)
                .get().await()
                .toObjects(AppNotification::class.java)
                .sortedByDescending { it.createdAt }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun markAllRead(userId: String) {
        val snapshot = db.collection("notifications")
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .get().await()
        val batch = db.batch()
        for (doc in snapshot.documents) batch.update(doc.reference, "isRead", true)
        batch.commit().await()
    }

    suspend fun getUnreadCount(userId: String): Int {
        return try {
            db.collection("notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get().await()
                .size()
        } catch (e: Exception) { 0 }
    }
}
