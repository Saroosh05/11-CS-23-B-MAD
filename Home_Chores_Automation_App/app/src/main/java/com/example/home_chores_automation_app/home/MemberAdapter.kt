package com.example.home_chores_automation_app.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.home_chores_automation_app.data.model.User
import com.example.home_chores_automation_app.databinding.ItemMemberBinding

class MemberAdapter(
    private val members: List<User>,
    private val adminId: String
) : RecyclerView.Adapter<MemberAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemMemberBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = members[position]
        holder.binding.tvMemberName.text = user.name
        holder.binding.tvAvatar.text = user.name.first().uppercaseChar().toString()

        if (user.id == adminId) {
            holder.binding.tvAdminBadge.visibility = View.VISIBLE
        } else {
            holder.binding.tvAdminBadge.visibility = View.GONE
        }
    }

    override fun getItemCount() = members.size
}
