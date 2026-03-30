package com.example.home_chores_automation_app

import com.example.home_chores_automation_app.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.home_chores_automation_app.model.Task
import java.text.SimpleDateFormat
import java.util.*

class TaskAdapter(
    private var items: List<Task>,
    private val membersById: Map<Long, String>,
    private val onToggle: (Long) -> Unit
) : RecyclerView.Adapter<TaskAdapter.VH>() {

    private val fmt = SimpleDateFormat.getDateTimeInstance()

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.taskTitle)
        val meta: TextView = v.findViewById(R.id.taskMeta)
        val btn: Button = v.findViewById(R.id.taskToggle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val t = items[position]
        holder.title.text = t.title
        val assignee = t.assigneeId?.let { membersById[it] } ?: "Unassigned"
        val due = t.due?.let { fmt.format(it) } ?: "No deadline"
        holder.meta.text = "Assigned: $assignee • Due: $due"
        holder.btn.text = if (t.completed) "Undo" else "Complete"
        holder.btn.setOnClickListener { onToggle(t.id) }
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<Task>) {
        items = newItems
        notifyDataSetChanged()
    }
}
