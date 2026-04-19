package com.example.home_chores_automation_app.home

import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.home_chores_automation_app.R
import com.example.home_chores_automation_app.data.model.User
import com.example.home_chores_automation_app.data.prefs.SessionManager
import com.example.home_chores_automation_app.data.repository.AppRepository
import com.example.home_chores_automation_app.databinding.FragmentHomeBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
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

        binding.cardNewGroup.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_createGroup)
        }

        binding.cardJoinGroup.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_joinGroup)
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

        // Avatar
        setupAvatar(user)

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

        // Notification badge on the bottom nav Alerts item
        val unreadCount = repo.getUnreadCount(userId)
        val navView = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavView)
        if (unreadCount > 0) {
            val badge = navView?.getOrCreateBadge(R.id.notificationsFragment)
            badge?.isVisible = true
            badge?.number = unreadCount
        } else {
            navView?.removeBadge(R.id.notificationsFragment)
        }
    }

    private fun setupAvatar(user: User?) {
        if (user?.profilePictureBase64 != null) {
            try {
                val bytes = Base64.decode(user.profilePictureBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                binding.ivHomeAvatar.setImageBitmap(bitmap)
                binding.ivHomeAvatar.visibility = View.VISIBLE
                binding.tvAvatarInitial.visibility = View.GONE
                return
            } catch (ignore: Exception) { /* fall through to initial */ }
        }
        binding.ivHomeAvatar.visibility = View.GONE
        binding.tvAvatarInitial.visibility = View.VISIBLE
        binding.tvAvatarInitial.text = user?.name?.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
        try {
            binding.tvAvatarInitial.backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    Color.parseColor(user?.avatarColorHex ?: "#00897B")
                )
        } catch (ignore: Exception) {}
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
