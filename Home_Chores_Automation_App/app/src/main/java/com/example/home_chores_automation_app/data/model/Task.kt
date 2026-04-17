package com.example.home_chores_automation_app.data.model

data class Task(
    val id: String,
    val groupId: String,
    val title: String,
    val description: String,
    val assignedTo: String,
    val createdBy: String,
    val isCompleted: Boolean,
    val createdAt: Long
)
