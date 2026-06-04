package com.example.home_chores_automation_app.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.home_chores_automation_app.data.prefs.SessionManager
import com.example.home_chores_automation_app.data.repository.FirebaseRepository
import com.example.home_chores_automation_app.databinding.FragmentCalendarBinding
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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

        val repo   = FirebaseRepository.getInstance()
        val userId = SessionManager(requireContext()).getCurrentUserId() ?: return

        binding.rvCalendar.layoutManager = LinearLayoutManager(requireContext())
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        viewLifecycleOwner.lifecycleScope.launch {
            val groups = repo.getGroupsForUser(userId)
            // Fetch tasks for all groups in parallel; getUserById hits cache after first load
            val allTasks = coroutineScope {
                groups.map { group ->
                    async {
                        repo.getTasksForGroup(group.id).map { task ->
                            val assignedName = repo.getUserById(task.assignedTo)?.name ?: "Unassigned"
                            Triple(task, group.name, assignedName)
                        }
                    }
                }.awaitAll()
            }.flatten()

            if (_binding == null) return@launch

            if (allTasks.isEmpty()) {
                binding.rvCalendar.visibility  = View.GONE
                binding.layoutEmpty.visibility = View.VISIBLE
                binding.tvTaskCount.text       = "No tasks yet"
                return@launch
            }

            binding.rvCalendar.visibility  = View.VISIBLE
            binding.layoutEmpty.visibility = View.GONE

            val dateFormat = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())
            val grouped = allTasks.sortedBy { it.first.createdAt }
                .groupBy { dateFormat.format(Date(it.first.createdAt)) }

            val items = mutableListOf<CalendarItem>()
            for ((dateLabel, tasks) in grouped) {
                items.add(CalendarItem.Header(dateLabel))
                for ((task, groupName, assignedName) in tasks) {
                    items.add(CalendarItem.TaskRow(
                        taskTitle     = task.title,
                        assignedName  = assignedName,
                        groupName     = groupName,
                        isCompleted   = task.isCompleted
                    ))
                }
            }

            binding.tvTaskCount.text = "${allTasks.size} task${if (allTasks.size == 1) "" else "s"}"
            binding.rvCalendar.adapter = CalendarAdapter(items)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
