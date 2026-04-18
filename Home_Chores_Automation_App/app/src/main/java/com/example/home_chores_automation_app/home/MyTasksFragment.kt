package com.example.home_chores_automation_app.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.home_chores_automation_app.data.model.AppNotification
import com.example.home_chores_automation_app.data.model.Task
import com.example.home_chores_automation_app.data.prefs.SessionManager
import com.example.home_chores_automation_app.data.repository.AppRepository
import com.example.home_chores_automation_app.databinding.FragmentMyTasksBinding
import java.util.UUID

class MyTasksFragment : Fragment() {

    private var _binding: FragmentMyTasksBinding? = null
    private val binding get() = _binding!!

    private lateinit var repo: AppRepository
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
        repo = AppRepository.getInstance(requireContext())
        userId = SessionManager(requireContext()).getCurrentUserId() ?: return

        binding.rvMyTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        binding.tvTitle.text = if (filter == "done") "Completed Tasks" else "Pending Tasks"

        loadTasks()
    }

    private fun loadTasks() {
        val groups = repo.getGroupsForUser(userId)

        // Collect tasks assigned to this user across all groups
        val allMyTasks = groups.flatMap { group ->
            repo.getTasksForGroup(group.id).filter { it.assignedTo == userId }
        }

        // Send one-time overdue notifications for pending tasks
        if (filter != "done") {
            checkAndNotifyOverdue(allMyTasks.filter { !it.isCompleted }, groups)
        }

        // Build member name map across all user's groups
        val memberNames = groups
            .flatMap { it.memberIds }
            .distinct()
            .mapNotNull { repo.findUserById(it) }
            .associate { it.id to it.name }

        // Apply pending / done filter
        val filtered = if (filter == "done") {
            allMyTasks.filter { it.isCompleted }
        } else {
            allMyTasks.filter { !it.isCompleted }
        }.toMutableList()

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

        // adminId = "" → edit/delete buttons stay hidden (isAdmin = false),
        // but currentUserId == task.assignedTo keeps the checkbox enabled
        binding.rvMyTasks.adapter = TaskAdapter(
            tasks = filtered,
            memberNames = memberNames,
            currentUserId = userId,
            adminId = "",
            onCheckedChange = { task, _ ->
                repo.updateTask(task)
                // post defers the adapter swap to the next frame so it never
                // conflicts with RecyclerView's ongoing touch-event processing
                view?.post { if (_binding != null) loadTasks() }
            },
            onEdit = {},
            onDelete = {}
        )
    }

    private fun checkAndNotifyOverdue(
        tasks: List<Task>,
        groups: List<com.example.home_chores_automation_app.data.model.Group>
    ) {
        val now = System.currentTimeMillis()
        val groupMap = groups.associateBy { it.id }
        tasks.forEach { task ->
            val isOverdue = task.dueDate > 0L && task.dueDate < now
            // use != true so Gson-null (old tasks without the field) is treated as false
            if (isOverdue && task.overdueNotified != true) {
                repo.markOverdueNotified(task.id)
                val adminId = groupMap[task.groupId]?.adminId ?: ""
                val assigneeName = repo.findUserById(task.assignedTo)?.name ?: "Someone"
                repo.addNotification(
                    AppNotification(
                        id = UUID.randomUUID().toString(),
                        userId = task.assignedTo,
                        title = "Task Overdue",
                        message = "Task overdue: \"${task.title}\" is now overdue",
                        isRead = false,
                        createdAt = now
                    )
                )
                if (adminId.isNotEmpty() && task.assignedTo != adminId) {
                    repo.addNotification(
                        AppNotification(
                            id = UUID.randomUUID().toString(),
                            userId = adminId,
                            title = "Overdue Alert",
                            message = "Overdue alert: \"${task.title}\" assigned to $assigneeName is overdue",
                            isRead = false,
                            createdAt = now
                        )
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
