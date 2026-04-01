package com.example.home_chores_automation_app.utils

import android.graphics.Color
import android.text.format.DateFormat
import java.util.Calendar
import java.util.Date

fun Long.toFormattedDate(): String {
    if (this == 0L) return "No deadline"
    return DateFormat.format("dd MMM yyyy, hh:mm a", Date(this)).toString()
}

fun Long.toShortDate(): String {
    if (this == 0L) return "—"
    return DateFormat.format("dd MMM yyyy", Date(this)).toString()
}

fun Long.toTimeAgo(): String {
    val diff = System.currentTimeMillis() - this
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 604_800_000 -> "${diff / 86_400_000}d ago"
        else -> DateFormat.format("dd MMM", Date(this)).toString()
    }
}

fun Long.isOverdue(): Boolean = this > 0L && this < System.currentTimeMillis()

fun Long.isDueToday(): Boolean {
    if (this == 0L) return false
    val now = Calendar.getInstance()
    val due = Calendar.getInstance().apply { timeInMillis = this@isDueToday }
    return now.get(Calendar.YEAR) == due.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == due.get(Calendar.DAY_OF_YEAR)
}

fun String.toInitials(): String {
    val parts = this.trim().split(" ")
    return when {
        parts.size >= 2 -> "${parts[0].firstOrNull()?.uppercaseChar() ?: ""}${parts[1].firstOrNull()?.uppercaseChar() ?: ""}"
        parts.isNotEmpty() -> parts[0].take(2).uppercase()
        else -> "?"
    }
}

fun String.toColorInt(): Int = try {
    Color.parseColor(this)
} catch (e: Exception) {
    Color.parseColor("#FF6B35")
}
