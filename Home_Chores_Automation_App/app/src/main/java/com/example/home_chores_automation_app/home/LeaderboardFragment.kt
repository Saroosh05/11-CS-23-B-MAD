package com.example.home_chores_automation_app.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.home_chores_automation_app.data.repository.FirebaseRepository
import com.example.home_chores_automation_app.databinding.FragmentLeaderboardBinding
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class LeaderboardFragment : Fragment() {

    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding!!

    private val repo = FirebaseRepository.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLeaderboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val groupId = arguments?.getString("groupId") ?: return
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.rvLeaderboard.layoutManager = LinearLayoutManager(requireContext())

        loadLeaderboard(groupId)
    }

    private fun loadLeaderboard(groupId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val group = repo.getGroupById(groupId) ?: return@launch
            if (_binding == null) return@launch

            binding.tvGroupName.text = group.name

            val statsList = repo.getLeaderboardForGroup(groupId, group.memberIds)
            // getUserById hits the in-memory cache for members already loaded this session
            val entries = coroutineScope {
                statsList.mapIndexed { index, stats ->
                    async {
                        val user = repo.getUserById(stats.userId) ?: return@async null
                        LeaderboardEntry(rank = index + 1, user = user, stats = stats)
                    }
                }.awaitAll()
            }.filterNotNull()

            if (entries.isEmpty()) {
                binding.tvEmpty.visibility      = View.VISIBLE
                binding.rvLeaderboard.visibility = View.GONE
            } else {
                binding.tvEmpty.visibility      = View.GONE
                binding.rvLeaderboard.visibility = View.VISIBLE
                binding.rvLeaderboard.adapter   = LeaderboardAdapter(entries)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
