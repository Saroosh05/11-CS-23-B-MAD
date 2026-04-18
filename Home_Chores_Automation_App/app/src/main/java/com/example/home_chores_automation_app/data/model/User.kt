package com.example.home_chores_automation_app.data.model

import java.util.UUID

data class User(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val avatarColorHex: String = "#FF6B35",
    val profilePictureBase64: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
