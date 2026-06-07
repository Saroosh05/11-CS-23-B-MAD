package com.example.home_chores_automation_app.home

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.home_chores_automation_app.R
import com.example.home_chores_automation_app.data.model.AppNotification
import com.example.home_chores_automation_app.data.model.Task
import com.example.home_chores_automation_app.data.prefs.SessionManager
import com.example.home_chores_automation_app.data.repository.FirebaseRepository
import com.example.home_chores_automation_app.databinding.FragmentTasksBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
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

    private var loadTasksJob: Job? = null
    private var taskUpdateInProgress = false
    private var tasksAdapter: TaskAdapter? = null
    private var swipeHelperAttached = false
    private var activeDeleteSnackbar: Snackbar? = null
    private var groupAdminId: String = ""

    override fun onResume() {
        super.onResume()
        if (!taskUpdateInProgress && activeDeleteSnackbar == null) loadTasks()
    }

    private fun loadTasks() {
        loadTasksJob?.cancel()
        loadTasksJob = viewLifecycleOwner.lifecycleScope.launch {
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
            )

            updateCountLabel(tasks)

            if (tasks.isEmpty()) {
                binding.rvTasks.visibility = View.GONE
                binding.layoutEmpty.visibility = View.VISIBLE
                tasksAdapter?.replaceAll(emptyList())
                return@launch
            }

            binding.rvTasks.visibility = View.VISIBLE
            binding.layoutEmpty.visibility = View.GONE
            val adminId = group.adminId
            groupAdminId = adminId

            val existing = tasksAdapter
            if (existing != null) {
                existing.replaceAll(tasks)
            } else {
                tasksAdapter = TaskAdapter(
                    tasks.toMutableList(), memberNames, currentUserId, adminId,
                    onCheckedChange = { previous, updated, isChecked ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            taskUpdateInProgress = true
                            try {
                                val saved = repo.handleTaskCompletionChange(previous, updated)
                                repo.updateTask(saved)
                                if (saved.isCompleted && saved.recurrence != "none") {
                                    regenerateRecurringTask(saved, adminId)
                                }
                            } catch (e: Exception) {
                                // fall through to reload
                            } finally {
                                taskUpdateInProgress = false
                                loadTasks()
                            }
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
                                viewLifecycleOwner.lifecycleScope.launch {
                                    repo.deleteTask(task.id)
                                    loadTasks()
                                }
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                )
                binding.rvTasks.adapter = tasksAdapter
                attachSwipeHelperOnce()
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
                repo.penalizeOverdue(task.assignedTo, task.groupId)  // 📉 deduct points
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
        if (newTask.assignedTo != adminId) {
            repo.addNotification(AppNotification(
                id = UUID.randomUUID().toString(), userId = adminId,
                title = "Recurring Task Reassigned",
                message = "\"${newTask.title}\" auto-assigned to $assigneeName.",
                isRead = false, createdAt = System.currentTimeMillis()
            ))
        }
    }

    private fun updateCountLabel(tasks: List<Task>) {
        val completed = tasks.count { it.isCompleted }
        binding.tvTaskCount.text = "$completed of ${tasks.size} completed"
    }

    private fun attachSwipeHelperOnce() {
        if (swipeHelperAttached) return
        swipeHelperAttached = true

        val callback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return

                val adapter = tasksAdapter
                if (adapter == null) {
                    if (pos != RecyclerView.NO_POSITION) {
                        viewHolder.itemView.post { binding.rvTasks.adapter?.notifyItemChanged(pos) }
                    }
                    return
                }

                if (direction == ItemTouchHelper.RIGHT) {
                    val task = adapter.getTasks().getOrNull(pos) ?: return
                    if (!task.isCompleted) {
                        val updated = task.copy(
                            isCompleted = true,
                            completedAt = System.currentTimeMillis()
                        )
                        adapter.updateAt(pos, updated)
                        viewLifecycleOwner.lifecycleScope.launch {
                            taskUpdateInProgress = true
                            try {
                                val saved = repo.handleTaskCompletionChange(task, updated)
                                repo.updateTask(saved)
                                if (saved.recurrence != "none") {
                                    regenerateRecurringTask(saved, groupAdminId)
                                }
                            } catch (e: Exception) {
                                // fall through to reload
                            } finally {
                                taskUpdateInProgress = false
                                loadTasks()
                            }
                        }
                        Snackbar.make(binding.root, "\"${task.title}\" marked done ✓",
                            Snackbar.LENGTH_SHORT).show()
                    } else {
                        adapter.notifyItemChanged(pos)
                    }
                } else {
                    activeDeleteSnackbar?.dismiss()
                    val task = adapter.removeAt(pos)
                    updateCountLabel(adapter.getTasks())
                    val snack = Snackbar.make(
                        binding.root,
                        "\"${task.title}\" deleted",
                        Snackbar.LENGTH_LONG
                    ).setAction("Undo") {
                        adapter.insertAt(pos, task)
                        updateCountLabel(adapter.getTasks())
                        activeDeleteSnackbar = null
                    }
                    snack.addCallback(object : Snackbar.Callback() {
                        override fun onDismissed(bar: Snackbar, event: Int) {
                            if (event != DISMISS_EVENT_ACTION) {
                                viewLifecycleOwner.lifecycleScope.launch {
                                    repo.deleteTask(task.id)
                                }
                            }
                            if (activeDeleteSnackbar === bar) activeDeleteSnackbar = null
                        }
                    })
                    activeDeleteSnackbar = snack
                    snack.show()
                }
            }

            private val bgGreen = ColorDrawable(Color.parseColor("#4CAF50"))
            private val bgRed   = ColorDrawable(Color.parseColor("#F44336"))
            private val textPaint = android.graphics.Paint().apply {
                color = Color.WHITE; textSize = 52f; isAntiAlias = true
            }

            override fun onChildDraw(c: Canvas, rv: RecyclerView,
                                     vh: RecyclerView.ViewHolder, dX: Float, dY: Float,
                                     actionState: Int, isActive: Boolean) {
                val item = vh.itemView
                val cy   = item.top + item.height / 2f
                if (dX > 0) {
                    bgGreen.setBounds(item.left, item.top, item.left + dX.toInt(), item.bottom)
                    bgGreen.draw(c)
                    c.drawText("✓", item.left + 48f, cy + 18f, textPaint)
                } else if (dX < 0) {
                    bgRed.setBounds(item.right + dX.toInt(), item.top, item.right, item.bottom)
                    bgRed.draw(c)
                    c.drawText("🗑", item.right - 110f, cy + 18f, textPaint)
                }
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isActive)
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(binding.rvTasks)
    }

    override fun onDestroyView() {
        activeDeleteSnackbar?.dismiss()
        activeDeleteSnackbar = null
        swipeHelperAttached = false
        tasksAdapter = null
        super.onDestroyView()
        _binding = null
    }
}
