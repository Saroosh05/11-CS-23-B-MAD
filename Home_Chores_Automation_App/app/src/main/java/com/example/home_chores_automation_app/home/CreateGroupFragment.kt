package com.example.home_chores_automation_app.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.home_chores_automation_app.data.model.Group
import com.example.home_chores_automation_app.data.prefs.SessionManager
import com.example.home_chores_automation_app.data.repository.AppRepository
import com.example.home_chores_automation_app.databinding.FragmentCreateGroupBinding
import com.google.android.material.button.MaterialButton
import java.util.UUID

class CreateGroupFragment : Fragment() {

    private var _binding: FragmentCreateGroupBinding? = null
    private val binding get() = _binding!!

    private lateinit var repo: AppRepository
    private lateinit var session: SessionManager
    private var selectedType: String = "Home"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repo = AppRepository.getInstance(requireContext())
        session = SessionManager(requireContext())

        // Default selection
        setTypeSelected(binding.btnTypeHome)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Type selection buttons
        val typeButtons = listOf(
            binding.btnTypeHome to "Home",
            binding.btnTypeOffice to "Office",
            binding.btnTypeHostel to "Hostel",
            binding.btnTypeOther to "Other"
        )
        for ((btn, type) in typeButtons) {
            btn.setOnClickListener {
                selectedType = type
                typeButtons.forEach { (b, _) -> setTypeDeselected(b) }
                setTypeSelected(btn)
            }
        }

        binding.btnCreate.setOnClickListener {
            createGroup()
        }
    }

    private fun setTypeSelected(button: MaterialButton) {
        button.setBackgroundColor(
            resources.getColor(com.example.home_chores_automation_app.R.color.primary, null)
        )
        button.setTextColor(
            resources.getColor(com.example.home_chores_automation_app.R.color.white, null)
        )
    }

    private fun setTypeDeselected(button: MaterialButton) {
        button.setBackgroundColor(
            resources.getColor(android.R.color.transparent, null)
        )
        button.setTextColor(
            resources.getColor(com.example.home_chores_automation_app.R.color.primary, null)
        )
    }

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    private fun createGroup() {
        val name = binding.etGroupName.text.toString().trim()

        if (name.isEmpty()) {
            binding.tilGroupName.error = "Please enter a group name"
            return
        }
        binding.tilGroupName.error = null

        val userId = session.getCurrentUserId() ?: return

        val group = Group(
            id = UUID.randomUUID().toString(),
            name = name,
            type = selectedType,
            adminId = userId,
            memberIds = mutableListOf(userId),
            inviteCode = generateInviteCode(),
            createdAt = System.currentTimeMillis()
        )

        repo.createGroup(group)

        Toast.makeText(requireContext(), "Group \"$name\" created!", Toast.LENGTH_SHORT).show()
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
