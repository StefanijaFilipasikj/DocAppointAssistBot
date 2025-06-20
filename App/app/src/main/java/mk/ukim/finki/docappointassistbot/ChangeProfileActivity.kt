package mk.ukim.finki.docappointassistbot

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.*
import mk.ukim.finki.docappointassistbot.databinding.ActivityChangeProfileBinding
import androidx.core.net.toUri

class ChangeProfileActivity : AppCompatActivity() {

    private var _binding: ActivityChangeProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private var userRole: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        _binding = ActivityChangeProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("users")

        val user = auth.currentUser

        user?.let { currentUser ->
            val userId = currentUser.uid
            database.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!isFinishing && !isDestroyed) {
                        val fullName = snapshot.child("fullName").getValue(String::class.java)
                        val photoUrl = snapshot.child("photoUrl").getValue(String::class.java)
                        userRole = snapshot.child("role").getValue(String::class.java)

                        binding.fullNameEditText.setText(fullName ?: "")
                        binding.photoUrlEditText.setText(photoUrl ?: "")

                        if (userRole == "doctor") {
                            binding.cityEditText.visibility = View.VISIBLE
                            binding.countryEditText.visibility = View.VISIBLE
                            binding.specialtyEditText.visibility = View.VISIBLE
                            binding.bioEditText.visibility = View.VISIBLE
                            binding.experienceEditText.visibility = View.VISIBLE

                            val doctorRef = FirebaseDatabase.getInstance()
                                .getReference("doctors")
                                .child(userId)

                            doctorRef.get().addOnSuccessListener { snap ->
                                snap.child("city").getValue(String::class.java)?.let {
                                    binding.cityEditText.setText(it)
                                }
                                snap.child("country").getValue(String::class.java)?.let {
                                    binding.countryEditText.setText(it)
                                }
                                snap.child("specialty").getValue(String::class.java)?.let {
                                    binding.specialtyEditText.setText(it)
                                }
                                snap.child("bio").getValue(String::class.java)?.let {
                                    binding.bioEditText.setText(it)
                                }
                                snap.child("experience").getValue(Double::class.java)?.let {
                                    binding.experienceEditText.setText(it.toString())
                                }
                            }

                        } else {
                            binding.cityEditText.visibility = View.GONE
                            binding.countryEditText.visibility = View.GONE
                            binding.specialtyEditText.visibility = View.GONE
                            binding.bioEditText.visibility = View.GONE
                            binding.experienceEditText.visibility = View.GONE
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@ChangeProfileActivity,
                        "Failed to load profile info: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e("ChangeProfileActivity", "Database error: ${error.toException()}")
                }
            })
        }

        binding.saveChanges.setOnClickListener {
            val fullName = binding.fullNameEditText.text.toString()
            val photoUrl = binding.photoUrlEditText.text.toString()

            user?.let { currentUser ->
                val profileUpdatesBuilder = UserProfileChangeRequest.Builder()
                var shouldUpdateProfile = false

                if (fullName != currentUser.displayName) {
                    profileUpdatesBuilder.setDisplayName(fullName)
                    shouldUpdateProfile = true
                }

                if (photoUrl != currentUser.photoUrl?.toString() && photoUrl.isNotBlank()) {
                    try {
                        profileUpdatesBuilder.setPhotoUri(photoUrl.toUri())
                        shouldUpdateProfile = true
                    } catch (e: Exception) {
                        Log.w("ChangeProfileActivity", "Invalid photo URL: $photoUrl")
                    }
                }

                val userUpdates = mapOf(
                    "fullName" to fullName,
                    "photoUrl" to photoUrl
                )

                val doctorUpdates = mapOf(
                    "fullname" to fullName,
                    "image" to photoUrl,
                    "city" to binding.cityEditText.text.toString(),
                    "country" to binding.countryEditText.text.toString(),
                    "specialty" to binding.specialtyEditText.text.toString(),
                    "bio" to binding.bioEditText.text.toString(),
                    "experience" to binding.experienceEditText.text.toString().toDoubleOrNull()
                )

                val finishUpdateFlow = {
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    navigateToMainActivity()
                }

                val updateInDatabase = {
                    database.child(currentUser.uid).updateChildren(userUpdates)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                if (userRole == "doctor") {
                                    val doctorRef = FirebaseDatabase.getInstance()
                                        .getReference("doctors")
                                        .child(currentUser.uid)

                                    doctorRef.updateChildren(doctorUpdates).addOnSuccessListener {
                                        finishUpdateFlow()
                                    }.addOnFailureListener {
                                        Toast.makeText(this, "Doctor profile update failed", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    finishUpdateFlow()
                                }
                            } else {
                                Toast.makeText(this, "User profile update failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                }

                if (shouldUpdateProfile) {
                    currentUser.updateProfile(profileUpdatesBuilder.build())
                        .addOnCompleteListener {
                            updateInDatabase()
                        }
                } else {
                    updateInDatabase()
                }
            }
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
