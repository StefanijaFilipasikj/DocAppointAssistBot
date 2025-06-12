package mk.ukim.finki.docappointassistbot

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.bumptech.glide.Glide
import mk.ukim.finki.docappointassistbot.databinding.FragmentDoctorDetailsBinding
import mk.ukim.finki.docappointassistbot.domain.Doctor
import mk.ukim.finki.docappointassistbot.domain.Hospital
import mk.ukim.finki.docappointassistbot.domain.WorkHours
import com.google.firebase.database.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class DoctorDetailsFragment : Fragment() {

    private var _binding: FragmentDoctorDetailsBinding? = null
    private val binding get() = _binding!!

    private var doctorId: Int? = null
    private lateinit var firebaseRef: DatabaseReference

    companion object {
        private const val ARG_DOCTOR_ID = "id"

        fun newInstance(doctorId: Int): DoctorDetailsFragment {
            val fragment = DoctorDetailsFragment()
            val args = Bundle()
            args.putInt(ARG_DOCTOR_ID, doctorId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        doctorId = arguments?.getInt(ARG_DOCTOR_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoctorDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firebaseRef = FirebaseDatabase
            .getInstance("https://docappointassistbot-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("doctors")

        doctorId?.let { fetchDoctor(it) }
    }

    private fun fetchDoctor(doctorId: Int) {
        firebaseRef.child(doctorId.toString()).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val doctor = snapshot.getValue(Doctor::class.java)

                doctor?.let {
                    if (it.hospitalId != null) {
                        fetchHospital(it.hospitalId) { hospital ->
                            it.hospital = hospital

                            fetchWorkHours(it.workHourIds ?: emptyList()) { workHours ->
                                it.workHours = workHours
                                updateUI(it)
                            }
                        }
                    }

                }
            } else {
                Toast.makeText(requireContext(), "Doctor not found", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchHospital(hospitalId: Int, callback: (Hospital?) -> Unit) {
        val hospitalRef = FirebaseDatabase
            .getInstance("https://docappointassistbot-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("hospitals")
            .child(hospitalId.toString())

        hospitalRef.get().addOnSuccessListener { snapshot ->
            val hospital = snapshot.getValue(Hospital::class.java)
            callback(hospital)
        }.addOnFailureListener {
            callback(null)
        }
    }


    private fun fetchWorkHours(workHourIds: List<Int>, callback: (List<WorkHours>) -> Unit) {
        val workHours = mutableListOf<WorkHours>()
        val workHoursRef = FirebaseDatabase
            .getInstance("https://docappointassistbot-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("workHours")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        workHourIds.forEach { workHourId ->
            workHoursRef.child(workHourId.toString()).get().addOnSuccessListener { snapshot ->
                val daysOfWeek = snapshot.child("daysOfWeek").getValue(String::class.java)
                val startTime = snapshot.child("startTime").getValue(String::class.java)
                val endTime = snapshot.child("endTime").getValue(String::class.java)

                if (daysOfWeek != null && startTime != null && endTime != null) {
                    try {
                        val startDate = dateFormat.parse(startTime) ?: Date()
                        val endDate = dateFormat.parse(endTime) ?: Date()

                        workHours.add(WorkHours(daysOfWeek, startDate, endDate))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (workHours.size == workHourIds.size) {
                    callback(workHours)
                }
            }
        }
    }

    private fun updateUI(doctor: Doctor) {
        Glide.with(requireContext())
            .load(doctor.image)
            .centerCrop()
            .placeholder(R.drawable.ic_launcher_background)
            .into(binding.doctorImage)

        binding.tvFullName.text = doctor.fullname
        binding.tvSpecialty.text = doctor.specialty

        binding.tvNumPatients.text = "Patients\n ${formatNumberToK(doctor.patients)}"
        binding.tvYearsExperience.text = "Experience\n ${doctor.experience} years"
        binding.tvNumReviews.text = "Reviews\n ${formatNumberToK(doctor.reviews)}"

        binding.tvBiography.text = doctor.bio

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val workHoursText = doctor.workHours?.joinToString("\n") {
            "${it.daysOfWeek}:  ${timeFormat.format(it.startTime)} - ${timeFormat.format(it.endTime)}"
        }
        binding.tvSchedule.text = workHoursText

        binding.tvCityAndCountry.text = "${doctor.city}, ${doctor.country}"
        binding.tvHospitals.text = doctor.hospital?.name ?: "Unknown hospital"


        binding.btnBookAppointment.setOnClickListener {
            val bookAppointmentFragment = BookAppointmentFragment()
            val bundle = Bundle()
            bundle.putString("doctor_name", doctor.fullname)
            bundle.putInt("doctor_id", doctor.id)
            bookAppointmentFragment.arguments = bundle

            val fragmentTransaction = parentFragmentManager.beginTransaction()
            fragmentTransaction.replace(R.id.frameLayout, bookAppointmentFragment)
            fragmentTransaction.addToBackStack(null)
            fragmentTransaction.commit()
        }
    }

    private fun formatNumberToK(number: Int): String {
        return if (number >= 1000) {
            val formattedNumber = number / 1000.0
            val decimalFormat = DecimalFormat("#,##0.#")
            "${decimalFormat.format(formattedNumber)}k"
        } else {
            number.toString()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
