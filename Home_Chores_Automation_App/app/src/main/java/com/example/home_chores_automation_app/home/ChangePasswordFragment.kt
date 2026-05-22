package com.example.home_chores_automation_app.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.home_chores_automation_app.R
import com.example.home_chores_automation_app.data.prefs.SessionManager
import com.example.home_chores_automation_app.databinding.FragmentChangePasswordBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChangePasswordFragment : Fragment() {

    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()

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

        binding.etOldPassword.addTextChangedListener     { updateChangeButton() }
        binding.etNewPassword.addTextChangedListener     { updateChangeButton() }
        binding.etConfirmPassword.addTextChangedListener { updateChangeButton() }

        binding.btnChangePassword.setOnClickListener { changePassword() }
        binding.btnCancel.setOnClickListener { findNavController().popBackStack() }
        binding.btnBack.setOnClickListener   { findNavController().popBackStack() }

        updateChangeButton()
    }

    private fun validateNewPassword(): Boolean {
        val newPwd = binding.etNewPassword.text.toString()
        return if (newPwd.length < 6) {
            binding.tilNewPassword.error = "Password must be at least 6 characters"; false
        } else { binding.tilNewPassword.error = null; true }
    }

    private fun validateConfirmPassword(): Boolean {
        val newPwd     = binding.etNewPassword.text.toString()
        val confirmPwd = binding.etConfirmPassword.text.toString()
        return if (newPwd != confirmPwd) {
            binding.tilConfirmPassword.error = "Passwords do not match"; false
        } else { binding.tilConfirmPassword.error = null; true }
    }

    private fun updateChangeButton() {
        binding.btnChangePassword.isEnabled =
            binding.etOldPassword.text?.isNotEmpty() == true &&
            validateNewPassword() && validateConfirmPassword()
    }

    private fun changePassword() {
        val oldPassword = binding.etOldPassword.text.toString()
        val newPassword = binding.etNewPassword.text.toString()
        val firebaseUser = auth.currentUser ?: return
        val email = firebaseUser.email ?: return

        binding.btnChangePassword.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Re-authenticate to confirm old password before changing
                val credential = EmailAuthProvider.getCredential(email, oldPassword)
                firebaseUser.reauthenticate(credential).await()
                firebaseUser.updatePassword(newPassword).await()
                Toast.makeText(requireContext(), "Password changed successfully", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } catch (e: Exception) {
                binding.tilOldPassword.error = "Old password is incorrect"
                binding.btnChangePassword.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
