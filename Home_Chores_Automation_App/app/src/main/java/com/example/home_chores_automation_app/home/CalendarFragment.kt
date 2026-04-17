package com.example.home_chores_automation_app.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.home_chores_automation_app.data.prefs.SessionManager
import com.example.home_chores_automation_app.data.repository.AppRepository
import com.example.home_chores_automation_app.databinding.FragmentCalendarBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repo = AppRepository.getInstance(requireContext())
        val session = SessionManager(requireContext())
        val userId = session.getCurrentUserId() ?: return

        binding.rvCalendar.layoutManager = LinearLayoutManager(requireContext())

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        loadCalendar(repo, userId)
    }

    private fun loadCalendar(repo: AppRepository, userId: String) {
        // Gather all tasks across all groups the user belongs to
        val groups = repo.getGroupsForUser(userId)

        // Collect all tasks with group name and assigned name
        val allTasks = groups.flatMap { group ->
            repo.getTasksForGroup(group.id).map { task ->
                val assignedName = repo.findUserById(task.assignedTo)?.name ?: "Unassigned"
                Triple(task, group.name, assignedName)
            }
        }

        if (allTasks.isEmpty()) {
            binding.rvCalendar.visibility = View.GONE
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.tvTaskCount.text = "No tasks yet"
            return
        }

        binding.rvCalendar.visibility = View.VISIBLE
        binding.layoutEmpty.visibility = View.GONE

        // Group tasks by date string (oldest first)
        val dateFormat = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())
        val grouped = allTasks
            .sortedBy { it.first.createdAt }
            .groupBy { dateFormat.format(Date(it.first.createdAt)) }

        // Build flat list of CalendarItems
        val items = mutableListOf<CalendarItem>()
        for ((dateLabel, tasks) in grouped) {
            items.add(CalendarItem.Header(dateLabel))
            for ((task, groupName, assignedName) in tasks) {
                items.add(
                    CalendarItem.TaskRow(
                        taskTitle = task.title,
                        assignedName = assignedName,
                        groupName = groupName,
                        isCompleted = task.isCompleted
                    )
                )
            }
        }

        val total = allTasks.size
        val completed = allTasks.count { it.first.isCompleted }
        binding.tvTaskCount.text = "$completed of $total completed"

        binding.rvCalendar.adapter = CalendarAdapter(items)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
