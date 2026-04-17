package com.example.home_chores_automation_app.home

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.home_chores_automation_app.data.model.Task
import com.example.home_chores_automation_app.databinding.ItemTaskBinding

class TaskAdapter(
    private val tasks: MutableList<Task>,
    private val memberNames: Map<String, String>,
    private val currentUserId: String,
    private val adminId: String,
    private val onCheckedChange: (Task, Boolean) -> Unit
) : RecyclerView.Adapter<TaskAdapter.ViewHolder>() {

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

        // Prevent listener triggering during bind
        holder.binding.cbDone.setOnCheckedChangeListener(null)
        holder.binding.cbDone.isChecked = task.isCompleted

        // Only admin or assigned user can mark complete
        val canToggle = currentUserId == adminId || currentUserId == task.assignedTo
        holder.binding.cbDone.isEnabled = canToggle
        holder.binding.cbDone.alpha = if (canToggle) 1f else 0.4f

        if (task.isCompleted) {
            holder.binding.tvTaskTitle.paintFlags =
                holder.binding.tvTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.binding.tvTaskTitle.paintFlags =
                holder.binding.tvTaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }

        holder.binding.cbDone.setOnCheckedChangeListener { _, isChecked ->
            // Update the list item in-place so rebind shows correct strikethrough
            tasks[position] = tasks[position].copy(isCompleted = isChecked)
            notifyItemChanged(position)
            onCheckedChange(tasks[position], isChecked)
        }
    }

    override fun getItemCount() = tasks.size
}
