package com.example.home_chores_automation_app.data.model

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val avatarColorHex: String = "#FF6B35",
    val profilePictureUrl: String? = null,
    val createdAt: Long = 0L
)
