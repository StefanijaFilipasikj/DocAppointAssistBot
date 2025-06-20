package mk.ukim.finki.docappointassistbot

import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
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
import mk.ukim.finki.docappointassistbot.domain.Specialty
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
            replaceFragment(ChatbotFragment.newInstance(user?.email, "patient"))
        }

        // Upcoming Appointments
        if (user != null) {
            fetchUpcomingAppointments(user.email)
        }
        viewModel = activityViewModels<AppointmentsViewModel>().value
        appointmentsAdapter = AppointmentAdapter(
            emptyList(),
            onClick = { appointment -> onClickAppointment(appointment) },
            onCancel = { },
            enableCancel = false
        )
        binding.recyclerAppointments.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerAppointments.adapter = appointmentsAdapter

        // Browse by specialty
        fetchAndDisplaySpecialties()
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

    private fun fetchUpcomingAppointments(userEmail: String?) {
        val appointmentsRef = FirebaseDatabase
            .getInstance("https://docappointassistbot-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("appointments")

        appointmentsRef.orderByChild("userId")
            .equalTo(userEmail)
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

    private fun fetchAndDisplaySpecialties(){
        val dbRef = FirebaseDatabase
            .getInstance("https://docappointassistbot-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("doctors")

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val specialtyCountMap = mutableMapOf<String, Int>()

                for (doctorSnap in snapshot.children) {
                    val specialty = doctorSnap.child("specialty").getValue(String::class.java)?.trim()
                    if (!specialty.isNullOrEmpty()) {
                        specialtyCountMap[specialty] = specialtyCountMap.getOrDefault(specialty, 0) + 1
                    }
                }

                val specialties = specialtyCountMap.map { (specialty, _) ->
                    val key = specialty.lowercase().replace(" ", "_")
                    val stringResId = resources.getIdentifier(key, "string", requireContext().packageName)

                    val drawableResId = resources.getIdentifier(
                        "ic_illustration_${key}_40",
                        "drawable",
                        requireContext().packageName
                    ).takeIf { it != 0 } ?: R.drawable.ic_illustration_sickness_40

                    Specialty(specialty, stringResId, drawableResId)
                }.sortedByDescending { specialtyCountMap[it.key] }

                val recyclerSpecialists = view?.findViewById<RecyclerView>(R.id.recycler_specialists)
                recyclerSpecialists?.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                recyclerSpecialists?.adapter = SpecialtyAdapter(specialties) { selectedSpecialist ->
                    val fragment = DoctorsFragment().apply {
                        arguments = Bundle().apply {
                            putString("specialty", selectedSpecialist.trim())
                        }
                    }
                    replaceFragment(fragment)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "error: $error", Toast.LENGTH_SHORT).show()
            }
        })
    }

}
