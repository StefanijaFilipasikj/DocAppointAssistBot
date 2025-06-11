package mk.ukim.finki.docappointassistbot

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
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

                        binding.fullNameEditText.setText(fullName ?: "")
                        binding.photoUrlEditText.setText(photoUrl ?: "")
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
                val currentDisplayName = currentUser.displayName
                val currentPhotoUrl = currentUser.photoUrl?.toString()

                val profileUpdatesBuilder = UserProfileChangeRequest.Builder()

                var shouldUpdateProfile = false

                if (fullName != currentDisplayName) {
                    profileUpdatesBuilder.setDisplayName(fullName)
                    shouldUpdateProfile = true
                }

                if (photoUrl != currentPhotoUrl && photoUrl.isNotBlank()) {
                    try {
                        val uri = Uri.parse(photoUrl)
                        profileUpdatesBuilder.setPhotoUri(uri)
                        shouldUpdateProfile = true
                    } catch (e: Exception) {
                        Log.w("ChangeProfileActivity", "Invalid photo URL: $photoUrl")
                    }
                }

                val updates = mapOf(
                    "fullName" to fullName,
                    "photoUrl" to photoUrl
                )

                val navigateAfterUpdate = {
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    navigateToMainActivity()
                }

                val updateDatabaseAndNavigate = {
                    database.child(currentUser.uid).updateChildren(updates)
                        .addOnCompleteListener { dbUpdateTask ->
                            if (dbUpdateTask.isSuccessful) {
                                Log.d("ChangeProfileActivity", "Database update successful.")
                                navigateAfterUpdate()
                            } else {
                                Toast.makeText(this, "Database update failed", Toast.LENGTH_LONG).show()
                            }
                        }
                }

                if (shouldUpdateProfile) {
                    val profileUpdates = profileUpdatesBuilder.build()
                    currentUser.updateProfile(profileUpdates)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d("ChangeProfileActivity", "Auth profile updated.")
                                updateDatabaseAndNavigate()
                            } else {
                                Toast.makeText(this, "Auth update failed", Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    Log.d("ChangeProfileActivity", "No auth changes detected. Skipping updateProfile.")
                    updateDatabaseAndNavigate()
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
