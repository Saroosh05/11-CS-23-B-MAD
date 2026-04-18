package com.example.home_chores_automation_app.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.home_chores_automation_app.R
import com.example.home_chores_automation_app.data.model.AppNotification
import com.example.home_chores_automation_app.data.model.Task
import com.example.home_chores_automation_app.data.prefs.SessionManager
import com.example.home_chores_automation_app.data.repository.AppRepository
import com.example.home_chores_automation_app.databinding.FragmentTasksBinding
import java.util.UUID

class TasksFragment : Fragment() {

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!

    private lateinit var repo: AppRepository
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

        repo = AppRepository.getInstance(requireContext())
        groupId = arguments?.getString("groupId") ?: return
        currentUserId = SessionManager(requireContext()).getCurrentUserId() ?: return

        val group = repo.findGroupById(groupId) ?: return
        binding.tvGroupName.text = group.name

        binding.rvTasks.layoutManager = LinearLayoutManager(requireContext())

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Only admin can add tasks
        if (currentUserId == repo.findGroupById(groupId)?.adminId) {
            binding.fab.visibility = View.VISIBLE
        } else {
            binding.fab.visibility = View.GONE
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
        val group = repo.findGroupById(groupId) ?: return
        val tasks = repo.getTasksForGroup(groupId).toMutableList()

        val memberNames = group.memberIds
            .mapNotNull { repo.findUserById(it) }
            .associate { it.id to it.name }

        updateCountLabel(tasks)

        if (tasks.isEmpty()) {
            binding.rvTasks.visibility = View.GONE
            binding.layoutEmpty.visibility = View.VISIBLE
        } else {
            binding.rvTasks.visibility = View.VISIBLE
            binding.layoutEmpty.visibility = View.GONE
            binding.rvTasks.adapter = TaskAdapter(tasks, memberNames, currentUserId, group.adminId,
                onCheckedChange = { task, isChecked ->
                    repo.updateTask(task)
                    updateCountLabel(tasks)
                    if (isChecked && task.isRecurring) {
                        regenerateRecurringTask(task, group.adminId)
                    }
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
                            repo.deleteTask(task.id)
                            loadTasks()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            )
        }
    }

    private fun regenerateRecurringTask(completedTask: Task, adminId: String) {
        val newTask = completedTask.copy(
            id = UUID.randomUUID().toString(),
            isCompleted = false,
            createdAt = System.currentTimeMillis()
        )
        repo.createTask(newTask)

        val assigneeName = repo.findUserById(newTask.assignedTo)?.name ?: "Someone"

        if (newTask.assignedTo != adminId) {
            repo.addNotification(
                AppNotification(
                    id = UUID.randomUUID().toString(),
                    userId = newTask.assignedTo,
                    title = "New Recurring Task Assigned",
                    message = "You have been assigned \"${newTask.title}\" (Recurring Task)",
                    isRead = false,
                    createdAt = System.currentTimeMillis()
                )
            )
        }

        repo.addNotification(
            AppNotification(
                id = UUID.randomUUID().toString(),
                userId = adminId,
                title = "Recurring Task Generated",
                message = "Task \"${newTask.title}\" has been automatically recreated and assigned to $assigneeName.",
                isRead = false,
                createdAt = System.currentTimeMillis()
            )
        )

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
