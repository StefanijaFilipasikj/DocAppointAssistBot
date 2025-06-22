package mk.ukim.finki.docappointassistbot

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.viewpager2.widget.ViewPager2
import mk.ukim.finki.docappointassistbot.databinding.FragmentLoadingBinding

class LoadingFragment : Fragment() {

    private var _binding: FragmentLoadingBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoadingBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        sharedPref.edit() { putString("last_fragment", "loading") }

        sharedPrefs = requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)

        val isFirstLaunch = sharedPrefs.getBoolean("first_launch", true)
        // if you want to see onboarding screens all the time use this instead: val isFirstLaunch = true

        if (!isFirstLaunch) {
            startActivity(Intent(requireContext(), MainActivity::class.java))
            requireActivity().finish()
        } else {
            binding.btnContinue.setOnClickListener {
                sharedPrefs.edit().putBoolean("first_launch", false).apply()
                (activity as? OnboardingActivity)?.findViewById<ViewPager2>(R.id.viewPager)?.currentItem = 1
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
