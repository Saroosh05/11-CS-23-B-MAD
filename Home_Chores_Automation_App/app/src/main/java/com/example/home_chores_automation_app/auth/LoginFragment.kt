package com.example.home_chores_automation_app.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.home_chores_automation_app.MainActivity
import com.example.home_chores_automation_app.R
import com.example.home_chores_automation_app.data.prefs.SessionManager
import com.example.home_chores_automation_app.data.repository.AppRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginFragment : Fragment() {

    private lateinit var repo: AppRepository
    private lateinit var session: SessionManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_login, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = AppRepository.getInstance(requireContext())
        session = SessionManager(requireContext())

        val tilEmail = view.findViewById<TextInputLayout>(R.id.tilEmail)
        val tilPassword = view.findViewById<TextInputLayout>(R.id.tilPassword)
        val etEmail = view.findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = view.findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = view.findViewById<MaterialButton>(R.id.btnLogin)
        val btnGoRegister = view.findViewById<MaterialButton>(R.id.btnGoRegister)

        btnLogin.setOnClickListener {
            val email = etEmail.text?.toString()?.trim() ?: ""
            val password = etPassword.text?.toString() ?: ""
            tilEmail.error = null
            tilPassword.error = null

            if (email.isEmpty()) { tilEmail.error = "Email is required"; return@setOnClickListener }
            if (password.isEmpty()) { tilPassword.error = "Password is required"; return@setOnClickListener }

            val user = repo.findUserByEmail(email)
            if (user == null) {
                tilEmail.error = "No account with this email"
                return@setOnClickListener
            }
            if (user.password != password) {
                tilPassword.error = "Incorrect password"
                return@setOnClickListener
            }

            session.login(user.id)
            startActivity(Intent(requireContext(), MainActivity::class.java))
            requireActivity().finish()
        }

        btnGoRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
    }
}
