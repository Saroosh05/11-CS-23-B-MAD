package com.example.home_chores_automation_app.home

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.home_chores_automation_app.R
import com.example.home_chores_automation_app.data.model.User
import com.example.home_chores_automation_app.data.prefs.SessionManager
import com.example.home_chores_automation_app.data.repository.FirebaseRepository
import com.example.home_chores_automation_app.databinding.FragmentHomeBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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

        binding.rvGroups.layoutManager = LinearLayoutManager(requireContext())

        binding.cardNewGroup.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_createGroup)
        }

        binding.cardJoinGroup.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_joinGroup)
        }

        binding.cardStatGroups.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_myGroups)
        }

        binding.cardStatPending.setOnClickListener {
            val bundle = Bundle().apply { putString("filter", "pending") }
            findNavController().navigate(R.id.action_home_to_myTasks, bundle)
        }

        binding.cardStatDone.setOnClickListener {
            val bundle = Bundle().apply { putString("filter", "done") }
            findNavController().navigate(R.id.action_home_to_myTasks, bundle)
        }

        refreshDashboard()
    }

    override fun onResume() {
        super.onResume()
        refreshDashboard()
    }

    private fun refreshDashboard() {
        val session = SessionManager(requireContext())
        val repo    = FirebaseRepository.getInstance()
        val userId  = session.getCurrentUserId() ?: return

        // Time-based greeting
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        binding.tvGreeting.text = when {
            hour < 12 -> "Good morning,"
            hour < 18 -> "Good afternoon,"
            else      -> "Good evening,"
        }
        binding.tvDate.text = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()).format(Date())

        viewLifecycleOwner.lifecycleScope.launch {
            // Fetch user, groups and unread count all at once
            val userDeferred    = async { repo.getUserById(userId) }
            val groupsDeferred  = async { repo.getGroupsForUser(userId) }
            val unreadDeferred  = async { repo.getUnreadCount(userId) }
            val user            = userDeferred.await()
            val groups          = groupsDeferred.await()

            if (_binding == null) return@launch

            binding.tvUserName.text = "${user?.name ?: "User"}!"
            setupAvatar(user)

            binding.tvStatGroups.text = groups.size.toString()
            val groupWord = if (groups.size == 1) "group" else "groups"
            binding.tvGroupCount.text = "${groups.size} $groupWord"

            // Fetch tasks for all groups in parallel
            val myTasks = coroutineScope {
                groups.map { async { repo.getTasksForGroup(it.id) } }.awaitAll()
            }.flatten().filter { it.assignedTo == userId }
            binding.tvStatPending.text = myTasks.count { !it.isCompleted }.toString()
            binding.tvStatDone.text    = myTasks.count { it.isCompleted }.toString()

            if (groups.isEmpty()) {
                binding.rvGroups.visibility   = View.GONE
                binding.layoutEmpty.visibility = View.VISIBLE
            } else {
                binding.rvGroups.visibility   = View.VISIBLE
                binding.layoutEmpty.visibility = View.GONE
                binding.rvGroups.adapter = GroupAdapter(groups) { group, cardView ->
                    val bundle = android.os.Bundle().apply { putString("groupId", group.id) }
                    val extras = FragmentNavigatorExtras(cardView to "group_card_${group.id}")
                    findNavController().navigate(R.id.action_home_to_groupDetail, bundle, null, extras)
                }
            }

            val unreadCount = unreadDeferred.await()
            val navView = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavView)
            if (unreadCount > 0) {
                val badge = navView?.getOrCreateBadge(R.id.notificationsFragment)
                badge?.isVisible = true
                badge?.number = unreadCount
            } else {
                navView?.removeBadge(R.id.notificationsFragment)
            }
        }
    }

    private fun setupAvatar(user: User?) {
        if (user?.profilePictureUrl != null) {
            binding.ivHomeAvatar.visibility  = View.VISIBLE
            binding.tvAvatarInitial.visibility = View.GONE
            Glide.with(this).load(user.profilePictureUrl).circleCrop().into(binding.ivHomeAvatar)
            return
        }
        binding.ivHomeAvatar.visibility  = View.GONE
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
