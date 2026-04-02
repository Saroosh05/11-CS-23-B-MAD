package com.example.home_chores_automation_app.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.home_chores_automation_app.R
import com.example.home_chores_automation_app.auth.AuthActivity
import com.example.home_chores_automation_app.data.prefs.SessionManager
import com.example.home_chores_automation_app.data.repository.AppRepository
import com.example.home_chores_automation_app.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = SessionManager(requireContext())
        val repo = AppRepository.getInstance(requireContext())
        val userId = session.getCurrentUserId() ?: return

        val user = repo.findUserById(userId)
        binding.tvUserName.text = "Hello, ${user?.name ?: "User"}!"

        binding.rvGroups.layoutManager = LinearLayoutManager(requireContext())

        binding.btnLogout.setOnClickListener {
            session.logout()
            startActivity(Intent(requireContext(), AuthActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }

        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_createGroup)
        }

        loadGroups(repo, userId)
    }

    override fun onResume() {
        super.onResume()
        val session = SessionManager(requireContext())
        val repo = AppRepository.getInstance(requireContext())
        val userId = session.getCurrentUserId() ?: return
        loadGroups(repo, userId)
    }

    private fun loadGroups(repo: AppRepository, userId: String) {
        val groups = repo.getGroupsForUser(userId)

        if (groups.isEmpty()) {
            binding.rvGroups.visibility = View.GONE
            binding.layoutEmpty.visibility = View.VISIBLE
        } else {
            binding.rvGroups.visibility = View.VISIBLE
            binding.layoutEmpty.visibility = View.GONE
            binding.rvGroups.adapter = GroupAdapter(groups) { group ->
                Toast.makeText(requireContext(), "Tapped: ${group.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
