package com.example.home_chores_automation_app.data.model

data class Group(
    val id: String = "",
    val name: String = "",
    val type: String = "Home",
    val adminId: String = "",
    val memberIds: MutableList<String> = mutableListOf(),
    val inviteCode: String = "",
    val createdAt: Long = 0L,
    val rotationIndex: Int = 0   // tracks whose turn it is in round-robin rotation
)
