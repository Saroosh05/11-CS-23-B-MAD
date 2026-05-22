package com.example.home_chores_automation_app.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.home_chores_automation_app.data.prefs.SessionManager
import com.example.home_chores_automation_app.data.repository.FirebaseRepository
import com.example.home_chores_automation_app.databinding.FragmentNotificationsBinding
import kotlinx.coroutines.launch

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private val repo = FirebaseRepository.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = SessionManager(requireContext()).getCurrentUserId() ?: return

        binding.rvNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        binding.btnMarkAllRead.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                repo.markAllRead(userId)
                loadNotifications(userId)
            }
        }

        loadNotifications(userId)
    }

    private fun loadNotifications(userId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val notifications = repo.getNotificationsForUser(userId)
            if (_binding == null) return@launch

            if (notifications.isEmpty()) {
                binding.rvNotifications.visibility = View.GONE
                binding.layoutEmpty.visibility     = View.VISIBLE
            } else {
                binding.rvNotifications.visibility = View.VISIBLE
                binding.layoutEmpty.visibility     = View.GONE
                binding.rvNotifications.adapter = NotificationAdapter(notifications)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
