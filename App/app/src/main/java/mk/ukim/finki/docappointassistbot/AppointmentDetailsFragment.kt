package mk.ukim.finki.docappointassistbot

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import mk.ukim.finki.docappointassistbot.databinding.FragmentAppointmentDetailsBinding
import mk.ukim.finki.docappointassistbot.domain.Appointment
import mk.ukim.finki.docappointassistbot.domain.PatientReport
import mk.ukim.finki.docappointassistbot.retrofit.ChatbotRetrofitClient
import mk.ukim.finki.docappointassistbot.ui.viewModels.AppointmentsViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AppointmentDetailsFragment : Fragment() {

    private var _binding: FragmentAppointmentDetailsBinding? = null
    private val binding get() = _binding!!

    private lateinit var appointment: Appointment
    private val viewModel: AppointmentsViewModel by activityViewModels()

    private lateinit var database: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppointmentDetailsBinding.inflate(inflater, container, false)
        database = FirebaseDatabase.getInstance().getReference("users")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val appointmentId = requireArguments().getInt("appointmentId")

        viewModel.appointments.observe(viewLifecycleOwner) { list ->
            val appointmentFound = list.firstOrNull { it.id == appointmentId }
            if (appointmentFound == null) {
                return@observe
            }

            appointment = appointmentFound
            if (appointment.status.trim() != "Upcoming")
                binding.editButton.visibility = View.GONE

            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                fetchUserRole(currentUser.uid) { role ->
                    if (role == "doctor") {
                        getPatientName(appointment.userId) { patientName ->
                            binding.tvDoctorFullName.text = "${getString(R.string.patient)}: $patientName"
                        }
                        showTextMode()
                    } else {
                        binding.tvDoctorFullName.text = "${getString(R.string.doctor)}: ${appointment.doctor?.fullname}"
                        hideDetailsSection()
                    }
                }
            }

            binding.tvSpecialty.text = "${getString(R.string.specialty)}: ${appointment.doctor?.specialty}"
            binding.tvHospital.text = "${getString(R.string.hospital)}: ${appointment.doctor?.hospital?.name}"
            binding.tvAppointmentDateTime.text = "${getString(R.string.date)}: ${appointment.startTime}"

            binding.editButton.setOnClickListener {
                showEditMode()
            }

            binding.saveButton.setOnClickListener {
                val newDetails = binding.detailsInput.text.toString().trim()
                if (newDetails.isNotBlank()) {
                    viewModel.updateAppointmentDetails(appointment.id, newDetails)
                    showTextMode()
                }

                val patientReport = PatientReport(
                    text = binding.detailsInput.text.toString(),
                    patient_id = appointment.userId,
                    date = appointment.startTime
                )

                ChatbotRetrofitClient.api.embedReport(patientReport).enqueue(object : Callback<Boolean> {
                    override fun onResponse(call: Call<Boolean>, response: Response<Boolean>) {
                        if (response.isSuccessful && response.body() == true) {
                            Log.d("RETROFIT", "Document embedded!")
                        } else {
                            Log.e("RETROFIT", "Error: ${response.code()}")
                        }
                    }

                    override fun onFailure(call: Call<Boolean>, t: Throwable) {
                        Log.e("RETROFIT", "Failure: ${t.message}")
                    }
                })
            }
        }
    }

    private fun showTextMode() {
        val details = appointment.details?.trim()
        if (details.isNullOrEmpty()) {
            binding.detailsText.text = getString(R.string.enter_appointment_details)
            binding.editButton.visibility = View.VISIBLE
        } else {
            binding.detailsText.text = details
            binding.editButton.visibility = View.GONE
        }
        binding.detailsText.visibility = View.VISIBLE
        binding.detailsInput.visibility = View.GONE
        binding.saveButton.visibility = View.GONE
    }

    private fun showEditMode() {
        binding.detailsInput.setText(appointment.details)
        binding.detailsText.visibility = View.GONE
        binding.editButton.visibility = View.GONE
        binding.detailsInput.visibility = View.VISIBLE
        binding.saveButton.visibility = View.VISIBLE
    }

    private fun hideDetailsSection() {
        binding.tvDetails.visibility = View.GONE
        binding.cvDetails.visibility = View.GONE
        binding.detailsInput.visibility = View.GONE
        binding.editButton.visibility = View.GONE
        binding.saveButton.visibility = View.GONE
    }

    private fun fetchUserRole(userId: String, callback: (String) -> Unit) {
        database.child(userId).child("role").addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val role = snapshot.getValue(String::class.java)
                callback(role ?: "unknown")
            }

            override fun onCancelled(error: DatabaseError) {
                callback("unknown")
            }
        })
    }

    private fun getPatientName(userId: String, callback: (String) -> Unit) {
        val database = FirebaseDatabase.getInstance().getReference("users")
        database.orderByChild("email").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val child = snapshot.children.firstOrNull()
                    val patientFullName = child?.child("fullName")?.getValue(String::class.java) ?: "Unknown Patient"
                    callback(patientFullName)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback("Unknown Patient")
                }
            })
    }

}
