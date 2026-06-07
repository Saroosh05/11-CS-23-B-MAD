package com.example.home_chores_automation_app.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.home_chores_automation_app.data.model.Task
import com.example.home_chores_automation_app.data.prefs.SessionManager
import com.example.home_chores_automation_app.data.repository.FirebaseRepository
import com.example.home_chores_automation_app.data.repository.GeminiRepository
import com.example.home_chores_automation_app.databinding.FragmentAiInsightsBinding
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AiInsightsFragment : Fragment() {

    private var _binding: FragmentAiInsightsBinding? = null
    private val binding get() = _binding!!

    private val repo   = FirebaseRepository.getInstance()
    private val gemini = GeminiRepository.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiInsightsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val groupId = arguments?.getString("groupId") ?: return
        val userId  = SessionManager(requireContext()).getCurrentUserId() ?: return

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        loadInsights(groupId, userId)
        binding.btnRefreshAssignment.setOnClickListener { loadFairAssignment(groupId) }
    }

    // ── Productivity Insights (pure local analytics, no AI needed) ───────────

    private fun loadInsights(groupId: String, userId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val group  = repo.getGroupById(groupId) ?: return@launch
            if (_binding == null) return@launch
            binding.tvGroupName.text = group.name

            val allTasks = coroutineScope {
                group.memberIds.map { async { repo.getTasksForGroup(groupId) } }.awaitAll()
            }.flatten().distinctBy { it.id }

            val myTasks = allTasks.filter { it.assignedTo == userId }
            showProductivityInsights(myTasks)
            loadFairAssignment(groupId)
        }
    }

    private fun showProductivityInsights(tasks: List<Task>) {
        if (_binding == null) return
        val total      = tasks.size
        val completed  = tasks.filter { it.isCompleted }
        val withDue    = completed.filter { it.dueDate > 0L && it.completedAt > 0L }
        val onTime     = withDue.count { it.completedAt <= it.dueDate }
        val onTimePct  = if (withDue.isNotEmpty()) (onTime * 100 / withDue.size) else 0
        val avgDays    = if (completed.isNotEmpty()) {
            val sumMs = completed.filter { it.createdAt > 0L && it.completedAt > 0L }
                .sumOf { it.completedAt - it.createdAt }
            val count = completed.count { it.createdAt > 0L && it.completedAt > 0L }
            if (count > 0) sumMs / count / 86_400_000.0 else 0.0
        } else 0.0

        // Busiest day of week
        val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        val dayCount  = completed
            .filter { it.completedAt > 0L }
            .groupingBy { dayFormat.format(Date(it.completedAt)) }
            .eachCount()
        val busiestDay = dayCount.maxByOrNull { it.value }?.key ?: "N/A"

        // Streak from UserStats — read from repo (already computed there)
        val pending = tasks.count { !it.isCompleted }
        val overdue = tasks.count { it.dueDate > 0L && it.dueDate < System.currentTimeMillis() && !it.isCompleted }

        // Build insight text
        val sb = StringBuilder()
        sb.appendLine("📊  Your Productivity Snapshot")
        sb.appendLine()
        if (total == 0) {
            sb.appendLine("No tasks assigned to you yet in this group.")
        } else {
            sb.appendLine("✅  You completed $onTimePct% of tasks on time.")
            sb.appendLine("📋  Total assigned: $total   |   Done: ${completed.size}   |   Pending: $pending")
            if (overdue > 0)  sb.appendLine("⚠️  Overdue right now: $overdue")
            if (busiestDay != "N/A") sb.appendLine("📅  Your most productive day is $busiestDay.")
            if (avgDays > 0)  sb.appendLine("⏱️  Average time to complete a task: ${"%.1f".format(avgDays)} days.")
        }

        binding.tvInsights.text = sb.toString().trim()
    }

    // ── Fair Assignment (Gemini AI) ───────────────────────────────────────────

    private fun loadFairAssignment(groupId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val group = repo.getGroupById(groupId) ?: return@launch
            if (_binding == null) return@launch

            binding.tvAssignmentSuggestion.text = "Analyzing team stats…"
            binding.btnRefreshAssignment.isEnabled = false

            try {
                val allTasks = repo.getTasksForGroup(groupId)
                val now      = System.currentTimeMillis()
                val memberStats = coroutineScope {
                    group.memberIds.map { uid ->
                        async {
                            val user = repo.getUserById(uid)
                            val myTasks = allTasks.filter { it.assignedTo == uid }
                            GeminiRepository.MemberStat(
                                name           = user?.name ?: uid,
                                completedOnTime = myTasks.count { it.isCompleted && it.dueDate > 0L && it.completedAt <= it.dueDate },
                                completedLate   = myTasks.count { it.isCompleted && it.dueDate > 0L && it.completedAt > it.dueDate },
                                overdue         = myTasks.count { !it.isCompleted && it.dueDate > 0L && it.dueDate < now },
                                pending         = myTasks.count { !it.isCompleted }
                            )
                        }
                    }.awaitAll()
                }

                val suggestion = gemini.suggestFairAssignment("next chore", memberStats)
                if (_binding == null) return@launch
                binding.tvAssignmentSuggestion.text = "🤖  $suggestion"
            } catch (e: Exception) {
                if (_binding == null) return@launch
                val message = GeminiRepository.friendlyMessage(e)
                binding.tvAssignmentSuggestion.text = message
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            } finally {
                if (_binding != null) binding.btnRefreshAssignment.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
