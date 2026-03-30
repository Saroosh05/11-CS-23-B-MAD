package com.example.home_chores_automation_app

import com.example.home_chores_automation_app.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.home_chores_automation_app.model.Member

class MemberAdapter(private var items: List<Member>) : RecyclerView.Adapter<MemberAdapter.VH>() {
    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.memberName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_member, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.name.text = items[position].name
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<Member>) {
        items = newItems
        notifyDataSetChanged()
    }
}
