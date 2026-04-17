package com.example.home_chores_automation_app.home

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.home_chores_automation_app.data.prefs.SessionManager
import com.example.home_chores_automation_app.data.repository.AppRepository
import com.example.home_chores_automation_app.databinding.FragmentMyTasksBinding

class MyTasksFragment : Fragment() {

    private var _binding: FragmentMyTasksBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // "pending" or "done"
        val filter = arguments?.getString("filter") ?: "pending"

        val repo = AppRepository.getInstance(requireContext())
        val userId = SessionManager(requireContext()).getCurrentUserId() ?: return

        binding.rvMyTasks.layoutManager = LinearLayoutManager(requireContext())

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        // Configure header colour + title based on filter
        if (filter == "done") {
            binding.tvTitle.text = "Completed Tasks"
        } else {
            binding.tvTitle.text = "Pending Tasks"
        }

        loadTasks(repo, userId, filter)
    }

    private fun loadTasks(repo: AppRepository, userId: String, filter: String) {
        val groups = repo.getGroupsForUser(userId)

        // Collect tasks assigned to this user, with group name
        val myTasks = groups.flatMap { group ->
            repo.getTasksForGroup(group.id)
                .filter { it.assignedTo == userId }
                .map { task ->
                    val assignedName = repo.findUserById(task.assignedTo)?.name ?: "Unassigned"
                    Triple(task, group.name, assignedName)
                }
        }

        // Apply filter
        val filtered = if (filter == "done") {
            myTasks.filter { it.first.isCompleted }
        } else {
            myTasks.filter { !it.first.isCompleted }
        }

        binding.tvSubtitle.text = "${filtered.size} task${if (filtered.size == 1) "" else "s"}"

        if (filtered.isEmpty()) {
            binding.rvMyTasks.visibility = View.GONE
            binding.layoutEmpty.visibility = View.VISIBLE
            if (filter == "done") {
                binding.tvEmptyMessage.text = "No Completed Tasks"
                binding.tvEmptyHint.text = "Complete some tasks to see them here"
            } else {
                binding.tvEmptyMessage.text = "All Caught Up!"
                binding.tvEmptyHint.text = "You have no pending tasks right now"
            }
            return
        }

        binding.rvMyTasks.visibility = View.VISIBLE
        binding.layoutEmpty.visibility = View.GONE

        // Build CalendarItem list (no date headers — flat list)
        val items = filtered.map { (task, groupName, assignedName) ->
            CalendarItem.TaskRow(
                taskTitle = task.title,
                assignedName = assignedName,
                groupName = groupName,
                isCompleted = task.isCompleted
            )
        }

        binding.rvMyTasks.adapter = CalendarAdapter(items)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
