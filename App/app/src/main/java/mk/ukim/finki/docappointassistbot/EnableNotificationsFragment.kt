package mk.ukim.finki.docappointassistbot

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.viewpager2.widget.ViewPager2
import mk.ukim.finki.docappointassistbot.databinding.FragmentEnableNotificationsBinding
import androidx.core.content.edit

class EnableNotificationsFragment : Fragment() {

    private var _binding: FragmentEnableNotificationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewPager: ViewPager2

    private val sharedPref by lazy {
        requireContext().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
    }

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        sharedPref.edit() { putBoolean("notifications_enabled", isGranted) }
        if (isGranted) {
            Toast.makeText(requireContext(), "Notifications enabled!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Notifications denied!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEnableNotificationsBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewPager = requireActivity().findViewById(R.id.viewPager)

        binding.btnTurnOnNotifications.setOnClickListener {
            requestNotificationPermission()
            viewPager.currentItem = 3
        }

        binding.btnDoNotTurnOnNotifications.setOnClickListener {
            viewPager.currentItem = 3
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            Toast.makeText(requireContext(), "Notifications enabled!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
