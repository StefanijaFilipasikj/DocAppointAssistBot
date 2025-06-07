package mk.ukim.finki.docappointassistbot

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import mk.ukim.finki.docappointassistbot.databinding.ActivityUserInfoBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = FirebaseAuth.getInstance().currentUser
        user?.let {
            binding.email.text = it.email
            binding.username.text = it.displayName

            val creationDate = it.metadata?.creationTimestamp?.let { timestamp ->
                val date = Date(timestamp)
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                sdf.format(date)
            }
            binding.dateCreated.text = creationDate ?: "N/A"

            Glide.with(this)
                .load(it.photoUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_baseline_user_24)
                .into(binding.userIcon)
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
    }
}