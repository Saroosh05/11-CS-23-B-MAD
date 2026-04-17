package com.example.home_chores_automation_app.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.home_chores_automation_app.R
import com.example.home_chores_automation_app.auth.AuthActivity
import com.example.home_chores_automation_app.data.prefs.SessionManager
import com.example.home_chores_automation_app.data.repository.AppRepository
import com.example.home_chores_automation_app.databinding.FragmentHomeBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = SessionManager(requireContext())
        val repo = AppRepository.getInstance(requireContext())
        val userId = session.getCurrentUserId() ?: return

        binding.rvGroups.layoutManager = LinearLayoutManager(requireContext())

        binding.btnLogout.setOnClickListener {
            session.logout()
            startActivity(Intent(requireContext(), AuthActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }

        binding.btnProfile.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_profile)
        }

        binding.btnCalendar.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_calendar)
        }

        binding.btnNotifications.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_notifications)
        }

        binding.cardNewGroup.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_createGroup)
        }

        binding.cardJoinGroup.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_joinGroup)
        }

        binding.cardSchedule.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_calendar)
        }

        binding.cardNotifications.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_notifications)
        }

        binding.cardStatPending.setOnClickListener {
            val bundle = Bundle().apply { putString("filter", "pending") }
            findNavController().navigate(R.id.action_home_to_myTasks, bundle)
        }

        binding.cardStatDone.setOnClickListener {
            val bundle = Bundle().apply { putString("filter", "done") }
            findNavController().navigate(R.id.action_home_to_myTasks, bundle)
        }

        refreshDashboard(repo, userId)
    }

    override fun onResume() {
        super.onResume()
        val session = SessionManager(requireContext())
        val repo = AppRepository.getInstance(requireContext())
        val userId = session.getCurrentUserId() ?: return
        refreshDashboard(repo, userId)
    }

    private fun refreshDashboard(repo: AppRepository, userId: String) {
        // Time-based greeting
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        binding.tvGreeting.text = when {
            hour < 12 -> "Good morning,"
            hour < 18 -> "Good afternoon,"
            else      -> "Good evening,"
        }
        val user = repo.findUserById(userId)
        binding.tvUserName.text = "${user?.name ?: "User"}!"

        // Today's date
        binding.tvDate.text = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()).format(Date())

        // Groups
        val groups = repo.getGroupsForUser(userId)
        binding.tvStatGroups.text = groups.size.toString()
        val groupWord = if (groups.size == 1) "group" else "groups"
        binding.tvGroupCount.text = "${groups.size} $groupWord"

        // Task stats (tasks assigned to this user)
        val myTasks = groups.flatMap { repo.getTasksForGroup(it.id) }.filter { it.assignedTo == userId }
        binding.tvStatPending.text = myTasks.count { !it.isCompleted }.toString()
        binding.tvStatDone.text = myTasks.count { it.isCompleted }.toString()

        // Groups list
        if (groups.isEmpty()) {
            binding.rvGroups.visibility = View.GONE
            binding.layoutEmpty.visibility = View.VISIBLE
        } else {
            binding.rvGroups.visibility = View.VISIBLE
            binding.layoutEmpty.visibility = View.GONE
            binding.rvGroups.adapter = GroupAdapter(groups) { group ->
                val bundle = android.os.Bundle().apply { putString("groupId", group.id) }
                findNavController().navigate(R.id.action_home_to_groupDetail, bundle)
            }
        }

        // Notification badge
        val unreadCount = repo.getUnreadCount(userId)
        if (unreadCount > 0) {
            binding.tvBadge.visibility = View.VISIBLE
            binding.tvBadge.text = if (unreadCount > 9) "9+" else unreadCount.toString()
        } else {
            binding.tvBadge.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
