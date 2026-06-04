package com.example.home_chores_automation_app.data.model

data class UserStats(
    val userId: String = "",
    val groupId: String = "",
    val points: Int = 0,
    val totalCompleted: Int = 0,
    val totalOnTime: Int = 0,          // tasks finished before their due date
    val consecutiveOnTime: Int = 0,    // current run of back-to-back on-time completions
    val streakDays: Int = 0,           // consecutive calendar days with at least one completion
    val lastCompletionDate: Long = 0L,
    val badges: List<String> = emptyList()
)
