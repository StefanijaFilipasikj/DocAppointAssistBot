package mk.ukim.finki.docappointassistbot

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.*
import mk.ukim.finki.docappointassistbot.adapter.DoctorsAdapter
import mk.ukim.finki.docappointassistbot.databinding.FragmentDoctorsBinding
import mk.ukim.finki.docappointassistbot.domain.Doctor

class DoctorsFragment : Fragment() {

    private var _binding: FragmentDoctorsBinding? = null
    private val binding get() = _binding!!

    private lateinit var doctors: ArrayList<Doctor>
    private lateinit var firebaseRef: DatabaseReference
    private lateinit var doctorListener: ValueEventListener
    private var isListenerAttached = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoctorsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseRef = FirebaseDatabase
            .getInstance("https://docappointassistbot-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("doctors")

        doctors = arrayListOf()

        binding.doctors.layoutManager = LinearLayoutManager(context)

        fetchData()
    }

    private fun fetchData() {
        doctorListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                doctors.clear()
                if (snapshot.exists()) {
                    for (doctorSnap in snapshot.children) {
                        val doctor = doctorSnap.getValue(Doctor::class.java)
                        if (doctor != null) {
                            doctors.add(doctor)
                            Log.d("DoctorsFragment", "Doctor fetched: $doctor")
                        }
                    }
                }
                val adapter = DoctorsAdapter(doctors) { selectedDoctor ->
                    val fragment = DoctorDetailsFragment.newInstance(selectedDoctor.id)
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.frameLayout, fragment)
                        .addToBackStack(null)
                        .commit()
                }
                binding.doctors.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error: $error", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("DoctorsFragment", "onCancelled called but fragment not attached: ${error.message}")
                }
            }
        }

        firebaseRef.addValueEventListener(doctorListener)
        isListenerAttached = true
    }

    override fun onDestroyView() {
        if (isListenerAttached) {
            firebaseRef.removeEventListener(doctorListener)
            isListenerAttached = false
        }
        _binding = null
        super.onDestroyView()
    }
}
