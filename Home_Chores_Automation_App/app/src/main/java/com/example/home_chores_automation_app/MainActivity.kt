package com.example.home_chores_automation_app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.home_chores_automation_app.auth.AuthActivity
import com.example.home_chores_automation_app.data.prefs.SessionManager
import com.example.home_chores_automation_app.data.repository.AppRepository
import com.example.home_chores_automation_app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val session = SessionManager(this)
        val repo = AppRepository.getInstance(this)

        val userId = session.getCurrentUserId()
        val user = userId?.let { repo.findUserById(it) }

        binding.tvWelcome.text = "Welcome, ${user?.name ?: "User"}!"
        binding.tvEmail.text = user?.email ?: ""

        binding.btnLogout.setOnClickListener {
            session.logout()
            startActivity(
                Intent(this, AuthActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            finish()
        }
    }
}