package com.example.home_chores_automation_app.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.home_chores_automation_app.databinding.FragmentForgotPasswordBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBackToLogin.setOnClickListener { findNavController().popBackStack() }

        binding.etEmail.addTextChangedListener {
            binding.tilEmail.error = null
            binding.btnSendReset.isEnabled = !binding.etEmail.text.isNullOrBlank()
        }
        binding.btnSendReset.isEnabled = false

        binding.btnSendReset.setOnClickListener { sendResetEmail() }
    }

    private fun sendResetEmail() {
        val email = binding.etEmail.text?.toString()?.trim() ?: ""

        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Enter a valid email address"
            return
        }

        binding.btnSendReset.isEnabled = false
        binding.btnSendReset.text = "Sending…"

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                if (_binding == null) return@launch
                Toast.makeText(
                    requireContext(),
                    "Reset link sent to $email — check your inbox",
                    Toast.LENGTH_LONG
                ).show()
                findNavController().popBackStack()
            } catch (e: Exception) {
                if (_binding == null) return@launch
                binding.tilEmail.error = "No account found with this email"
                binding.btnSendReset.isEnabled = true
                binding.btnSendReset.text = "Send Reset Link"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
