package com.example.home_chores_automation_app.home

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.home_chores_automation_app.databinding.ItemCalendarHeaderBinding
import com.example.home_chores_automation_app.databinding.ItemTaskBinding

// Sealed list item: either a date header or a task row
sealed class CalendarItem {
    data class Header(val label: String) : CalendarItem()
    data class TaskRow(
        val taskTitle: String,
        val assignedName: String,
        val groupName: String,
        val isCompleted: Boolean
    ) : CalendarItem()
}

class CalendarAdapter(
    private val items: List<CalendarItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_TASK = 1
    }

    override fun getItemViewType(position: Int): Int =
        if (items[position] is CalendarItem.Header) TYPE_HEADER else TYPE_TASK

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val binding = ItemCalendarHeaderBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            HeaderViewHolder(binding)
        } else {
            val binding = ItemTaskBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            TaskViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        if (holder is HeaderViewHolder && item is CalendarItem.Header) {
            holder.binding.tvDateHeader.text = item.label
        } else if (holder is TaskViewHolder && item is CalendarItem.TaskRow) {
            holder.binding.tvTaskTitle.text = item.taskTitle
            holder.binding.tvAssignedTo.text = "${item.groupName}  ·  ${item.assignedName}"

            // Checkbox is display-only in calendar view
            holder.binding.cbDone.setOnCheckedChangeListener(null)
            holder.binding.cbDone.isChecked = item.isCompleted
            holder.binding.cbDone.isEnabled = false
            holder.binding.cbDone.alpha = 0.6f

            // Strikethrough for completed
            if (item.isCompleted) {
                holder.binding.tvTaskTitle.paintFlags =
                    holder.binding.tvTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                holder.binding.tvTaskTitle.paintFlags =
                    holder.binding.tvTaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            // Always hide admin actions in calendar view
            holder.binding.layoutAdminActions.visibility = android.view.View.GONE
        }
    }

    override fun getItemCount() = items.size

    inner class HeaderViewHolder(val binding: ItemCalendarHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class TaskViewHolder(val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root)
}
