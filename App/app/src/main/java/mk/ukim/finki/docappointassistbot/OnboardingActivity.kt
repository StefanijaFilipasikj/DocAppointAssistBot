package mk.ukim.finki.docappointassistbot

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import mk.ukim.finki.docappointassistbot.adapter.OnboardingAdapter
import mk.ukim.finki.docappointassistbot.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {

    private lateinit var adapter: OnboardingAdapter

    private var _binding: ActivityOnboardingBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        _binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = OnboardingAdapter(this)
        binding.viewPager.adapter = adapter
    }
}