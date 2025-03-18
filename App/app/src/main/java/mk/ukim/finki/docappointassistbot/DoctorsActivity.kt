package mk.ukim.finki.docappointassistbot

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import mk.ukim.finki.docappointassistbot.adapter.DoctorsAdapter
import mk.ukim.finki.docappointassistbot.databinding.ActivityDoctorsBinding
import mk.ukim.finki.docappointassistbot.domain.Doctor

class DoctorsActivity : AppCompatActivity() {

    private var _binding: ActivityDoctorsBinding? = null
    private val binding get() = _binding!!

    private lateinit var doctors: ArrayList<Doctor>
    private lateinit var firebaseRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        _binding = ActivityDoctorsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseRef = FirebaseDatabase
            .getInstance("https://docappointassistbot-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("doctors")
        doctors = arrayListOf()
        fetchData()

        binding.doctors.layoutManager = LinearLayoutManager(this)
    }

    private fun fetchData() {
        firebaseRef.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot){
                doctors.clear()
                if (snapshot.exists()){
                    for (doctorSnap in snapshot.children){
                        val doctor = doctorSnap.getValue(Doctor::class.java)
                        doctors.add(doctor!!)
                        Log.d("DoctorsActivity", "Doctor fetched: $doctor")
                    }
                }
                val adapter = DoctorsAdapter(doctors)
                binding.doctors.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DoctorsActivity, "error: $error", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
