package com.example.home_chores_automation_app

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.example.home_chores_automation_app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.main_nav_host) as NavHostFragment
        val navController = navHostFragment.navController

        NavigationUI.setupWithNavController(binding.bottomNavView, navController)

        // Top-level destinations show the bottom nav; all sub-screens hide it
        val topLevelDestinations = setOf(
            R.id.homeFragment,
            R.id.calendarFragment,
            R.id.notificationsFragment,
            R.id.profileFragment
        )

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNavView.visibility =
                if (destination.id in topLevelDestinations) View.VISIBLE else View.GONE
        }
    }
}