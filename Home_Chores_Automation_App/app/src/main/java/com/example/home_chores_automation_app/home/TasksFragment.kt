package com.example.home_chores_automation_app.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.home_chores_automation_app.R
import com.example.home_chores_automation_app.data.model.AppNotification
import com.example.home_chores_automation_app.data.model.Task
import com.example.home_chores_automation_app.data.prefs.SessionManager
import com.example.home_chores_automation_app.data.repository.FirebaseRepository
import com.example.home_chores_automation_app.databinding.FragmentTasksBinding
import kotlinx.coroutines.launch
import java.util.UUID

class TasksFragment : Fragment() {

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!

    private val repo = FirebaseRepository.getInstance()
    private lateinit var groupId: String
    private lateinit var currentUserId: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        groupId       = arguments?.getString("groupId") ?: return
        currentUserId = SessionManager(requireContext()).getCurrentUserId() ?: return

        binding.rvTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        viewLifecycleOwner.lifecycleScope.launch {
            val group = repo.getGroupById(groupId) ?: return@launch
            if (_binding == null) return@launch
            binding.tvGroupName.text = group.name
            if (currentUserId == group.adminId) {
                binding.fab.visibility = View.VISIBLE
            }
        }

        binding.fab.setOnClickListener {
            val bundle = Bundle().apply { putString("groupId", groupId) }
            findNavController().navigate(R.id.action_tasks_to_addTask, bundle)
        }

        loadTasks()
    }

    override fun onResume() {
        super.onResume()
        loadTasks()
    }

    private fun loadTasks() {
        viewLifecycleOwner.lifecycleScope.launch {
            val group = repo.getGroupById(groupId) ?: return@launch
            val raw   = repo.getTasksForGroup(groupId)
            if (_binding == null) return@launch

            val memberNames = group.memberIds
                .mapNotNull { repo.getUserById(it) }
                .associate { it.id to it.name }

            checkAndNotifyOverdue(raw, group.adminId, memberNames)

            val now = System.currentTimeMillis()
            val tasks = raw.sortedWith(
                compareByDescending<Task> { it.dueDate > 0L && it.dueDate < now && !it.isCompleted }
                    .thenBy { if (it.dueDate > 0L) it.dueDate else Long.MAX_VALUE }
            ).toMutableList()

            updateCountLabel(tasks)

            if (tasks.isEmpty()) {
                binding.rvTasks.visibility   = View.GONE
                binding.layoutEmpty.visibility = View.VISIBLE
            } else {
                binding.rvTasks.visibility   = View.VISIBLE
                binding.layoutEmpty.visibility = View.GONE
                binding.rvTasks.adapter = TaskAdapter(
                    tasks, memberNames, currentUserId, group.adminId,
                    onCheckedChange = { task, isChecked ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            repo.updateTask(task)
                            if (isChecked && task.recurrence != "none") {
                                view?.post { if (_binding != null) {
                                    viewLifecycleOwner.lifecycleScope.launch { regenerateRecurringTask(task, group.adminId) }
                                }}
                            }
                        }
                        updateCountLabel(tasks)
                    },
                    onEdit = { task ->
                        val bundle = Bundle().apply {
                            putString("taskId", task.id)
                            putString("groupId", groupId)
                        }
                        findNavController().navigate(R.id.action_tasks_to_editTask, bundle)
                    },
                    onDelete = { task ->
                        AlertDialog.Builder(requireContext())
                            .setTitle("Delete Task")
                            .setMessage("Delete \"${task.title}\"?")
                            .setPositiveButton("Delete") { _, _ ->
                                viewLifecycleOwner.lifecycleScope.launch {
                                    repo.deleteTask(task.id)
                                    loadTasks()
                                }
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                )
            }
        }
    }

    private suspend fun checkAndNotifyOverdue(
        tasks: List<Task>,
        adminId: String,
        memberNames: Map<String, String>
    ) {
        val now = System.currentTimeMillis()
        for (task in tasks) {
            val isOverdue = task.dueDate > 0L && task.dueDate < now && !task.isCompleted
            if (isOverdue && task.overdueNotified != true) {
                repo.markOverdueNotified(task.id)
                val assigneeName = memberNames[task.assignedTo] ?: "Someone"
                repo.addNotification(AppNotification(
                    id = UUID.randomUUID().toString(), userId = task.assignedTo,
                    title = "Task Overdue", message = "\"${task.title}\" is now overdue",
                    isRead = false, createdAt = now
                ))
                if (task.assignedTo != adminId) {
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

    private suspend fun regenerateRecurringTask(completedTask: Task, adminId: String) {
        val newTask = repo.buildRecurringTask(completedTask)
        repo.createTask(newTask)
        val assigneeName = repo.getUserById(newTask.assignedTo)?.name ?: "Someone"
        if (newTask.assignedTo != adminId) {
            repo.addNotification(AppNotification(
                id = UUID.randomUUID().toString(), userId = newTask.assignedTo,
                title = "New Recurring Task Assigned",
                message = "You have been assigned \"${newTask.title}\" (Recurring Task)",
                isRead = false, createdAt = System.currentTimeMillis()
            ))
        }
        repo.addNotification(AppNotification(
            id = UUID.randomUUID().toString(), userId = adminId,
            title = "Recurring Task Generated",
            message = "\"${newTask.title}\" has been automatically recreated and assigned to $assigneeName.",
            isRead = false, createdAt = System.currentTimeMillis()
        ))
        loadTasks()
    }

    private fun updateCountLabel(tasks: List<Task>) {
        val completed = tasks.count { it.isCompleted }
        binding.tvTaskCount.text = "$completed of ${tasks.size} completed"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
