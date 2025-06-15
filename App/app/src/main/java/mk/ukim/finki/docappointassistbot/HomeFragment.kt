package mk.ukim.finki.docappointassistbot

import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import mk.ukim.finki.docappointassistbot.adapter.AppointmentAdapter
import mk.ukim.finki.docappointassistbot.adapter.DoctorAdapter
import mk.ukim.finki.docappointassistbot.adapter.SpecialtyAdapter
import mk.ukim.finki.docappointassistbot.databinding.FragmentHomeBinding
import mk.ukim.finki.docappointassistbot.domain.Appointment
import mk.ukim.finki.docappointassistbot.ui.viewModels.AppointmentsViewModel
import mk.ukim.finki.docappointassistbot.utils.DoctorLocationUtil

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var appointmentsAdapter: AppointmentAdapter
    private lateinit var doctorsNearbyAdapter: DoctorAdapter
    private lateinit var viewModel: AppointmentsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val user = FirebaseAuth.getInstance().currentUser

        // Chatbot
        binding.textHello.text = "${getString(R.string.hello)} ${user?.displayName ?: ""}"
        binding.cardChatWithChatbot.setOnClickListener {
            replaceFragment(ChatbotFragment())
        }

        // Upcoming Appointments
        if (user != null) {
            fetchUpcomingAppointments(user.uid)
        }
        viewModel = ViewModelProvider(this).get(AppointmentsViewModel::class.java)
        appointmentsAdapter = AppointmentAdapter(
            emptyList(),
            onCancel = { appointment -> onCancelAppointment(appointment) },
            onClick = { appointment -> onClickAppointment(appointment) }
        )

        // Browse by specialty
        //TODO: make this dynamic - get distinct specialties of all doctors
        //TODO: map the photos ic_illustration_{specialty}_40 (make sure vector exists)
        //TODO: sort in alphabetical order or by number of doctors in specialty
        val specialties = listOf(
            "Cardiologist" to R.drawable.ic_illustration_cardiologist_40,
            "Neurologist" to R.drawable.ic_illustration_neurologist_40,
            "Gastroenterologist" to R.drawable.ic_illustration_gastroenterologist_40,
            "Dentist" to R.drawable.ic_illustration_dentist_40,
            "Dermatologist" to R.drawable.ic_illustration_dermatologist_40,
            "Psychiatrist" to R.drawable.ic_illustration_psychiatrist_40,
            "Allergist" to R.drawable.ic_illustration_allergist_40,
            "Nephrologist" to R.drawable.ic_illustration_nephrologist_40,
            "Pediatrician" to R.drawable.ic_illustration_pediatrician_40,
        )

        val recyclerSpecialists = view.findViewById<RecyclerView>(R.id.recycler_specialists)
        recyclerSpecialists.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerSpecialists.adapter = SpecialtyAdapter(specialties) { selectedSpecialist ->
            val fragment = DoctorsFragment().apply {
                arguments = Bundle().apply {
                    putString("specialty", selectedSpecialist)
                }
            }
            replaceFragment(fragment);
        }

        binding.tvSeeAllSpecialties.setOnClickListener{
            replaceFragment(DoctorsFragment());
        }

        // Browse nearby doctors
        binding.tvSeeAllNearby.setOnClickListener {
             replaceFragment(DoctorsNearYouFragment())
        }

        doctorsNearbyAdapter = DoctorAdapter(emptyList()) { selectedDoctor ->
            val fragment = DoctorDetailsFragment.newInstance(selectedDoctor.id)
            parentFragmentManager.beginTransaction()
                .replace(R.id.frameLayout, fragment)
                .addToBackStack(null)
                .commit()
        }
        DoctorLocationUtil.getClosestDoctors(this, maxCount = 3) { nearbyDoctors ->
            if (nearbyDoctors.isNotEmpty()) {
                doctorsNearbyAdapter.updateDoctors(nearbyDoctors)
            } else {
                Toast.makeText(requireContext(), "No nearby doctors found", Toast.LENGTH_SHORT).show()
            }
        }
        binding.recyclerDoctorsNearby.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerDoctorsNearby.adapter = doctorsNearbyAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun fetchUpcomingAppointments(uid: String?) {
        val appointmentsRef = FirebaseDatabase
            .getInstance("https://docappointassistbot-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("appointments")

        appointmentsRef.orderByChild("userId")
            .equalTo(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val upcomingAppointments = mutableListOf<Appointment>()
                    for (appointmentSnap in snapshot.children) {
                        val appointment = appointmentSnap.getValue(Appointment::class.java)
                        if (appointment != null && appointment.status.equals("Upcoming", ignoreCase = true)) {
                            upcomingAppointments.add(appointment)
                        }
                    }

                    if (upcomingAppointments.isNotEmpty()) {
                        appointmentsAdapter.updateAppointments(upcomingAppointments)
                        binding.recyclerAppointments.visibility = View.VISIBLE
                    } else {
                        binding.recyclerAppointments.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "error: $error", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun onCancelAppointment(appointment: Appointment) {
        viewModel.cancelAppointment(requireContext(), appointment)
    }

    private fun onClickAppointment(appointment: Appointment) {
        val bundle = bundleOf("appointmentId" to appointment.id)
        val fragment = AppointmentDetailsFragment().apply {
            arguments = bundle
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, fragment)
            .addToBackStack(null)
            .commit()
    }

}
