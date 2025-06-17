package mk.ukim.finki.docappointassistbot

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import mk.ukim.finki.docappointassistbot.databinding.ActivitySignUpBinding
import mk.ukim.finki.docappointassistbot.domain.User
import androidx.core.net.toUri

class SignUpActivity : AppCompatActivity() {

    private var _binding: ActivitySignUpBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        _binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.signUpButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            val confirmPassword = binding.confirmPasswordEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                if (password == confirmPassword) {
                    signUpUser(email, password)
                } else {
                    Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill in all fields!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.logInLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun signUpUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(binding.fullNameEditText.text.toString())
                        .setPhotoUri(binding.photoUrlEditText.text.toString().toUri())
                        .build()

                    user?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener(this) { updateProfileTask ->
                            if(updateProfileTask.isSuccessful){
                                saveUserToDatabaseIfNotExists(user)
                                Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                finish()
                            }else {
                                Toast.makeText(this, "Sign-up failed: ${updateProfileTask.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    Toast.makeText(this, "Sign-up failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun saveUserToDatabaseIfNotExists(firebaseUser: FirebaseUser?) {
        firebaseUser?.let { user ->
            val ref = FirebaseDatabase.getInstance().getReference("users").child(user.uid)

            ref.get().addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    val newUser = User(
                        id = user.uid.hashCode(),
                        email = user.email ?: "",
                        fullName = user.displayName ?: "",
                        photoUrl = user.photoUrl?.toString() ?: "",
                        dateCreated = System.currentTimeMillis(),
                        role = "patient"
                    )
                    ref.setValue(newUser)
                }
            }
        }
    }
}