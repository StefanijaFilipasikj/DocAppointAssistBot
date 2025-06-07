package mk.ukim.finki.docappointassistbot

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import mk.ukim.finki.docappointassistbot.databinding.ActivityChangeProfileBinding

class ChangeProfileActivity : AppCompatActivity() {

    private var _binding: ActivityChangeProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        _binding = ActivityChangeProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val user = FirebaseAuth.getInstance().currentUser

        binding.fullNameEditText.setText(user?.displayName.toString())
        if (user?.photoUrl != null)
            binding.photoUrlEditText.setText(user.photoUrl.toString())

        binding.saveChanges.setOnClickListener{
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(binding.fullNameEditText.text.toString())
                .setPhotoUri(Uri.parse(binding.photoUrlEditText.text.toString()))
                .build()

            user?.updateProfile(profileUpdates)
                ?.addOnCompleteListener(this) { updateProfileTask ->
                    if(updateProfileTask.isSuccessful){
                        Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        invalidateOptionsMenu()
                        finish()
                    }else {
                        Toast.makeText(this, "Action failed: ${updateProfileTask.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}