package com.example.home_chores_automation_app.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.home_chores_automation_app.data.model.User
import com.example.home_chores_automation_app.data.model.UserStats
import com.example.home_chores_automation_app.databinding.ItemLeaderboardBinding

data class LeaderboardEntry(val rank: Int, val user: User, val stats: UserStats)

class LeaderboardAdapter(
    private val entries: List<LeaderboardEntry>
) : RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemLeaderboardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemLeaderboardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]

        holder.binding.tvRank.text = when (entry.rank) {
            1    -> "🥇"
            2    -> "🥈"
            3    -> "🥉"
            else -> "#${entry.rank}"
        }

        holder.binding.tvAvatar.text = entry.user.name.first().uppercaseChar().toString()
        holder.binding.tvName.text   = entry.user.name
        holder.binding.tvPoints.text = "${entry.stats.points} pts"
        holder.binding.tvStreakCompleted.text =
            "🔥 ${entry.stats.streakDays}d  ·  ✓ ${entry.stats.totalCompleted} done"

        val badgeText = entry.stats.badges.joinToString("  ") { badgeLabel(it) }
        if (badgeText.isEmpty()) {
            holder.binding.tvBadges.visibility = View.GONE
        } else {
            holder.binding.tvBadges.visibility = View.VISIBLE
            holder.binding.tvBadges.text       = badgeText
        }
    }

    private fun badgeLabel(badge: String): String = when (badge) {
        "Clean Freak"   -> "🧹 Clean Freak"
        "Never Late"    -> "⏰ Never Late"
        "Streak Master" -> "🔥 Streak Master"
        "Overachiever"  -> "🌟 Overachiever"
        "Team Player"   -> "🤝 Team Player"
        else            -> badge
    }

    override fun getItemCount() = entries.size
}
