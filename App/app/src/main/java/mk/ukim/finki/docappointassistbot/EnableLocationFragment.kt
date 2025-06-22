package mk.ukim.finki.docappointassistbot

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import mk.ukim.finki.docappointassistbot.databinding.FragmentEnableLocationBinding
import androidx.core.content.edit

class EnableLocationFragment : Fragment() {

    private var _binding: FragmentEnableLocationBinding? = null
    private val binding get() = _binding!!

    private val sharedPref by lazy {
        requireContext().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
    }

    private val requestLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            sharedPref.edit() { putBoolean("location_enabled", true) }
            Toast.makeText(requireContext(), "Location access enabled!", Toast.LENGTH_SHORT).show()
        } else {
            sharedPref.edit() { putBoolean("location_enabled", true) }
            Toast.makeText(requireContext(), "Location access denied!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEnableLocationBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        sharedPref.edit() { putString("last_fragment", "enableLocation") }

        binding.btnTurnOnLocation.setOnClickListener {
            requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            val intent = Intent(context, MainActivity::class.java)
            startActivity(intent)
        }

        binding.btnDoNotTurnOnLocation.setOnClickListener {
            val intent = Intent(context, MainActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
