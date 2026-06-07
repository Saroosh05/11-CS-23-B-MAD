package com.example.home_chores_automation_app.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.home_chores_automation_app.R
import com.example.home_chores_automation_app.data.prefs.SessionManager
import com.example.home_chores_automation_app.data.repository.FirebaseRepository
import com.example.home_chores_automation_app.databinding.FragmentMyGroupsBinding
import kotlinx.coroutines.launch

class MyGroupsFragment : Fragment() {

    private var _binding: FragmentMyGroupsBinding? = null
    private val binding get() = _binding!!

    private val repo = FirebaseRepository.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyGroupsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvGroups.layoutManager = LinearLayoutManager(requireContext())
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnAddGroup.setOnClickListener {
            findNavController().navigate(R.id.action_myGroups_to_createGroup)
        }
    }

    override fun onResume() {
        super.onResume()
        loadGroups()
    }

    private fun loadGroups() {
        val userId = SessionManager(requireContext()).getCurrentUserId() ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            val groups = repo.getGroupsForUser(userId)
            if (_binding == null) return@launch

            val countLabel = if (groups.size == 1) "1 group" else "${groups.size} groups"
            binding.tvSubtitle.text = countLabel

            if (groups.isEmpty()) {
                binding.rvGroups.visibility = View.GONE
                binding.layoutEmpty.visibility = View.VISIBLE
            } else {
                binding.rvGroups.visibility = View.VISIBLE
                binding.layoutEmpty.visibility = View.GONE
                binding.rvGroups.adapter = GroupAdapter(groups) { group, cardView ->
                    val bundle = Bundle().apply { putString("groupId", group.id) }
                    val extras = FragmentNavigatorExtras(cardView to "group_card_${group.id}")
                    findNavController().navigate(
                        R.id.action_myGroups_to_groupDetail,
                        bundle,
                        null,
                        extras
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
