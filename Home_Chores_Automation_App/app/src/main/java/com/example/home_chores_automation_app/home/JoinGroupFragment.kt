package com.example.home_chores_automation_app.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.home_chores_automation_app.data.model.AppNotification
import com.example.home_chores_automation_app.data.prefs.SessionManager
import com.example.home_chores_automation_app.data.repository.FirebaseRepository
import com.example.home_chores_automation_app.data.repository.JoinGroupResult
import com.example.home_chores_automation_app.databinding.FragmentJoinGroupBinding
import kotlinx.coroutines.launch
import java.util.UUID

class JoinGroupFragment : Fragment() {

    private var _binding: FragmentJoinGroupBinding? = null
    private val binding get() = _binding!!

    private val repo = FirebaseRepository.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJoinGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnJoin.setOnClickListener { joinGroup() }
    }

    private fun joinGroup() {
        val code = binding.etInviteCode.text.toString().trim().uppercase()

        if (code.length != 6) {
            binding.tilInviteCode.error = "Invite code must be 6 characters"
            return
        }
        binding.tilInviteCode.error = null

        val userId = SessionManager(requireContext()).getCurrentUserId() ?: return
        binding.btnJoin.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                when (val result = repo.joinGroupByInviteCode(code, userId)) {
                    is JoinGroupResult.NotFound -> {
                        binding.tilInviteCode.error = "No group found with this invite code"
                        binding.btnJoin.isEnabled = true
                    }
                    is JoinGroupResult.PermissionDenied -> {
                        Toast.makeText(
                            requireContext(),
                            "Firebase blocked this action. Ask whoever set up Firebase to update Firestore security rules.",
                            Toast.LENGTH_LONG
                        ).show()
                        binding.btnJoin.isEnabled = true
                    }
                    is JoinGroupResult.Failed -> {
                        Toast.makeText(
                            requireContext(),
                            "Could not join group. Please try again.",
                            Toast.LENGTH_LONG
                        ).show()
                        binding.btnJoin.isEnabled = true
                    }
                    is JoinGroupResult.AlreadyMember -> {
                        Toast.makeText(
                            requireContext(),
                            "You are already a member of \"${result.group.name}\"",
                            Toast.LENGTH_SHORT
                        ).show()
                        findNavController().popBackStack()
                    }
                    is JoinGroupResult.Success -> {
                        val group = result.group
                        val joinerName = repo.getUserById(userId)?.name ?: "Someone"
                        repo.addNotification(
                            AppNotification(
                                id        = UUID.randomUUID().toString(),
                                userId    = group.adminId,
                                title     = "New Member Joined",
                                message   = "$joinerName joined your group \"${group.name}\"",
                                isRead    = false,
                                createdAt = System.currentTimeMillis()
                            )
                        )
                        Toast.makeText(
                            requireContext(),
                            "Joined \"${group.name}\" successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                        findNavController().popBackStack()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to join group", Toast.LENGTH_SHORT).show()
                binding.btnJoin.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
