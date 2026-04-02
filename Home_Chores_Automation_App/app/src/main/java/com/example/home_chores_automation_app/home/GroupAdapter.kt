package com.example.home_chores_automation_app.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.home_chores_automation_app.data.model.Group
import com.example.home_chores_automation_app.databinding.ItemGroupBinding

class GroupAdapter(
    private val groups: List<Group>,
    private val onClick: (Group) -> Unit
) : RecyclerView.Adapter<GroupAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemGroupBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val group = groups[position]
        holder.binding.tvGroupName.text = group.name
        holder.binding.tvGroupType.text = group.type
        holder.binding.tvMemberCount.text = "${group.memberIds.size} member(s)"
        holder.itemView.setOnClickListener { onClick(group) }
    }

    override fun getItemCount() = groups.size
}
