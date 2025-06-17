package mk.ukim.finki.docappointassistbot

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Spinner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import mk.ukim.finki.docappointassistbot.databinding.FragmentSettingsBinding
import mk.ukim.finki.docappointassistbot.utils.PermissionsUtils
import java.util.Locale
import androidx.core.content.edit

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private var checkNotificationStatus = false
    private var checkLocationStatus = false

    private val requestLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sharedPref = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)

        // Notifications
        binding.switchNotifications.setOnCheckedChangeListener(null)
        binding.switchNotifications.isChecked = areNotificationsEnabled(requireContext())
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                PermissionsUtils.checkNotificationAndAlarmPermissions(requireContext()) { dialogBuilder ->
                    dialogBuilder.show()
                }
                PermissionsUtils.requestNotificationPermission(requireActivity())
                sharedPref.edit().putBoolean("notifications_enabled", true).apply()
            } else {
                checkNotificationStatus = true
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", requireContext().packageName, null)
                }
                startActivity(intent)
            }
        }

        // Location
        binding.switchLocation.setOnCheckedChangeListener(null)
        binding.switchLocation.isChecked = isLocationEnabled(requireContext())
        binding.switchLocation.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                sharedPref.edit() { putBoolean("location_enabled", true) }
            } else {
                checkLocationStatus = true
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", requireContext().packageName, null)
                }
                startActivity(intent)
            }
        }

        // Dark mode
        val currentThemeMode = sharedPref.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_NO)
        binding.switchDarkMode.isChecked = currentThemeMode == AppCompatDelegate.MODE_NIGHT_YES
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            val newMode = if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(newMode)
            sharedPref.edit() { putInt("theme_mode", newMode) }
        }

        // Language
        val spinner: Spinner = binding.spinnerLanguage
        val savedLanguage = sharedPref.getString("language", "en")
        val selectedIndex = when (savedLanguage) {
            "en" -> 0
            "mk" -> 1
            "fr" -> 2
            else -> 0
        }
        spinner.setSelection(selectedIndex)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedLang = parent.getItemAtPosition(position).toString()

                val langCode = when (selectedLang) {
                    "EN" -> "en"
                    "MK" -> "mk"
                    "FR" -> "fr"
                    else -> "en"
                }

                if (langCode != savedLanguage) {
                    setLocale(langCode)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }


        binding.tvSignOut.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    override fun onAttach(context: Context) {
        val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val languageCode = sharedPreferences.getString("language", "en") ?: "en"

        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.create(Locale.forLanguageTag(languageCode))
        )
        super.onAttach(context)
    }


    override fun onResume() {
        super.onResume()
        val sharedPref = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)

        val notificationEnabled = areNotificationsEnabled(requireContext())
        binding.switchNotifications.isChecked = notificationEnabled
        sharedPref.edit().putBoolean("notifications_enabled", notificationEnabled).apply()

        val locationEnabled = isLocationEnabled(requireContext())
        binding.switchLocation.isChecked = locationEnabled
        sharedPref.edit() { putBoolean("location_enabled", locationEnabled) }

        checkNotificationStatus = false
        checkLocationStatus = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isSystemLocationOn =
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return isSystemLocationOn && hasPermission
    }

    private fun areNotificationsEnabled(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    private fun setLocale(language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)

        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.create(Locale.forLanguageTag(language))
        )

        val sharedPref = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        sharedPref.edit() { putString("language", language) }
        requireActivity().recreate()
    }
}