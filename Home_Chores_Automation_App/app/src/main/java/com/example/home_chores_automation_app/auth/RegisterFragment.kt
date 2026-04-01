package com.example.home_chores_automation_app.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.home_chores_automation_app.MainActivity
import com.example.home_chores_automation_app.R
import com.example.home_chores_automation_app.data.model.User
import com.example.home_chores_automation_app.data.prefs.SessionManager
import com.example.home_chores_automation_app.data.repository.AppRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class RegisterFragment : Fragment() {

    private val avatarColors = listOf(
        "#FF6B35", "#1A73E8", "#0F9D58", "#F4B400",
        "#AB47BC", "#00ACC1", "#E53935", "#43A047"
    )

    private lateinit var repo: AppRepository
    private lateinit var session: SessionManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_register, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = AppRepository.getInstance(requireContext())
        session = SessionManager(requireContext())

        val tilName = view.findViewById<TextInputLayout>(R.id.tilName)
        val tilEmail = view.findViewById<TextInputLayout>(R.id.tilEmail)
        val tilPassword = view.findViewById<TextInputLayout>(R.id.tilPassword)
        val tilConfirm = view.findViewById<TextInputLayout>(R.id.tilConfirmPassword)
        val etName = view.findViewById<TextInputEditText>(R.id.etName)
        val etEmail = view.findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = view.findViewById<TextInputEditText>(R.id.etPassword)
        val etConfirm = view.findViewById<TextInputEditText>(R.id.etConfirmPassword)
        val btnRegister = view.findViewById<MaterialButton>(R.id.btnRegister)
        val btnGoLogin = view.findViewById<MaterialButton>(R.id.btnGoLogin)

        btnRegister.setOnClickListener {
            val name = etName.text?.toString()?.trim() ?: ""
            val email = etEmail.text?.toString()?.trim() ?: ""
            val password = etPassword.text?.toString() ?: ""
            val confirm = etConfirm.text?.toString() ?: ""

            listOf(tilName, tilEmail, tilPassword, tilConfirm).forEach { it.error = null }

            if (name.isEmpty()) { tilName.error = "Name is required"; return@setOnClickListener }
            if (email.isEmpty()) { tilEmail.error = "Email is required"; return@setOnClickListener }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.error = "Enter a valid email"; return@setOnClickListener
            }
            if (password.length < 6) { tilPassword.error = "Password must be at least 6 characters"; return@setOnClickListener }
            if (password != confirm) { tilConfirm.error = "Passwords do not match"; return@setOnClickListener }
            if (repo.findUserByEmail(email) != null) { tilEmail.error = "Account already exists"; return@setOnClickListener }

            val colorIdx = repo.getAllUsers().size % avatarColors.size
            val user = User(
                name = name,
                email = email,
                password = password,
                avatarColorHex = avatarColors[colorIdx]
            )
            repo.createUser(user)
            session.login(user.id)
            startActivity(Intent(requireContext(), MainActivity::class.java))
            requireActivity().finish()
        }

        btnGoLogin.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }
    }
}
