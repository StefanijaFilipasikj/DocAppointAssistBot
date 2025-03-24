package mk.ukim.finki.docappointassistbot

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import mk.ukim.finki.docappointassistbot.adapter.AppointmentAdapter
import mk.ukim.finki.docappointassistbot.databinding.FragmentAppointmentsBinding
import mk.ukim.finki.docappointassistbot.domain.Appointment
import mk.ukim.finki.docappointassistbot.domain.Doctor
import mk.ukim.finki.docappointassistbot.domain.Hospital

class AppointmentsFragment : Fragment() {

    private var _binding: FragmentAppointmentsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: AppointmentAdapter
    private val appointments = mutableListOf<Appointment>()
    private var selectedStatus: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppointmentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AppointmentAdapter(appointments)
        binding.appointmentsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.appointmentsRecyclerView.adapter = adapter
        fetchAppointments()

        binding.tvUpcoming.setOnClickListener {
            filterAppointments("Upcoming")
            selectButton(binding.tvUpcoming)
        }
        binding.tvCompleted.setOnClickListener {
            filterAppointments("Completed")
            selectButton(binding.tvCompleted)
        }
        binding.tvCanceled.setOnClickListener {
            filterAppointments("Canceled")
            selectButton(binding.tvCanceled)
        }

    }

    private fun selectButton(status: TextView) {
        selectedStatus?.apply {
            setTextColor(requireContext().getColor(R.color.gray_900))
            setBackgroundResource(R.drawable.bg_white_radius05)
        }

        if(selectedStatus != status){
            status.setTextColor(requireContext().getColor(R.color.white))
            status.setBackgroundResource(R.drawable.bg_blue500_radius05)
            selectedStatus = status
        }else{
            selectedStatus = null
        }
    }

    private fun fetchAppointments() {
        val userId = FirebaseAuth.getInstance().currentUser?.email.toString()
        val database = FirebaseDatabase
            .getInstance("https://docappointassistbot-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("appointments")

        database.orderByChild("userId").equalTo(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                appointments.clear()
                for (data in snapshot.children) {
                    val appointment = data.getValue(Appointment::class.java)
                    if (appointment != null) {
                        fetchDoctorDetails(appointment)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", error.message)
            }
        })
    }

    private fun fetchDoctorDetails(appointment: Appointment) {
        val doctorRef = FirebaseDatabase
            .getInstance("https://docappointassistbot-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("doctors/${appointment.doctorId}")

        doctorRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val doctor = snapshot.getValue(Doctor::class.java)
                if(doctor != null) {
                    appointment.doctor = doctor
                    fetchHospitalDetails(appointment, doctor.hospitalIds!!)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", error.message)
            }
        })
    }

    private fun fetchHospitalDetails(appointment: Appointment, hospitalIds: List<Int>){
        val hospitalRef = FirebaseDatabase
            .getInstance("https://docappointassistbot-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("hospitals")

        hospitalRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val hospitals = snapshot.children
                    .mapNotNull { it.getValue(Hospital::class.java) }
                    .filter { hospitalIds.contains(it.id) }

                appointment.doctor?.hospitals = hospitals
                appointments.add(appointment)
                adapter.updateAppointments(appointments)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", error.message)
            }
        })

    }

    private fun filterAppointments(status: String) {
        if(status == selectedStatus?.text){
            adapter.updateAppointments(appointments)
        }else{
            val filtered = appointments.filter { it.status.equals(status, ignoreCase = true) }
            adapter.updateAppointments(filtered)
        }
    }
}
