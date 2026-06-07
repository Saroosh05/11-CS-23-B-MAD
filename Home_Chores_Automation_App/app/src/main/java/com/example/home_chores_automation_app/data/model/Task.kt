package com.example.home_chores_automation_app.data.model

import com.google.firebase.firestore.PropertyName

data class Task(
    val id: String = "",
    val groupId: String = "",
    val title: String = "",
    val description: String = "",
    val assignedTo: String = "",
    val createdBy: String = "",
    @get:PropertyName("isCompleted")
    @set:PropertyName("isCompleted")
    var isCompleted: Boolean = false,
    val createdAt: Long = 0L,
    val dueDate: Long = 0L,
    val recurrence: String = "none",  // "none", "daily", "weekly", "monthly"
    @get:PropertyName("overdueNotified")
    @set:PropertyName("overdueNotified")
    var overdueNotified: Boolean = false,
    @get:PropertyName("reminderSent")
    @set:PropertyName("reminderSent")
    var reminderSent: Boolean = false,
    val completedAt: Long = 0L,
    val pointsAwarded: Int = 0   // points granted for the current completion cycle
)
