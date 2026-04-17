package com.example.home_chores_automation_app.data.model

data class AppNotification(
    val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val isRead: Boolean,
    val createdAt: Long
)
