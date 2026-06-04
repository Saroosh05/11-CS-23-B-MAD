package com.example.home_chores_automation_app.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.home_chores_automation_app.R
import com.example.home_chores_automation_app.data.prefs.SessionManager
import com.example.home_chores_automation_app.data.repository.FirebaseRepository
import com.example.home_chores_automation_app.databinding.FragmentGroupDetailBinding
import kotlinx.coroutines.launch

class GroupDetailFragment : Fragment() {

    private var _binding: FragmentGroupDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repo          = FirebaseRepository.getInstance()
        val currentUserId = SessionManager(requireContext()).getCurrentUserId() ?: return
        val groupId       = arguments?.getString("groupId") ?: return

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        viewLifecycleOwner.lifecycleScope.launch {
            val group = repo.getGroupById(groupId) ?: return@launch
            if (_binding == null) return@launch

            binding.tvGroupName.text   = group.name
            binding.tvGroupType.text   = group.type
            binding.tvInviteCode.text  = group.inviteCode
            binding.tvMemberCount.text = "${group.memberIds.size} member(s)"

            binding.btnCopyCode.setOnClickListener {
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Invite Code", group.inviteCode))
                Toast.makeText(requireContext(), "Code copied!", Toast.LENGTH_SHORT).show()
            }

            val members = group.memberIds.mapNotNull { repo.getUserById(it) }
            binding.rvMembers.layoutManager = LinearLayoutManager(requireContext())
            binding.rvMembers.adapter = MemberAdapter(members, group.adminId)

            binding.btnViewTasks.setOnClickListener {
                val bundle = Bundle().apply { putString("groupId", groupId) }
                findNavController().navigate(R.id.action_groupDetail_to_tasks, bundle)
            }

            binding.btnLeaderboard.setOnClickListener {
                val bundle = Bundle().apply { putString("groupId", groupId) }
                findNavController().navigate(R.id.action_groupDetail_to_leaderboard, bundle)
            }

            binding.btnAiInsights.setOnClickListener {
                val bundle = Bundle().apply { putString("groupId", groupId) }
                findNavController().navigate(R.id.action_groupDetail_to_aiInsights, bundle)
            }

            if (currentUserId == group.adminId) {
                binding.btnDeleteGroup.visibility = View.VISIBLE
                binding.btnDeleteGroup.setOnClickListener {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Delete Group")
                        .setMessage("Are you sure you want to delete \"${group.name}\"? This will also delete all tasks in this group.")
                        .setPositiveButton("Delete") { _, _ ->
                            viewLifecycleOwner.lifecycleScope.launch {
                                try {
                                    repo.deleteTasksForGroup(groupId)
                                    repo.deleteGroup(groupId)
                                    Toast.makeText(requireContext(), "Group deleted", Toast.LENGTH_SHORT).show()
                                    findNavController().popBackStack(R.id.homeFragment, false)
                                } catch (e: Exception) {
                                    Toast.makeText(requireContext(), "Failed to delete group", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
