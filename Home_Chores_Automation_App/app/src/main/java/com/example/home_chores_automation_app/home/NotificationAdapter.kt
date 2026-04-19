package com.example.home_chores_automation_app.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.home_chores_automation_app.data.model.AppNotification
import com.example.home_chores_automation_app.databinding.ItemNotificationBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationAdapter(
    private val notifications: List<AppNotification>
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notif = notifications[position]
        holder.binding.tvTitle.text = notif.title
        holder.binding.tvMessage.text = notif.message

        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        holder.binding.tvTime.text = sdf.format(Date(notif.createdAt))

        val isOverdue = notif.title == "Task Overdue" || notif.title == "Overdue Alert"
        val overdueColor = Color.parseColor("#D32F2F")

        holder.binding.tvTitle.setTextColor(
            if (isOverdue) overdueColor else Color.parseColor("#212121")
        )
        holder.binding.viewDot.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(
                if (isOverdue) overdueColor else Color.parseColor("#00897B")
            )
        )

        // Unread dot visibility
        holder.binding.viewDot.visibility = if (notif.isRead) View.INVISIBLE else View.VISIBLE

        // Dim read notifications slightly
        holder.itemView.alpha = if (notif.isRead) 0.6f else 1f
    }

    override fun getItemCount() = notifications.size
}
