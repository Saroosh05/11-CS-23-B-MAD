package com.example.home_chores_automation_app.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.home_chores_automation_app.R
import com.example.home_chores_automation_app.data.prefs.SessionManager
import com.example.home_chores_automation_app.data.repository.AppRepository
import com.example.home_chores_automation_app.databinding.FragmentChangePasswordBinding

class ChangePasswordFragment : Fragment() {

    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!

    private lateinit var session: SessionManager
    private lateinit var repo: AppRepository
    private lateinit var currentUser: com.example.home_chores_automation_app.data.model.User

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        session = SessionManager(requireContext())
        repo = AppRepository.getInstance(requireContext())

        val userId = session.getCurrentUserId() ?: return
        currentUser = repo.findUserById(userId) ?: return

        // Validation listeners
        binding.etOldPassword.addTextChangedListener {
            validateOldPassword()
            updateChangeButton()
        }

        binding.etNewPassword.addTextChangedListener {
            validateNewPassword()
            validateConfirmPassword()
            updateChangeButton()
        }

        binding.etConfirmPassword.addTextChangedListener {
            validateConfirmPassword()
            updateChangeButton()
        }

        binding.btnChangePassword.setOnClickListener {
            if (validateForm()) {
                changePassword()
            }
        }

        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Initial validation
        validateForm()
    }

    private fun validateOldPassword(): Boolean {
        val oldPassword = binding.etOldPassword.text.toString()
        return if (oldPassword.isEmpty()) {
            binding.tilOldPassword.error = "Old password is required"
            false
        } else if (oldPassword != currentUser.password) {
            binding.tilOldPassword.error = "Old password is incorrect"
            false
        } else {
            binding.tilOldPassword.error = null
            true
        }
    }

    private fun validateNewPassword(): Boolean {
        val newPassword = binding.etNewPassword.text.toString()
        return if (newPassword.length < 6) {
            binding.tilNewPassword.error = "Password must be at least 6 characters"
            false
        } else {
            binding.tilNewPassword.error = null
            true
        }
    }

    private fun validateConfirmPassword(): Boolean {
        val newPassword = binding.etNewPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()
        return if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = "Confirm password is required"
            false
        } else if (newPassword != confirmPassword) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            false
        } else {
            binding.tilConfirmPassword.error = null
            true
        }
    }

    private fun validateForm(): Boolean {
        val oldValid = validateOldPassword()
        val newValid = validateNewPassword()
        val confirmValid = validateConfirmPassword()
        return oldValid && newValid && confirmValid
    }

    private fun updateChangeButton() {
        binding.btnChangePassword.isEnabled = validateForm()
    }

    private fun changePassword() {
        binding.btnChangePassword.isEnabled = false
        binding.btnChangePassword.text = "Changing..."

        val newPassword = binding.etNewPassword.text.toString()

        repo.updateUserPassword(currentUser.id, newPassword)

        Toast.makeText(requireContext(), "Password changed successfully", Toast.LENGTH_SHORT).show()

        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
