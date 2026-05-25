package com.example.home_chores_automation_app.home

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.home_chores_automation_app.data.model.AppNotification
import com.example.home_chores_automation_app.data.model.Task
import com.example.home_chores_automation_app.data.model.User
import com.example.home_chores_automation_app.data.prefs.SessionManager
import com.example.home_chores_automation_app.data.repository.FirebaseRepository
import com.example.home_chores_automation_app.databinding.FragmentAddTaskBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class AddTaskFragment : Fragment() {

    private var _binding: FragmentAddTaskBinding? = null
    private val binding get() = _binding!!

    private val repo    = FirebaseRepository.getInstance()
    private lateinit var session: SessionManager
    private lateinit var groupId: String
    private var members: List<User> = emptyList()

    private var selectedDueDate: Long = 0L
    private val dateFormatter = SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault())

    private val recurrenceOptions = listOf("None", "Daily", "Weekly", "Monthly")
    private val recurrenceValues  = listOf("none", "daily", "weekly", "monthly")

    companion object {
        private const val AUTO_ASSIGN_LABEL = "Auto-assign (Balanced)"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        session = SessionManager(requireContext())
        groupId = arguments?.getString("groupId") ?: return

        val recurrenceAdapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_dropdown_item_1line, recurrenceOptions
        )
        (binding.spinnerRecurrence as AutoCompleteTextView).setAdapter(recurrenceAdapter)
        (binding.spinnerRecurrence as AutoCompleteTextView).setText(recurrenceOptions[0], false)

        binding.etDueDate.setOnClickListener { showDatePicker() }
        binding.tilDueDate.setOnClickListener { showDatePicker() }
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        viewLifecycleOwner.lifecycleScope.launch {
            val group = repo.getGroupById(groupId) ?: return@launch
            members = group.memberIds.mapNotNull { repo.getUserById(it) }
            // First entry is the balanced auto-assign option
            val displayNames = listOf(AUTO_ASSIGN_LABEL) + members.map { it.name }
            val memberAdapter = ArrayAdapter(
                requireContext(), android.R.layout.simple_dropdown_item_1line, displayNames
            )
            (binding.spinnerAssign as AutoCompleteTextView).setAdapter(memberAdapter)
            (binding.spinnerAssign as AutoCompleteTextView).setText(displayNames[0], false)
        }

        binding.btnAddTask.setOnClickListener { addTask() }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        if (selectedDueDate > 0L) cal.timeInMillis = selectedDueDate
        DatePickerDialog(requireContext(), { _, year, month, day ->
            showTimePicker(year, month, day)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showTimePicker(year: Int, month: Int, day: Int) {
        val cal = Calendar.getInstance()
        TimePickerDialog(requireContext(), { _, hour, minute ->
            val picked = Calendar.getInstance()
            picked.set(year, month, day, hour, minute, 0)
            picked.set(Calendar.MILLISECOND, 0)
            if (picked.timeInMillis <= System.currentTimeMillis()) {
                Toast.makeText(requireContext(), "Due date must be in the future", Toast.LENGTH_SHORT).show()
                return@TimePickerDialog
            }
            selectedDueDate = picked.timeInMillis
            binding.etDueDate.setText(dateFormatter.format(picked.time))
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
    }

    private fun addTask() {
        val title       = binding.etTaskTitle.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        if (title.isEmpty()) {
            binding.tilTaskTitle.error = "Please enter a task title"
            return
        }
        binding.tilTaskTitle.error = null

        val selectedName = (binding.spinnerAssign as AutoCompleteTextView).text.toString()
        val isAutoAssign = selectedName == AUTO_ASSIGN_LABEL

        val recText    = (binding.spinnerRecurrence as AutoCompleteTextView).text.toString()
        val recIdx     = recurrenceOptions.indexOf(recText)
        val recurrence = if (recIdx >= 0) recurrenceValues[recIdx] else "none"
        val creatorId  = session.getCurrentUserId() ?: return

        binding.btnAddTask.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Resolve assignee: auto-balance picks the member with fewest pending tasks
                val assignedUserId = if (isAutoAssign) {
                    repo.getNextAssigneeByWorkload(groupId, members.map { it.id })
                        .ifEmpty { creatorId }
                } else {
                    val idx = members.indexOfFirst { it.name == selectedName }
                    if (members.isNotEmpty() && idx >= 0) members[idx].id else creatorId
                }

                val task = Task(
                    id          = UUID.randomUUID().toString(),
                    groupId     = groupId,
                    title       = title,
                    description = description,
                    assignedTo  = assignedUserId,
                    createdBy   = creatorId,
                    isCompleted = false,
                    createdAt   = System.currentTimeMillis(),
                    dueDate     = selectedDueDate,
                    recurrence  = recurrence
                )
                repo.createTask(task)

                val assigneeName = repo.getUserById(assignedUserId)?.name ?: "Someone"
                if (assignedUserId != creatorId) {
                    val creatorName = repo.getUserById(creatorId)?.name ?: "Someone"
                    repo.addNotification(AppNotification(
                        id = UUID.randomUUID().toString(), userId = assignedUserId,
                        title = "New Task Assigned",
                        message = "$creatorName assigned you \"$title\"",
                        isRead = false, createdAt = System.currentTimeMillis()
                    ))
                }
                val autoMsg = if (isAutoAssign) " (auto-balanced to $assigneeName)" else ""
                Toast.makeText(requireContext(), "Task \"$title\" added$autoMsg!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to add task", Toast.LENGTH_SHORT).show()
                binding.btnAddTask.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
