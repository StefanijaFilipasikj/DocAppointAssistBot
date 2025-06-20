package mk.ukim.finki.docappointassistbot

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import mk.ukim.finki.docappointassistbot.databinding.ActivityUserInfoBinding
import mk.ukim.finki.docappointassistbot.domain.User
import java.text.SimpleDateFormat
import java.util.*

class UserInfoActivity : AppCompatActivity() {

    private var _binding: ActivityUserInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityUserInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userId = FirebaseAuth.getInstance().currentUser?.uid

        userId?.let {
            val ref = FirebaseDatabase.getInstance().getReference("users").child(it)
            ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val user = snapshot.getValue(User::class.java)
                        user?.let { u ->
                            binding.email.text = u.email
                            binding.username.text = u.fullName

                            if (u.role == "admin" || u.role == "doctor") {
                                binding.requestDoctorButton.visibility = View.GONE
                            }

                            val creationDate = u.dateCreated?.let { timestamp ->
                                val date = Date(timestamp)
                                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                sdf.format(date)
                            }
                            binding.dateCreated.text = creationDate ?: "N/A"

                            if (!isDestroyed && !isFinishing) {
                                Glide.with(this@UserInfoActivity)
                                    .load(u.photoUrl)
                                    .circleCrop()
                                    .placeholder(getUserPlaceholder(this@UserInfoActivity))
                                    .into(binding.userIcon)
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("UserInfoActivity", "Database read failed: ${error.toException()}")
                }
            })
        }

        binding.changeProfileButton.setOnClickListener {
            val intent = Intent(this, ChangeProfileActivity::class.java)
            startActivity(intent)
        }

        binding.signOutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        binding.requestDoctorButton.setOnClickListener {
            val requestsRef = FirebaseDatabase.getInstance().getReference("doctorRequests")

            requestsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var hasSubmittedRequest = false

                    for (child in snapshot.children) {
                        val request =
                            child.getValue(mk.ukim.finki.docappointassistbot.domain.DoctorRequest::class.java)
                        if (request != null && request.userId == userId && request.status == "Submitted") {
                            hasSubmittedRequest = true
                            break
                        }
                    }

                    if (hasSubmittedRequest) {
                        Toast.makeText(
                            this@UserInfoActivity,
                            "You have already sent a request. Wait for an answer.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        val intent =
                            Intent(this@UserInfoActivity, DoctorRequestActivity::class.java)
                        startActivity(intent)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@UserInfoActivity,
                        "Failed to check requests",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }
    }

    private fun getUserPlaceholder(context: Context): Int {
        val isDarkMode = (context.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES

        return if (isDarkMode) {
            R.drawable.ic_baseline_user_24_white
        } else {
            R.drawable.ic_baseline_user_24
        }
    }
}
