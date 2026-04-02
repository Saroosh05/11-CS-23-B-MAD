package com.example.home_chores_automation_app.data.model

import java.util.UUID

data class Group(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val type: String = "Home",
    val adminId: String = "",
    val memberIds: MutableList<String> = mutableListOf(),
    val inviteCode: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
