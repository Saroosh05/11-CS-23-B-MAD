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
import com.example.home_chores_automation_app.databinding.FragmentEditTaskBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class EditTaskFragment : Fragment() {

    private var _binding: FragmentEditTaskBinding? = null
    private val binding get() = _binding!!

    private val repo = FirebaseRepository.getInstance()
    private lateinit var session: SessionManager
    private lateinit var taskId: String
    private lateinit var groupId: String
    private var members: List<User> = emptyList()
    private var currentTask: Task? = null

    private var selectedDueDate: Long = 0L
    private val dateFormatter = SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault())

    private val recurrenceOptions = listOf("None", "Daily", "Weekly", "Monthly")
    private val recurrenceValues  = listOf("none", "daily", "weekly", "monthly")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        session = SessionManager(requireContext())
        taskId  = arguments?.getString("taskId")  ?: return
        groupId = arguments?.getString("groupId") ?: return

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        val recurrenceAdapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_dropdown_item_1line, recurrenceOptions
        )
        (binding.spinnerRecurrence as AutoCompleteTextView).setAdapter(recurrenceAdapter)

        binding.etDueDate.setOnClickListener { showDatePicker() }
        binding.tilDueDate.setOnClickListener { showDatePicker() }

        viewLifecycleOwner.lifecycleScope.launch {
            val group = repo.getGroupById(groupId) ?: return@launch
            val task  = repo.getTasksForGroup(groupId).find { it.id == taskId } ?: return@launch
            if (_binding == null) return@launch

            currentTask = task
            members = group.memberIds.mapNotNull { repo.getUserById(it) }
            binding.tvTaskTitle.text = task.title

            val memberNames = members.map { it.name }
            val memberAdapter = ArrayAdapter(
                requireContext(), android.R.layout.simple_dropdown_item_1line, memberNames
            )
            (binding.spinnerAssign as AutoCompleteTextView).setAdapter(memberAdapter)
            val idx = members.indexOfFirst { it.id == task.assignedTo }
            (binding.spinnerAssign as AutoCompleteTextView).setText(
                if (idx >= 0) memberNames[idx] else memberNames.firstOrNull() ?: "", false
            )

            val recIdx = recurrenceValues.indexOf(task.recurrence).takeIf { it >= 0 } ?: 0
            (binding.spinnerRecurrence as AutoCompleteTextView).setText(recurrenceOptions[recIdx], false)

            selectedDueDate = task.dueDate
            if (selectedDueDate > 0L) binding.etDueDate.setText(dateFormatter.format(selectedDueDate))

            binding.btnSave.setOnClickListener {
                val selectedName  = (binding.spinnerAssign as AutoCompleteTextView).text.toString()
                val selectedIndex = members.indexOfFirst { it.name == selectedName }
                if (members.isEmpty() || selectedIndex < 0) {
                    Toast.makeText(requireContext(), "Please select a member", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val newAssignee    = members[selectedIndex]
                val oldAssigneeId  = task.assignedTo
                val recText        = (binding.spinnerRecurrence as AutoCompleteTextView).text.toString()
                val recurrenceIdx  = recurrenceOptions.indexOf(recText)
                val recurrence     = if (recurrenceIdx >= 0) recurrenceValues[recurrenceIdx] else "none"
                val updatedTask    = task.copy(assignedTo = newAssignee.id, dueDate = selectedDueDate, recurrence = recurrence)
                val adminId        = session.getCurrentUserId() ?: return@setOnClickListener

                binding.btnSave.isEnabled = false
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        repo.updateTask(updatedTask)
                        val adminName = repo.getUserById(adminId)?.name ?: "Admin"
                        if (newAssignee.id != adminId) {
                            repo.addNotification(AppNotification(
                                id = UUID.randomUUID().toString(), userId = newAssignee.id,
                                title = "Task Reassigned",
                                message = "$adminName reassigned \"${task.title}\" to you",
                                isRead = false, createdAt = System.currentTimeMillis()
                            ))
                        }
                        if (oldAssigneeId != newAssignee.id && oldAssigneeId != adminId) {
                            repo.addNotification(AppNotification(
                                id = UUID.randomUUID().toString(), userId = oldAssigneeId,
                                title = "Task Reassigned",
                                message = "$adminName reassigned \"${task.title}\" to someone else",
                                isRead = false, createdAt = System.currentTimeMillis()
                            ))
                        }
                        Toast.makeText(requireContext(), "Task updated!", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Failed to update task", Toast.LENGTH_SHORT).show()
                        binding.btnSave.isEnabled = true
                    }
                }
            }
        }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
