package com.example.home_chores_automation_app.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.home_chores_automation_app.R
import com.example.home_chores_automation_app.data.model.Task
import com.example.home_chores_automation_app.data.repository.AppRepository
import com.example.home_chores_automation_app.databinding.FragmentTasksBinding

class TasksFragment : Fragment() {

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!

    private lateinit var repo: AppRepository
    private lateinit var groupId: String

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

        val group = repo.findGroupById(groupId) ?: return
        binding.tvGroupName.text = group.name

        binding.rvTasks.layoutManager = LinearLayoutManager(requireContext())

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
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

        binding.tvTaskCount.text = "${tasks.size} task(s)"

        if (tasks.isEmpty()) {
            binding.rvTasks.visibility = View.GONE
            binding.layoutEmpty.visibility = View.VISIBLE
        } else {
            binding.rvTasks.visibility = View.VISIBLE
            binding.layoutEmpty.visibility = View.GONE
            binding.rvTasks.adapter = TaskAdapter(tasks, memberNames) { task, isChecked ->
                val updated = task.copy(isCompleted = isChecked)
                repo.updateTask(updated)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
