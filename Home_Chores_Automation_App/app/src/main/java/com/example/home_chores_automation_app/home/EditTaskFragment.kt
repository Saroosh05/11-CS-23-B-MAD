package com.example.home_chores_automation_app.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.home_chores_automation_app.data.model.AppNotification
import com.example.home_chores_automation_app.data.model.User
import com.example.home_chores_automation_app.data.prefs.SessionManager
import com.example.home_chores_automation_app.data.repository.AppRepository
import com.example.home_chores_automation_app.databinding.FragmentEditTaskBinding
import java.util.UUID

class EditTaskFragment : Fragment() {

    private var _binding: FragmentEditTaskBinding? = null
    private val binding get() = _binding!!

    private lateinit var repo: AppRepository
    private lateinit var session: SessionManager
    private lateinit var taskId: String
    private lateinit var groupId: String
    private var members: List<User> = emptyList()

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

        repo = AppRepository.getInstance(requireContext())
        session = SessionManager(requireContext())
        taskId = arguments?.getString("taskId") ?: return
        groupId = arguments?.getString("groupId") ?: return

        val task = repo.getTasksForGroup(groupId).find { it.id == taskId } ?: return
        val group = repo.findGroupById(groupId) ?: return

        binding.tvTaskTitle.text = task.title

        members = group.memberIds.mapNotNull { repo.findUserById(it) }

        val memberNames = members.map { it.name }
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            memberNames
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerAssign.adapter = spinnerAdapter

        // Pre-select current assignee
        val currentIndex = members.indexOfFirst { it.id == task.assignedTo }
        if (currentIndex >= 0) binding.spinnerAssign.setSelection(currentIndex)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnSave.setOnClickListener {
            val selectedIndex = binding.spinnerAssign.selectedItemPosition
            if (members.isEmpty() || selectedIndex < 0) {
                Toast.makeText(requireContext(), "Please select a member", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val oldAssigneeId = task.assignedTo
            val newAssignee = members[selectedIndex]
            val updatedTask = task.copy(assignedTo = newAssignee.id)
            repo.updateTask(updatedTask)

            val adminId = session.getCurrentUserId() ?: return@setOnClickListener
            val adminName = repo.findUserById(adminId)?.name ?: "Admin"

            // Notify the newly assigned user if different from admin
            if (newAssignee.id != adminId) {
                repo.addNotification(
                    AppNotification(
                        id = UUID.randomUUID().toString(),
                        userId = newAssignee.id,
                        title = "Task Reassigned",
                        message = "$adminName reassigned \"${task.title}\" to you",
                        isRead = false,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            // Notify the previous assignee if assignee changed and they are not the admin
            if (oldAssigneeId != newAssignee.id && oldAssigneeId != adminId) {
                repo.addNotification(
                    AppNotification(
                        id = UUID.randomUUID().toString(),
                        userId = oldAssigneeId,
                        title = "Task Reassigned",
                        message = "$adminName reassigned \"${task.title}\" to someone else",
                        isRead = false,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            Toast.makeText(requireContext(), "Task updated!", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
