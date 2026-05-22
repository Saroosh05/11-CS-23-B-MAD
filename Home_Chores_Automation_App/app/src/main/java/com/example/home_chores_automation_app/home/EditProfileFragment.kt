package com.example.home_chores_automation_app.home

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.home_chores_automation_app.R
import com.example.home_chores_automation_app.data.model.User
import com.example.home_chores_automation_app.data.prefs.SessionManager
import com.example.home_chores_automation_app.data.repository.FirebaseRepository
import com.example.home_chores_automation_app.databinding.FragmentEditProfileBinding
import kotlinx.coroutines.launch

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val repo = FirebaseRepository.getInstance()
    private lateinit var session: SessionManager
    private var currentUser: User? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        session = SessionManager(requireContext())
        val userId = session.getCurrentUserId() ?: return

        binding.btnCancel.setOnClickListener { findNavController().popBackStack() }
        binding.btnBack.setOnClickListener   { findNavController().popBackStack() }

        viewLifecycleOwner.lifecycleScope.launch {
            val user = repo.getUserById(userId) ?: return@launch
            if (_binding == null) return@launch
            currentUser = user

            binding.etName.setText(user.name)
            binding.etEmail.setText(user.email)

            binding.etName.addTextChangedListener  { updateSaveButton() }
            binding.etEmail.addTextChangedListener { updateSaveButton() }

            binding.btnSave.setOnClickListener {
                if (validateForm()) saveProfile()
            }
            updateSaveButton()
        }
    }

    private fun validateName(): Boolean {
        val name = binding.etName.text.toString().trim()
        return if (name.isEmpty()) {
            binding.tilName.error = "Name is required"; false
        } else {
            binding.tilName.error = null; true
        }
    }

    private fun validateEmail(): Boolean {
        val email = binding.etEmail.text.toString().trim()
        return when {
            email.isEmpty() -> { binding.tilEmail.error = "Email is required"; false }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> { binding.tilEmail.error = "Invalid email format"; false }
            else -> { binding.tilEmail.error = null; true }
        }
    }

    private fun validateForm() = validateName() && validateEmail()

    private fun updateSaveButton() {
        binding.btnSave.isEnabled = validateForm()
    }

    private fun saveProfile() {
        val user = currentUser ?: return
        binding.btnSave.isEnabled = false
        binding.btnSave.text = "Saving..."

        val updatedUser = user.copy(
            name  = binding.etName.text.toString().trim(),
            email = binding.etEmail.text.toString().trim()
        )

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                repo.updateUser(updatedUser)
                Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show()
                binding.btnSave.isEnabled = true
                binding.btnSave.text = "Save"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
