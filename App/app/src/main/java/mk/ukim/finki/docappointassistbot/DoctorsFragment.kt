package mk.ukim.finki.docappointassistbot

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import mk.ukim.finki.docappointassistbot.adapter.DoctorsAdapter
import mk.ukim.finki.docappointassistbot.databinding.FragmentDoctorsBinding
import mk.ukim.finki.docappointassistbot.domain.Doctor

class DoctorsFragment : Fragment() {

    private var _binding: FragmentDoctorsBinding? = null
    private val binding get() = _binding!!

    private lateinit var doctors: ArrayList<Doctor>
    private lateinit var firebaseRef: DatabaseReference

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
        fetchData()

        binding.doctors.layoutManager = LinearLayoutManager(context)
    }

    private fun fetchData() {
        firebaseRef.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot){
                doctors.clear()
                if (snapshot.exists()){
                    for (doctorSnap in snapshot.children){
                        val doctor = doctorSnap.getValue(Doctor::class.java)
                        doctors.add(doctor!!)
                        Log.d("DoctorsFragment", "Doctor fetched: $doctor")
                    }
                }
                val adapter = DoctorsAdapter(doctors)
                binding.doctors.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "error: $error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}