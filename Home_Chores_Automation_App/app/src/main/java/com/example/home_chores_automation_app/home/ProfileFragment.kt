package com.example.home_chores_automation_app.home

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.home_chores_automation_app.R
import com.example.home_chores_automation_app.auth.AuthActivity
import com.example.home_chores_automation_app.data.prefs.SessionManager
import com.example.home_chores_automation_app.data.repository.AppRepository
import com.example.home_chores_automation_app.databinding.FragmentProfileBinding
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var session: SessionManager
    private lateinit var repo: AppRepository
    private lateinit var currentUser: com.example.home_chores_automation_app.data.model.User

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleImageSelection(it) }
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
        repo = AppRepository.getInstance(requireContext())
        val userId = session.getCurrentUserId() ?: return
        currentUser = repo.findUserById(userId) ?: return

        setupAvatar()
        setupInfo()
        setupStats()
        setupButtons()
    }

    private fun setupAvatar() {
        binding.tvAvatarInitial.text = currentUser.name.first().uppercaseChar().toString()

        if (currentUser.profilePictureBase64 != null) {
            try {
                val decodedBytes = Base64.decode(currentUser.profilePictureBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                binding.ivProfilePicture.setImageBitmap(bitmap)
                binding.ivProfilePicture.visibility = View.VISIBLE
                binding.tvAvatarInitial.visibility = View.GONE
            } catch (e: Exception) {
                // Fallback to initial
                binding.ivProfilePicture.visibility = View.GONE
                binding.tvAvatarInitial.visibility = View.VISIBLE
            }
        } else {
            binding.ivProfilePicture.visibility = View.GONE
            binding.tvAvatarInitial.visibility = View.VISIBLE
        }
    }

    private fun setupInfo() {
        binding.tvName.text = currentUser.name
        binding.tvEmail.text = currentUser.email

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        binding.tvMemberSince.text = dateFormat.format(Date(currentUser.createdAt))
    }

    private fun setupStats() {
        val groups = repo.getGroupsForUser(currentUser.id)
        binding.tvGroupCount.text = groups.size.toString()

        val allTasks = groups.flatMap { repo.getTasksForGroup(it.id) }
        val assignedTasks = allTasks.filter { it.assignedTo == currentUser.id }
        binding.tvTaskCount.text = assignedTasks.size.toString()
        binding.tvCompletedCount.text = assignedTasks.count { it.isCompleted }.toString()
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_editProfile)
        }

        binding.btnChangePassword.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_changePassword)
        }

        binding.btnChangePicture.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.btnLogout.setOnClickListener {
            session.logout()
            startActivity(Intent(requireContext(), AuthActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    private fun handleImageSelection(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            val compressedBitmap = compressBitmap(originalBitmap, 200 * 1024) // 200KB
            val base64String = bitmapToBase64(compressedBitmap)

            repo.updateProfilePicture(currentUser.id, base64String)

            // Refresh user and avatar
            currentUser = repo.findUserById(currentUser.id) ?: currentUser
            setupAvatar()

            Toast.makeText(requireContext(), "Profile picture updated", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to update profile picture", Toast.LENGTH_SHORT).show()
        }
    }

    private fun compressBitmap(bitmap: Bitmap, maxSizeBytes: Int): Bitmap {
        var quality = 100
        var compressedBitmap = bitmap

        do {
            val outputStream = ByteArrayOutputStream()
            compressedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val byteArray = outputStream.toByteArray()
            if (byteArray.size <= maxSizeBytes) break
            quality -= 5
            if (quality < 10) {
                // Resize if quality too low
                val scale = Math.sqrt((maxSizeBytes.toDouble() / byteArray.size.toDouble()))
                val newWidth = (bitmap.width * scale).toInt()
                val newHeight = (bitmap.height * scale).toInt()
                compressedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
                quality = 100
            }
        } while (quality > 10)

        return compressedBitmap
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
