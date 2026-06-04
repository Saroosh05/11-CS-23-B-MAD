package com.example.home_chores_automation_app.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.home_chores_automation_app.data.model.AppNotification
import com.example.home_chores_automation_app.data.model.Task
import com.example.home_chores_automation_app.data.prefs.SessionManager
import com.example.home_chores_automation_app.data.repository.FirebaseRepository
import com.example.home_chores_automation_app.databinding.FragmentMyTasksBinding
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.UUID

class MyTasksFragment : Fragment() {

    private var _binding: FragmentMyTasksBinding? = null
    private val binding get() = _binding!!

    private val repo = FirebaseRepository.getInstance()
    private lateinit var userId: String
    private lateinit var filter: String

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

        filter = arguments?.getString("filter") ?: "pending"
        userId = SessionManager(requireContext()).getCurrentUserId() ?: return

        binding.rvMyTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.tvTitle.text = if (filter == "done") "Completed Tasks" else "Pending Tasks"

        loadTasks()
    }

    private fun loadTasks() {
        viewLifecycleOwner.lifecycleScope.launch {
            val groups = repo.getGroupsForUser(userId)
            // Fetch tasks for all groups in parallel
            val allMyTasks = coroutineScope {
                groups.map { group ->
                    async { repo.getTasksForGroup(group.id).filter { it.assignedTo == userId } }
                }.awaitAll()
            }.flatten()

            if (filter != "done") checkAndNotifyOverdue(allMyTasks.filter { !it.isCompleted }, groups)

            val memberNames = groups.flatMap { it.memberIds }.distinct()
                .mapNotNull { repo.getUserById(it) }.associate { it.id to it.name }

            val filtered = if (filter == "done") {
                allMyTasks.filter { it.isCompleted }
            } else {
                allMyTasks.filter { !it.isCompleted }
            }.toMutableList()

            if (_binding == null) return@launch

            binding.tvSubtitle.text = "${filtered.size} task${if (filtered.size == 1) "" else "s"}"

            if (filtered.isEmpty()) {
                binding.rvMyTasks.visibility   = View.GONE
                binding.layoutEmpty.visibility = View.VISIBLE
                if (filter == "done") {
                    binding.tvEmptyMessage.text = "No Completed Tasks"
                    binding.tvEmptyHint.text    = "Complete some tasks to see them here"
                } else {
                    binding.tvEmptyMessage.text = "All Caught Up!"
                    binding.tvEmptyHint.text    = "You have no pending tasks right now"
                }
                return@launch
            }

            binding.rvMyTasks.visibility   = View.VISIBLE
            binding.layoutEmpty.visibility = View.GONE
            binding.rvMyTasks.adapter = TaskAdapter(
                tasks = filtered, memberNames = memberNames,
                currentUserId = userId, adminId = "",
                onCheckedChange = { task, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        repo.updateTask(task)
                        view?.post { if (_binding != null) loadTasks() }
                    }
                },
                onEdit   = {},
                onDelete = {}
            )
        }
    }

    private suspend fun checkAndNotifyOverdue(
        tasks: List<Task>,
        groups: List<com.example.home_chores_automation_app.data.model.Group>
    ) {
        val now      = System.currentTimeMillis()
        val groupMap = groups.associateBy { it.id }
        for (task in tasks) {
            val isOverdue = task.dueDate > 0L && task.dueDate < now
            if (isOverdue && task.overdueNotified != true) {
                repo.markOverdueNotified(task.id)
                val adminId      = groupMap[task.groupId]?.adminId ?: ""
                val assigneeName = repo.getUserById(task.assignedTo)?.name ?: "Someone"
                repo.addNotification(AppNotification(
                    id = UUID.randomUUID().toString(), userId = task.assignedTo,
                    title = "Task Overdue", message = "\"${task.title}\" is now overdue",
                    isRead = false, createdAt = now
                ))
                if (adminId.isNotEmpty() && task.assignedTo != adminId) {
                    repo.addNotification(AppNotification(
                        id = UUID.randomUUID().toString(), userId = adminId,
                        title = "Overdue Alert",
                        message = "\"${task.title}\" assigned to $assigneeName is overdue",
                        isRead = false, createdAt = now
                    ))
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
