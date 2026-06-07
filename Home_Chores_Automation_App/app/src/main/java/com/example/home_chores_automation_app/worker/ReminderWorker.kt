package com.example.home_chores_automation_app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.home_chores_automation_app.MainActivity
import com.example.home_chores_automation_app.R
import com.example.home_chores_automation_app.data.repository.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * WorkManager background job that runs every hour (even when the app is closed).
 * It checks for two things:
 *  1. Tasks due within the next 2 hours  → sends a "reminder" notification
 *  2. Tasks that are already overdue     → sends an "overdue" notification
 */
class ReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val channelId = "chores_reminders"

    override suspend fun doWork(): Result {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()
        val repo   = FirebaseRepository.getInstance()

        ensureNotificationChannel()

        // ── Upcoming reminders (due in ≤ 2 hours) ──────────────────────────
        val upcoming = repo.getUpcomingTasksForReminder(userId)
        for (task in upcoming) {
            val timeLeft = task.dueDate - System.currentTimeMillis()
            val minutes  = timeLeft / 60_000
            val label    = if (minutes < 60) "$minutes min" else "${minutes / 60}h ${minutes % 60}min"
            showNotification(
                id      = task.id.hashCode(),
                title   = "⏰ Reminder: ${task.title}",
                message = "Due in $label — ${task.description.ifEmpty { "No description" }}"
            )
            repo.markReminderSent(task.id)
        }

        // ── Overdue alerts ──────────────────────────────────────────────────
        val groups = repo.getGroupsForUser(userId)
        val now    = System.currentTimeMillis()
        for (group in groups) {
            for (task in repo.getTasksForGroup(group.id)) {
                val isOverdue = task.dueDate > 0L && task.dueDate < now && !task.isCompleted
                if (isOverdue && !task.overdueNotified) {
                    val dateStr = SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault())
                        .format(Date(task.dueDate))
                    showNotification(
                        id      = ("overdue_" + task.id).hashCode(),
                        title   = "🚨 Overdue: ${task.title}",
                        message = "Was due $dateStr — ${group.name}"
                    )
                    repo.markOverdueNotified(task.id)
                    repo.penalizeOverdue(task.assignedTo, task.groupId)
                }
            }
        }

        // ── Weekly summary (Mondays only) ───────────────────────────────────
        val cal = java.util.Calendar.getInstance()
        if (cal.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.MONDAY) {
            for (group in groups) {
                repo.generateWeeklySummaryIfNeeded(group.id, group.memberIds)
            }
        }

        return Result.success()
    }

    private fun showNotification(id: Int, title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java)
            .apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) }
        val pendingIntent = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(id, notification)
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(channelId) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(channelId, "Task Reminders", NotificationManager.IMPORTANCE_HIGH)
                        .apply { description = "Reminders for upcoming and overdue chores" }
                )
            }
        }
    }
}
