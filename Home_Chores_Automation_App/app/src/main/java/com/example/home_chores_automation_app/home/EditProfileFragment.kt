package com.example.home_chores_automation_app.home

import android.os.Bundle
import android.util.Patterns
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
import com.example.home_chores_automation_app.databinding.FragmentEditProfileBinding
import com.google.android.material.textfield.TextInputLayout

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var session: SessionManager
    private lateinit var repo: AppRepository
    private lateinit var currentUser: com.example.home_chores_automation_app.data.model.User

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
        repo = AppRepository.getInstance(requireContext())

        val userId = session.getCurrentUserId() ?: return
        currentUser = repo.findUserById(userId) ?: return

        // Pre-fill fields
        binding.etName.setText(currentUser.name)
        binding.etEmail.setText(currentUser.email)

        // Validation listeners
        binding.etName.addTextChangedListener {
            validateName()
            updateSaveButton()
        }

        binding.etEmail.addTextChangedListener {
            validateEmail()
            updateSaveButton()
        }

        binding.btnSave.setOnClickListener {
            if (validateForm()) {
                saveProfile()
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

    private fun validateName(): Boolean {
        val name = binding.etName.text.toString().trim()
        return if (name.isEmpty()) {
            binding.tilName.error = "Name is required"
            false
        } else {
            binding.tilName.error = null
            true
        }
    }

    private fun validateEmail(): Boolean {
        val email = binding.etEmail.text.toString().trim()
        return when {
            email.isEmpty() -> {
                binding.tilEmail.error = "Email is required"
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.tilEmail.error = "Invalid email format"
                false
            }
            repo.checkEmailExists(email, currentUser.id) -> {
                binding.tilEmail.error = "Email already exists"
                false
            }
            else -> {
                binding.tilEmail.error = null
                true
            }
        }
    }

    private fun validateForm(): Boolean {
        val nameValid = validateName()
        val emailValid = validateEmail()
        return nameValid && emailValid
    }

    private fun updateSaveButton() {
        binding.btnSave.isEnabled = validateForm()
    }

    private fun saveProfile() {
        binding.btnSave.isEnabled = false
        binding.btnSave.text = "Saving..."

        val updatedUser = currentUser.copy(
            name = binding.etName.text.toString().trim(),
            email = binding.etEmail.text.toString().trim()
        )

        repo.updateUserProfile(updatedUser)

        Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()

        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
