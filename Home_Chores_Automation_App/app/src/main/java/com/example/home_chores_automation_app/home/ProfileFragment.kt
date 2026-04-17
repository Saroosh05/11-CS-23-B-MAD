package com.example.home_chores_automation_app.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.home_chores_automation_app.auth.AuthActivity
import com.example.home_chores_automation_app.data.prefs.SessionManager
import com.example.home_chores_automation_app.data.repository.AppRepository
import com.example.home_chores_automation_app.databinding.FragmentProfileBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = SessionManager(requireContext())
        val repo = AppRepository.getInstance(requireContext())
        val userId = session.getCurrentUserId() ?: return
        val user = repo.findUserById(userId) ?: return

        // Avatar initial
        binding.tvAvatarInitial.text = user.name.first().uppercaseChar().toString()

        // Info rows
        binding.tvName.text = user.name
        binding.tvEmail.text = user.email

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        binding.tvMemberSince.text = dateFormat.format(Date(user.createdAt))

        // Stats
        val groups = repo.getGroupsForUser(userId)
        binding.tvGroupCount.text = groups.size.toString()

        val allTasks = groups.flatMap { repo.getTasksForGroup(it.id) }
        val assignedTasks = allTasks.filter { it.assignedTo == userId }
        binding.tvTaskCount.text = assignedTasks.size.toString()
        binding.tvCompletedCount.text = assignedTasks.count { it.isCompleted }.toString()

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnLogout.setOnClickListener {
            session.logout()
            startActivity(Intent(requireContext(), AuthActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
