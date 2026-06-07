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
                onCheckedChange = { previous, updated, isChecked ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            val saved = repo.handleTaskCompletionChange(previous, updated)
                            repo.updateTask(saved)
                            if (saved.isCompleted) {
                                if (saved.recurrence != "none") {
                                    regenerateRecurringTask(saved)
                                }
                                if (_binding == null) return@launch
                                val adapter = binding.rvMyTasks.adapter as? TaskAdapter
                                if (filter == "pending" && adapter != null) {
                                    val idx = adapter.getTasks().indexOfFirst { it.id == saved.id }
                                    if (idx >= 0) {
                                        adapter.removeAt(idx)
                                        val remaining = adapter.itemCount
                                        binding.tvSubtitle.text =
                                            "$remaining task${if (remaining == 1) "" else "s"}"
                                        if (remaining == 0) {
                                            binding.rvMyTasks.visibility = View.GONE
                                            binding.layoutEmpty.visibility = View.VISIBLE
                                            binding.tvEmptyMessage.text = "All Caught Up!"
                                            binding.tvEmptyHint.text =
                                                "You have no pending tasks right now"
                                        }
                                    }
                                }
                            } else if (filter == "done") {
                                loadTasks()
                            }
                        } catch (e: Exception) {
                            loadTasks()
                        }
                    }
                },
                onEdit   = {},
                onDelete = {}
            )
        }
    }

    private suspend fun regenerateRecurringTask(completedTask: Task) {
        val group = repo.getGroupById(completedTask.groupId) ?: return
        val nextAssignee = repo.getNextAssigneeByWorkload(
            group.id,
            group.memberIds,
            excludeUserId = completedTask.assignedTo
        ).ifEmpty { completedTask.assignedTo }
        val newTask = repo.buildRecurringTask(completedTask, nextAssignee)
        repo.createTask(newTask)

        val assigneeName = repo.getUserById(newTask.assignedTo)?.name ?: "Someone"
        repo.addNotification(AppNotification(
            id = UUID.randomUUID().toString(), userId = newTask.assignedTo,
            title = "Recurring Task Assigned",
            message = "Your turn: \"${newTask.title}\" has been assigned to you.",
            isRead = false, createdAt = System.currentTimeMillis()
        ))
        val adminId = group.adminId
        if (newTask.assignedTo != adminId) {
            repo.addNotification(AppNotification(
                id = UUID.randomUUID().toString(), userId = adminId,
                title = "Recurring Task Reassigned",
                message = "\"${newTask.title}\" auto-assigned to $assigneeName.",
                isRead = false, createdAt = System.currentTimeMillis()
            ))
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
                repo.penalizeOverdue(task.assignedTo, task.groupId)
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
