package com.example.home_chores_automation_app.home

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.home_chores_automation_app.R
import com.example.home_chores_automation_app.auth.AuthActivity
import com.example.home_chores_automation_app.data.model.User
import com.example.home_chores_automation_app.data.prefs.SessionManager
import com.example.home_chores_automation_app.data.repository.FirebaseRepository
import com.example.home_chores_automation_app.databinding.FragmentProfileBinding
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var session: SessionManager
    private val repo = FirebaseRepository.getInstance()
    private var currentUser: User? = null

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadProfilePicture(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        session = SessionManager(requireContext())
        val userId = session.getCurrentUserId() ?: return

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnEditProfile.setOnClickListener { findNavController().navigate(R.id.action_profile_to_editProfile) }
        binding.btnChangePassword.setOnClickListener { findNavController().navigate(R.id.action_profile_to_changePassword) }
        binding.btnChangePicture.setOnClickListener { galleryLauncher.launch("image/*") }
        binding.btnLogout.setOnClickListener {
            session.logout()
            startActivity(Intent(requireContext(), AuthActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }

        loadProfile(userId)
    }

    private fun loadProfile(userId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            var user = repo.getUserById(userId)
            // Firestore doc missing (e.g. Auth account exists but registration failed mid-way)
            if (user == null) {
                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (firebaseUser != null) {
                    user = com.example.home_chores_automation_app.data.model.User(
                        id        = firebaseUser.uid,
                        name      = firebaseUser.displayName ?: "User",
                        email     = firebaseUser.email ?: "",
                        createdAt = System.currentTimeMillis()
                    )
                    repo.createUser(user)
                } else {
                    return@launch
                }
            }
            if (_binding == null) return@launch
            currentUser = user

            setupAvatar(user)
            binding.tvName.text = user.name
            binding.tvEmail.text = user.email
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            binding.tvMemberSince.text = dateFormat.format(Date(user.createdAt))

            val groups = repo.getGroupsForUser(userId)
            binding.tvGroupCount.text = groups.size.toString()

            val allTasks = groups.flatMap { repo.getTasksForGroup(it.id) }
            val assignedTasks = allTasks.filter { it.assignedTo == userId }
            binding.tvTaskCount.text      = assignedTasks.size.toString()
            binding.tvCompletedCount.text = assignedTasks.count { it.isCompleted }.toString()
        }
    }

    private fun setupAvatar(user: User) {
        if (user.profilePictureUrl != null) {
            binding.ivProfilePicture.visibility = View.VISIBLE
            binding.tvAvatarInitial.visibility  = View.GONE
            Glide.with(this).load(user.profilePictureUrl).circleCrop().into(binding.ivProfilePicture)
        } else {
            binding.ivProfilePicture.visibility = View.GONE
            binding.tvAvatarInitial.visibility  = View.VISIBLE
            binding.tvAvatarInitial.text = user.name.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
            try {
                binding.tvAvatarInitial.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(Color.parseColor(user.avatarColorHex))
            } catch (ignore: Exception) {}
        }
    }

    private fun uploadProfilePicture(uri: Uri) {
        val userId = session.getCurrentUserId() ?: return
        Toast.makeText(requireContext(), "Uploading...", Toast.LENGTH_SHORT).show()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val storageRef = FirebaseStorage.getInstance()
                    .reference.child("profile_pictures/$userId/${UUID.randomUUID()}.jpg")
                storageRef.putFile(uri).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()

                repo.updateProfilePictureUrl(userId, downloadUrl)
                // Update cached user so avatar reflects immediately
                currentUser = currentUser?.copy(profilePictureUrl = downloadUrl)
                currentUser?.let { setupAvatar(it) }

                Toast.makeText(requireContext(), "Profile picture updated", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to upload picture", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
