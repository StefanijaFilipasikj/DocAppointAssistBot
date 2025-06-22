package mk.ukim.finki.docappointassistbot

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.content.pm.PackageManager
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import mk.ukim.finki.docappointassistbot.databinding.FragmentDoctorsNearYouBinding
import mk.ukim.finki.docappointassistbot.domain.Doctor
import mk.ukim.finki.docappointassistbot.utils.DoctorLocationUtil

class DoctorsNearYouFragment : Fragment() {

    private var _binding: FragmentDoctorsNearYouBinding? = null
    private val binding get() = _binding!!

    private lateinit var googleMap: GoogleMap
    private var doctors: ArrayList<Doctor> = arrayListOf()
    private lateinit var firebaseRef: DatabaseReference
    private val markerDoctorMap = mutableMapOf<String, String>()

    private val LOCATION_PERMISSION_REQUEST_CODE = 101

    private val callback = OnMapReadyCallback { map ->
        googleMap = map

        googleMap.setOnInfoWindowClickListener { marker ->
            val doctorId = markerDoctorMap[marker.id]
            if (doctorId != null) {
                val doctorDetailsFragment = DoctorDetailsFragment.newInstance(doctorId)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.frameLayout, doctorDetailsFragment)
                    .addToBackStack(null)
                    .commit()
            }
        }

        checkLocationPermissionAndFetch()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoctorsNearYouBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        sharedPref.edit() { putString("last_fragment", "doctorsNearYou") }

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)

        firebaseRef = FirebaseDatabase
            .getInstance("https://docappointassistbot-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("doctors")
        doctors = arrayListOf()
        fetchDoctors()
    }

    private fun fetchDoctors() {
        DoctorLocationUtil.fetchDoctorsWithHospitals { doctorsList ->
            doctors = ArrayList(doctorsList)
            checkLocationPermissionAndFetch()
        }
    }

    private fun checkLocationPermissionAndFetch() {
        if (ContextCompat.checkSelfPermission(requireContext(), ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getUserLocation()
        } else {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    private fun getUserLocation() {
        DoctorLocationUtil.getUserLocation(this) { location ->
            if (location != null) updateMapWithDoctors(location.latitude, location.longitude)
            else Toast.makeText(requireContext(), "No location", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateMapWithDoctors(userLat: Double, userLng: Double) {
        if (!::googleMap.isInitialized) return

        googleMap.clear()

        val userLatLng = LatLng(userLat, userLng)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 12f))
        googleMap.addMarker(MarkerOptions().position(userLatLng).title("You are here"))

        for (doctor in doctors) {
            if (DoctorLocationUtil.isDoctorNearby(userLat, userLng, doctor)) {
                val docLat = doctor.hospital?.latitude
                val docLng = doctor.hospital?.longitude

                if (docLat != null && docLng != null) {
                    val docLatLng = LatLng(docLat, docLng)
                    val marker = googleMap.addMarker(
                        MarkerOptions()
                            .position(docLatLng)
                            .title(doctor.fullname)
                            .snippet(doctor.specialty)
                    )
                    marker?.id?.let { markerId ->
                        doctor.id.let { doctorId ->
                            markerDoctorMap[markerId] = doctorId
                        }
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getUserLocation()
            } else {
                Toast.makeText(context, "Location permission is required to show nearby doctors", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
