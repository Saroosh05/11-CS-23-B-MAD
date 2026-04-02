package com.example.home_chores_automation_app.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.home_chores_automation_app.data.prefs.SessionManager
import com.example.home_chores_automation_app.data.repository.AppRepository
import com.example.home_chores_automation_app.databinding.FragmentJoinGroupBinding

class JoinGroupFragment : Fragment() {

    private var _binding: FragmentJoinGroupBinding? = null
    private val binding get() = _binding!!

    private lateinit var repo: AppRepository
    private lateinit var session: SessionManager

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

        repo = AppRepository.getInstance(requireContext())
        session = SessionManager(requireContext())

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnJoin.setOnClickListener {
            joinGroup()
        }
    }

    private fun joinGroup() {
        val code = binding.etInviteCode.text.toString().trim().uppercase()

        if (code.length != 6) {
            binding.tilInviteCode.error = "Invite code must be 6 characters"
            return
        }
        binding.tilInviteCode.error = null

        val group = repo.findGroupByInviteCode(code)
        if (group == null) {
            binding.tilInviteCode.error = "No group found with this invite code"
            return
        }

        val userId = session.getCurrentUserId() ?: return

        if (group.memberIds.contains(userId)) {
            Toast.makeText(requireContext(), "You are already a member of \"${group.name}\"", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        group.memberIds.add(userId)
        repo.updateGroup(group)

        Toast.makeText(requireContext(), "Joined \"${group.name}\" successfully!", Toast.LENGTH_SHORT).show()
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
