package mk.ukim.finki.docappointassistbot.domain.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import mk.ukim.finki.docappointassistbot.domain.Appointment
import mk.ukim.finki.docappointassistbot.domain.Doctor
import mk.ukim.finki.docappointassistbot.domain.Hospital
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppointmentsRepository {

    private val appointmentsList = mutableListOf<Appointment>()
    private val appointmentsLiveData = MutableLiveData<List<Appointment>>()

    fun getAppointments(): LiveData<List<Appointment>> {
        val userId = FirebaseAuth.getInstance().currentUser?.email.toString()
        val database = FirebaseDatabase.getInstance("https://docappointassistbot-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("appointments")

        database.orderByChild("userId").equalTo(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                appointmentsList.clear()
                for (data in snapshot.children) {
                    val appointment = data.getValue(Appointment::class.java)
                    if (appointment != null) {
                        val firebaseKey = data.key
                        if (firebaseKey != null) {
                            fetchDoctorDetails(appointment, firebaseKey)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", error.message)
            }
        })

        return appointmentsLiveData
    }

    private fun fetchDoctorDetails(appointment: Appointment, firebaseKey: String) {
        val doctorRef = FirebaseDatabase.getInstance("https://docappointassistbot-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("doctors/${appointment.doctorId}")

        doctorRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val doctor = snapshot.getValue(Doctor::class.java)
                if (doctor != null) {
                    appointment.doctor = doctor

                    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm a", Locale.getDefault())
                    try{
                        val appointmentDate = formatter.parse(appointment.startTime)
                        val now = Date()

                        if (appointmentDate != null && appointmentDate.before(now) && appointment.status.equals("Upcoming", ignoreCase = true)) {
                            appointment.status = "Completed"

                            val appointmentRef = FirebaseDatabase.getInstance("https://docappointassistbot-default-rtdb.europe-west1.firebasedatabase.app")
                                .getReference("appointments")
                                .child(firebaseKey)

                            val updates = mapOf<String, Any>(
                                "status" to "Completed"
                            )
                            appointmentRef.updateChildren(updates)
                        }
                    } catch (e: Exception) {
                        Log.e("DateParseError", "Failed to parse appointment date: ${e.message}")
                    }

                    fetchHospitalDetails(appointment, doctor.hospitalIds ?: emptyList())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", error.message)
            }
        })
    }

    private fun fetchHospitalDetails(appointment: Appointment, hospitalIds: List<Int>) {
        val hospitalRef = FirebaseDatabase.getInstance("https://docappointassistbot-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("hospitals")

        hospitalRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val hospitals = snapshot.children
                    .mapNotNull { it.getValue(Hospital::class.java) }
                    .filter { hospitalIds.contains(it.id) }

                appointment.doctor?.hospitals = hospitals

                if (!appointmentsList.contains(appointment)) {
                    appointmentsList.add(appointment)
                }

                val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm a", Locale.getDefault())
                appointmentsList.sortBy {
                    try {
                        formatter.parse(it.startTime)
                    } catch (e: Exception) {
                        null
                    }
                }

                appointmentsLiveData.postValue(appointmentsList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", error.message)
            }
        })
    }


    fun filterAppointments(status: String): List<Appointment> {
        return appointmentsList.filter { it.status.equals(status, ignoreCase = true) }
    }
}
