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
import com.example.home_chores_automation_app.data.model.Task
import com.example.home_chores_automation_app.data.model.User
import com.example.home_chores_automation_app.data.prefs.SessionManager
import com.example.home_chores_automation_app.data.repository.AppRepository
import com.example.home_chores_automation_app.databinding.FragmentAddTaskBinding
import java.util.UUID

class AddTaskFragment : Fragment() {

    private var _binding: FragmentAddTaskBinding? = null
    private val binding get() = _binding!!

    private lateinit var repo: AppRepository
    private lateinit var session: SessionManager
    private lateinit var groupId: String
    private var members: List<User> = emptyList()

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

        repo = AppRepository.getInstance(requireContext())
        session = SessionManager(requireContext())
        groupId = arguments?.getString("groupId") ?: return

        val group = repo.findGroupById(groupId) ?: return
        members = group.memberIds.mapNotNull { repo.findUserById(it) }

        val memberNames = members.map { it.name }
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            memberNames
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerAssign.adapter = spinnerAdapter

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnAddTask.setOnClickListener {
            addTask()
        }
    }

    private fun addTask() {
        val title = binding.etTaskTitle.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        if (title.isEmpty()) {
            binding.tilTaskTitle.error = "Please enter a task title"
            return
        }
        binding.tilTaskTitle.error = null

        val selectedIndex = binding.spinnerAssign.selectedItemPosition
        val assignedUserId = if (members.isNotEmpty() && selectedIndex >= 0) {
            members[selectedIndex].id
        } else {
            session.getCurrentUserId() ?: return
        }

        val task = Task(
            id = UUID.randomUUID().toString(),
            groupId = groupId,
            title = title,
            description = description,
            assignedTo = assignedUserId,
            createdBy = session.getCurrentUserId() ?: return,
            isCompleted = false,
            createdAt = System.currentTimeMillis(),
            isRecurring = binding.switchRecurring.isChecked
        )

        repo.createTask(task)

        // Notify the assigned user (if not the creator)
        val creatorId = session.getCurrentUserId() ?: return
        if (assignedUserId != creatorId) {
            val creatorName = repo.findUserById(creatorId)?.name ?: "Someone"
            repo.addNotification(
                AppNotification(
                    id = UUID.randomUUID().toString(),
                    userId = assignedUserId,
                    title = "New Task Assigned",
                    message = "$creatorName assigned you \"$title\"",
                    isRead = false,
                    createdAt = System.currentTimeMillis()
                )
            )
        }

        Toast.makeText(requireContext(), "Task \"$title\" added!", Toast.LENGTH_SHORT).show()
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
