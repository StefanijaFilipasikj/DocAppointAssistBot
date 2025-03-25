package mk.ukim.finki.docappointassistbot

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import mk.ukim.finki.docappointassistbot.databinding.FragmentEnableLocationBinding

class EnableLocationFragment : Fragment() {

    private var _binding: FragmentEnableLocationBinding? = null
    private val binding get() = _binding!!

    private val requestLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(requireContext(), "Location access enabled!", Toast.LENGTH_SHORT).show()
        } else {
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
