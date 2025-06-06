package mk.ukim.finki.docappointassistbot

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import mk.ukim.finki.docappointassistbot.adapter.TimeSlotAdapter
import mk.ukim.finki.docappointassistbot.databinding.FragmentBookAppointmentBinding
import mk.ukim.finki.docappointassistbot.domain.Appointment
import mk.ukim.finki.docappointassistbot.domain.Doctor
import mk.ukim.finki.docappointassistbot.domain.WorkHours
import mk.ukim.finki.docappointassistbot.domain.repository.AppointmentsRepository
import mk.ukim.finki.docappointassistbot.utils.NotificationScheduler
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BookAppointmentFragment : Fragment() {

    private var _binding: FragmentBookAppointmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var firebaseRef: DatabaseReference
    private lateinit var timeSlotAdapter: TimeSlotAdapter

    private var selectedDate: String = ""
    private var selectedTime: String? = null
    private var doctorId: Int = 0
    private var workHours: List<WorkHours> = listOf()
    private var bookedTimeSlots: List<String> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookAppointmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firebaseRef = FirebaseDatabase
            .getInstance("https://docappointassistbot-default-rtdb.europe-west1.firebasedatabase.app")
            .reference

        doctorId = arguments?.getInt("doctor_id") ?: 0
        val doctorName = arguments?.getString("doctor_name") ?: "Unknown Doctor"
        binding.tvDoctorFullName.text = "Booking: $doctorName"

        timeSlotAdapter = TimeSlotAdapter { time -> selectedTime = time }
        binding.rvTimeSlots.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvTimeSlots.adapter = timeSlotAdapter

        binding.calendarView.setOnDateChangeListener { _, year, month, day ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, day)
            val dayOfWeek = SimpleDateFormat("EEE", Locale.ENGLISH).format(calendar.time)
            selectedDate = "$year-${month + 1}-$day"
            fetchWorkHours(dayOfWeek)
        }

        binding.btnBookAppointment.setOnClickListener {
            if (selectedTime != null) {
                bookAppointment()
            } else {
                Toast.makeText(requireContext(), "Select a time slot", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchWorkHours(dayOfWeek: String) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss a", Locale.ENGLISH)

        firebaseRef.child("doctors").child(doctorId.toString()).get().addOnSuccessListener { snapshot ->
            val doctor = snapshot.getValue(Doctor::class.java)
            val workHourIds = doctor?.workHourIds ?: listOf()

            firebaseRef.child("workHours").get().addOnSuccessListener { workHoursSnapshot ->
                workHours = workHourIds.mapNotNull { id ->
                    val workHourMap = workHoursSnapshot.child(id.toString()).value as? Map<*, *>
                    workHourMap?.let {
                        WorkHours(
                            daysOfWeek = it["daysOfWeek"] as String,
                            startTime = dateFormat.parse(it["startTime"] as String) ?: Date(),
                            endTime = dateFormat.parse(it["endTime"] as String) ?: Date()
                        )
                    }
                }.filter { it.daysOfWeek.contains(dayOfWeek) }

                fetchBookedAppointmentsForDate()
            }
        }
    }

    private fun fetchBookedAppointmentsForDate() {
        val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(
            SimpleDateFormat("yyyy-M-d", Locale.ENGLISH).parse(selectedDate)!!
        )

        firebaseRef.child("appointments").get().addOnSuccessListener { snapshot ->
            bookedTimeSlots = snapshot.children
                .mapNotNull { it.getValue(Appointment::class.java) }
                .filter { it.doctorId == doctorId && it.startTime.startsWith(formattedDate) }
                .filter { it.status != "Canceled" }
                .map { SimpleDateFormat("HH:mm a", Locale.ENGLISH)
                    .format(SimpleDateFormat("yyyy-MM-dd HH:mm a", Locale.ENGLISH).parse(it.startTime)!!) }
            generateTimeSlots()
        }
    }

    private fun generateTimeSlots() {
        val slots = mutableListOf<String>()
        for (workHour in workHours) {
            val startTime = workHour.startTime.time
            val endTime = workHour.endTime.time

            val calendar = Calendar.getInstance()
            calendar.timeInMillis = startTime

            while (calendar.timeInMillis < endTime) {
                val slotTime = SimpleDateFormat("HH:mm a", Locale.ENGLISH).format(calendar.time)
                slots.add(slotTime)
                calendar.add(Calendar.MINUTE, 30)
            }
        }

        timeSlotAdapter.updateSlots(slots, bookedTimeSlots)
    }

    private fun bookAppointment() {
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm a", Locale.ENGLISH)
        val time = timeFormat.parse("$selectedDate $selectedTime") ?: Date()

        val calendar = Calendar.getInstance()
        calendar.time = time
        calendar.add(Calendar.MINUTE, 30)

        val startTime = timeFormat.format(time)
        val endTime = timeFormat.format(calendar.time)

        val userId = FirebaseAuth.getInstance().currentUser?.email.toString()
        Log.d("BookAppointmentFragment", "User email: ${userId}")

        firebaseRef.child("doctors").child(doctorId.toString()).get().addOnSuccessListener { snapshot ->
            val doctor = snapshot.getValue(Doctor::class.java)

            val appointmentRef = firebaseRef.child("appointments").push()
            val generatedKey = appointmentRef.key

            if (doctor != null) {
                val appointment = Appointment(
                    id = generatedKey.hashCode(),
                    doctorId = doctorId,
                    userId = userId,
                    startTime = startTime,
                    endTime = endTime,
                    status = "Upcoming",
                    doctor = doctor
                )

                val now = Date()
                val appointmentTime = timeFormat.parse(startTime)

                if (appointmentTime != null && appointmentTime.before(now)){
                    appointment.status = "Completed"
                }

                firebaseRef.child("appointments").push().setValue(appointment)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Appointment booked!", Toast.LENGTH_SHORT).show()

                        NotificationScheduler.scheduleNotification(requireContext(), appointment)

                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to book!", Toast.LENGTH_SHORT).show()
                    }

                val fragment = AppointmentsFragment()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.frameLayout, fragment)
                    .addToBackStack(null)
                    .commit()

                val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
                bottomNav.selectedItemId = R.id.appointments

            } else {
                Toast.makeText(requireContext(), "Doctor not found!", Toast.LENGTH_SHORT).show()
            }
        }
    }

}