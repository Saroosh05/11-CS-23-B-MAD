package com.example.home_chores_automation_app.model

import java.util.*

data class Task(
    val id: Long,
    val title: String,
    val assigneeId: Long?,
    val due: Date?,
    var completed: Boolean = false
)
