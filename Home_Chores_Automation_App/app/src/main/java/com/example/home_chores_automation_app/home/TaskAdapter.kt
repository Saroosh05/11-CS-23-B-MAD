package com.example.home_chores_automation_app.home

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.home_chores_automation_app.data.model.Task
import com.example.home_chores_automation_app.databinding.ItemTaskBinding
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskAdapter(
    private val tasks: MutableList<Task>,
    private val memberNames: Map<String, String>,
    private val currentUserId: String,
    private val adminId: String,
    private val onCheckedChange: (Task, Boolean) -> Unit,
    private val onEdit: (Task) -> Unit,
    private val onDelete: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.ViewHolder>() {

    private val dateFormatter = SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault())

    inner class ViewHolder(val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val task = tasks[position]
        holder.binding.tvTaskTitle.text = task.title

        val assignedName = memberNames[task.assignedTo] ?: "Unassigned"
        holder.binding.tvAssignedTo.text = "Assigned to: $assignedName"

        // Due date
        val now = System.currentTimeMillis()
        val isOverdue = task.dueDate > 0L && task.dueDate < now && !task.isCompleted
        if (task.dueDate > 0L) {
            holder.binding.tvDueDate.visibility = View.VISIBLE
            if (isOverdue) {
                val diffMs = now - task.dueDate
                holder.binding.tvDueDate.text = "Overdue by ${formatDuration(diffMs)} · ${dateFormatter.format(Date(task.dueDate))}"
                holder.binding.tvDueDate.setTextColor(Color.parseColor("#D32F2F"))
            } else {
                holder.binding.tvDueDate.text = "Due: ${dateFormatter.format(Date(task.dueDate))}"
                holder.binding.tvDueDate.setTextColor(Color.parseColor("#757575"))
            }
        } else {
            holder.binding.tvDueDate.visibility = View.GONE
        }

        // Late-completed badge
        val isLateCompleted = task.isCompleted && task.dueDate > 0L &&
                task.completedAt > 0L && task.completedAt > task.dueDate
        holder.binding.tvLateCompleted.visibility = if (isLateCompleted) View.VISIBLE else View.GONE

        // Recurring badge (guard against null from Gson deserializing old stored tasks)
        val recurrence = task.recurrence ?: "none"
        if (recurrence != "none") {
            holder.binding.tvRecurringBadge.visibility = View.VISIBLE
            val label = recurrence.replaceFirstChar { it.uppercase() }
            holder.binding.tvRecurringBadge.text = "↻ $label"
        } else {
            holder.binding.tvRecurringBadge.visibility = View.GONE
        }

        // Prevent listener triggering during bind
        holder.binding.cbDone.setOnCheckedChangeListener(null)
        holder.binding.cbDone.isChecked = task.isCompleted

        // Only admin or assigned user can mark complete
        val canToggle = currentUserId == adminId || currentUserId == task.assignedTo
        holder.binding.cbDone.isEnabled = canToggle
        holder.binding.cbDone.alpha = if (canToggle) 1f else 0.4f

        // Show edit/delete only to admin
        val isAdmin = currentUserId == adminId
        holder.binding.layoutAdminActions.visibility = if (isAdmin) View.VISIBLE else View.GONE
        if (isAdmin) {
            holder.binding.btnEditTask.setOnClickListener { onEdit(task) }
            holder.binding.btnDeleteTask.setOnClickListener { onDelete(task) }
        }

        val cardColor = when {
            task.isCompleted -> Color.parseColor("#E8F5E9")
            isOverdue -> Color.parseColor("#FFEBEE")
            else -> Color.parseColor("#FFFFFF")
        }
        (holder.itemView as? MaterialCardView)?.setCardBackgroundColor(cardColor)

        if (task.isCompleted) {
            holder.binding.tvTaskTitle.paintFlags =
                holder.binding.tvTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.binding.tvTaskTitle.paintFlags =
                holder.binding.tvTaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }

        holder.binding.cbDone.setOnCheckedChangeListener { _, isChecked ->
            val pos = holder.bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnCheckedChangeListener
            val now = System.currentTimeMillis()
            tasks[pos] = tasks[pos].copy(
                isCompleted = isChecked,
                completedAt = if (isChecked) now else 0L
            )
            val updatedTask = tasks[pos]

            // Update visuals directly on the ViewHolder — no notifyItemChanged call,
            // which eliminates any chance of RecyclerView throwing IllegalStateException
            // while it is still processing the touch event that fired this listener.
            if (isChecked) {
                holder.binding.tvTaskTitle.paintFlags =
                    holder.binding.tvTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                holder.binding.tvTaskTitle.paintFlags =
                    holder.binding.tvTaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
            val overdueNow = updatedTask.dueDate > 0L && updatedTask.dueDate < now && !isChecked
            val cardColor = when {
                isChecked  -> Color.parseColor("#E8F5E9")
                overdueNow -> Color.parseColor("#FFEBEE")
                else       -> Color.parseColor("#FFFFFF")
            }
            (holder.itemView as? MaterialCardView)?.setCardBackgroundColor(cardColor)

            onCheckedChange(updatedTask, isChecked)
        }
    }

    override fun getItemCount() = tasks.size

    private fun formatDuration(ms: Long): String {
        val minutes = ms / 60_000
        val hours = minutes / 60
        val days = hours / 24
        return when {
            days >= 1 -> "${days}d"
            hours >= 1 -> "${hours}h"
            minutes >= 1 -> "${minutes}m"
            else -> "moments"
        }
    }
}
