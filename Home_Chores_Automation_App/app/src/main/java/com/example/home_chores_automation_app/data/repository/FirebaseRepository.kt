package com.example.home_chores_automation_app.data.repository

import com.example.home_chores_automation_app.data.model.AppNotification
import com.example.home_chores_automation_app.data.model.Group
import com.example.home_chores_automation_app.data.model.Task
import com.example.home_chores_automation_app.data.model.User
import com.example.home_chores_automation_app.data.model.UserStats
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

    /** Build the next recurring task, assigned to nextAssignee (defaults to same person). */
    fun buildRecurringTask(completedTask: Task, nextAssignee: String = completedTask.assignedTo): Task {
        return completedTask.copy(
            id              = UUID.randomUUID().toString(),
            isCompleted     = false,
            assignedTo      = nextAssignee,
            createdAt       = System.currentTimeMillis(),
            dueDate         = getNextDeadline(completedTask),
            overdueNotified = false,
            reminderSent    = false,
            completedAt     = 0L
        )
    }

    /**
     * Round-robin rotation: returns the next member in the group's rotation order
     * and advances the rotationIndex counter in Firestore.
     */
    suspend fun getNextAssigneeByRotation(group: Group): String {
        val members = group.memberIds
        if (members.isEmpty()) return ""
        val idx = group.rotationIndex % members.size
        db.collection("groups").document(group.id)
            .update("rotationIndex", group.rotationIndex + 1).await()
        return members[idx]
    }

    /**
     * Workload balancing: returns the member userId who has the fewest incomplete tasks,
     * so new tasks are spread evenly across the team.
     */
    suspend fun getNextAssigneeByWorkload(groupId: String, memberIds: List<String>): String {
        if (memberIds.isEmpty()) return ""
        val tasks = getTasksForGroup(groupId)
        val pendingCount = memberIds.associateWith { uid ->
            tasks.count { it.assignedTo == uid && !it.isCompleted }
        }
        return pendingCount.minByOrNull { it.value }?.key ?: memberIds.first()
    }

    /**
     * Returns tasks assigned to userId that are due within the next 2 hours
     * and have not yet had a reminder sent.
     */
    suspend fun getUpcomingTasksForReminder(userId: String): List<Task> {
        val now          = System.currentTimeMillis()
        val twoHoursAway = now + 2 * 60 * 60 * 1000L
        return getGroupsForUser(userId)
            .flatMap { getTasksForGroup(it.id) }
            .filter { task ->
                task.assignedTo == userId
                    && !task.isCompleted
                    && !task.reminderSent
                    && task.dueDate in (now + 1)..twoHoursAway
            }
    }

    suspend fun markReminderSent(taskId: String) {
        db.collection("tasks").document(taskId)
            .update("reminderSent", true).await()
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

    // ── GAMIFICATION ─────────────────────────────────────────────────────────

    private fun statsDocId(userId: String, groupId: String) = "${groupId}_${userId}"

    suspend fun getUserStats(userId: String, groupId: String): UserStats {
        return try {
            db.collection("user_stats")
                .document(statsDocId(userId, groupId))
                .get().await()
                .toObject(UserStats::class.java)
                ?: UserStats(userId = userId, groupId = groupId)
        } catch (e: Exception) {
            UserStats(userId = userId, groupId = groupId)
        }
    }

    suspend fun updateUserStats(stats: UserStats) {
        db.collection("user_stats")
            .document(statsDocId(stats.userId, stats.groupId))
            .set(stats).await()
    }

    /**
     * Called when a task is marked complete.
     * Awards +10 pts (on-time), +5 pts (no deadline), +3 pts (late but done).
     * Updates streak, consecutive on-time counter, and checks for new badges.
     */
    suspend fun awardTaskCompletion(task: Task) {
        if (task.assignedTo.isEmpty() || task.groupId.isEmpty()) return
        val stats      = getUserStats(task.assignedTo, task.groupId)
        val now        = System.currentTimeMillis()
        val completedAt = if (task.completedAt > 0L) task.completedAt else now
        val onTime     = task.dueDate > 0L && completedAt <= task.dueDate
        val pointsEarned = when {
            onTime          -> 10
            task.dueDate == 0L -> 5   // no deadline — still reward completion
            else            ->  3   // completed late
        }
        val newConsecutive = if (onTime) stats.consecutiveOnTime + 1 else 0

        // Extend day-streak if last completion was today or yesterday
        val todayStart     = getDayStart(now)
        val lastDayStart   = if (stats.lastCompletionDate > 0L) getDayStart(stats.lastCompletionDate) else 0L
        val yesterdayStart = todayStart - 86_400_000L
        val newStreak = when {
            lastDayStart == todayStart     -> stats.streakDays
            lastDayStart == yesterdayStart -> stats.streakDays + 1
            stats.lastCompletionDate == 0L -> 1
            else                           -> 1
        }

        val updated = checkAndAwardBadges(
            stats.copy(
                points            = stats.points + pointsEarned,
                totalCompleted    = stats.totalCompleted + 1,
                totalOnTime       = if (onTime) stats.totalOnTime + 1 else stats.totalOnTime,
                consecutiveOnTime = newConsecutive,
                streakDays        = newStreak,
                lastCompletionDate = now
            )
        )
        updateUserStats(updated)
    }

    /**
     * Called when a task goes overdue.
     * Deducts 5 pts (minimum 0) and resets the consecutive on-time counter.
     */
    suspend fun penalizeOverdue(userId: String, groupId: String) {
        if (userId.isEmpty() || groupId.isEmpty()) return
        val stats   = getUserStats(userId, groupId)
        val updated = checkAndAwardBadges(
            stats.copy(
                points            = maxOf(0, stats.points - 5),
                consecutiveOnTime = 0
            )
        )
        updateUserStats(updated)
    }

    private fun getDayStart(time: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = time
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun checkAndAwardBadges(stats: UserStats): UserStats {
        val badges = stats.badges.toMutableList()
        if (stats.totalCompleted    >= 10 && "Clean Freak"   !in badges) badges.add("Clean Freak")
        if (stats.consecutiveOnTime >=  5 && "Never Late"    !in badges) badges.add("Never Late")
        if (stats.streakDays        >=  7 && "Streak Master" !in badges) badges.add("Streak Master")
        if (stats.totalCompleted    >= 30 && "Overachiever"  !in badges) badges.add("Overachiever")
        return stats.copy(badges = badges)
    }

    /** Returns all members' stats sorted by points descending (for leaderboard). */
    suspend fun getLeaderboardForGroup(groupId: String, memberIds: List<String>): List<UserStats> =
        memberIds.map { getUserStats(it, groupId) }.sortedByDescending { it.points }

    /**
     * Generates a weekly summary notification for the whole group.
     * Safe to call repeatedly — only runs once per calendar week (Monday trigger).
     */
    suspend fun generateWeeklySummaryIfNeeded(groupId: String, memberIds: List<String>) {
        if (memberIds.isEmpty()) return
        val cal     = Calendar.getInstance()
        val weekKey = "${cal.get(Calendar.YEAR)}-W${cal.get(Calendar.WEEK_OF_YEAR)}"
        val flagRef = db.collection("weekly_summary_flags").document(groupId)
        val existing = try { flagRef.get().await().getString("lastGeneratedWeek") } catch (e: Exception) { null }
        if (existing == weekKey) return

        val statsList        = memberIds.map { getUserStats(it, groupId) }
        val topByPoints      = statsList.maxByOrNull { it.points }
        val topByCompleted   = statsList.maxByOrNull { it.totalCompleted }
        val topPointsName    = topByPoints?.userId?.let { getUserById(it)?.name } ?: "Unknown"
        val topCompletedName = topByCompleted?.userId?.let { getUserById(it)?.name } ?: "Unknown"
        val summaryMsg = "🏆 $topPointsName leads with ${topByPoints?.points ?: 0} pts. " +
            "Most tasks done: $topCompletedName (${topByCompleted?.totalCompleted ?: 0}). Great work!"

        val now = System.currentTimeMillis()
        for (uid in memberIds) {
            addNotification(AppNotification(
                id        = UUID.randomUUID().toString(),
                userId    = uid,
                title     = "📊 Weekly Group Summary",
                message   = summaryMsg,
                isRead    = false,
                createdAt = now
            ))
        }
        flagRef.set(mapOf("lastGeneratedWeek" to weekKey, "generatedAt" to now)).await()
    }
}
