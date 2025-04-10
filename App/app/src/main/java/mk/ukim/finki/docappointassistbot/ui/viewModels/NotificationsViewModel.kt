package mk.ukim.finki.docappointassistbot.ui.viewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import mk.ukim.finki.docappointassistbot.domain.Appointment
import mk.ukim.finki.docappointassistbot.domain.Doctor
import mk.ukim.finki.docappointassistbot.domain.Hospital
import mk.ukim.finki.docappointassistbot.domain.Notification

class NotificationsViewModel : ViewModel() {
    private val _notifications = MutableLiveData<List<Notification>>()
    val notifications: LiveData<List<Notification>> get() = _notifications

    private val dbRef = FirebaseDatabase.getInstance("https://docappointassistbot-default-rtdb.europe-west1.firebasedatabase.app").reference

    init {
        loadUserAppointments()
    }

    private fun loadUserAppointments() {
        val userId = FirebaseAuth.getInstance().currentUser?.email ?: return

        viewModelScope.launch {
            try {
                val snapshot = dbRef.child("appointments")
                    .orderByChild("userId")
                    .equalTo(userId)
                    .get()
                    .await()

                val appointments = mutableListOf<Appointment>()
                for (data in snapshot.children) {
                    val appointment = data.getValue(Appointment::class.java)
                    if (appointment != null) {
                        appointments.add(appointment)
                    }
                }
                fetchDoctorsForAppointments(appointments)
            } catch (e: Exception) {
                Log.e("NotificationsVM", "Failed to load appointments: ${e.message}")
            }
        }
    }

    private suspend fun fetchDoctorsForAppointments(appointments: List<Appointment>) {
        if (appointments.isEmpty()) {
            _notifications.value = emptyList()
            return
        }

        val notifications = mutableListOf<Notification>()

        for (appointment in appointments) {
            try {
                val snapshot = dbRef.child("doctors")
                    .child(appointment.doctorId.toString())
                    .get()
                    .await()

                val doctor = snapshot.getValue(Doctor::class.java)
                if (doctor != null) {
                    fetchHospitalDetailsForDoctor(doctor, appointment, notifications)
                }
            } catch (e: Exception) {
                Log.e("NotificationsVM", "Failed to load doctor: ${e.message}")
            }
        }

        _notifications.value = notifications.sortedByDescending { it.timestamp }
    }

    private suspend fun fetchHospitalDetailsForDoctor(doctor: Doctor, appointment: Appointment, notifications: MutableList<Notification>) {
        val hospitalIds = doctor.hospitalIds ?: emptyList()

        try {
            val snapshot = dbRef.child("hospitals").get().await()
            val hospitals = snapshot.children
                .mapNotNull { it.getValue(Hospital::class.java) }
                .filter { hospitalIds.contains(it.id) }

            doctor.hospitals = hospitals
            val updatedAppointment = appointment.copy(doctor = doctor)

            val notification = Notification(
                id = updatedAppointment.hashCode(),
                appointment = updatedAppointment,
                timestamp = System.currentTimeMillis()
            )
            notifications.add(notification)

        } catch (e: Exception) {
            Log.e("NotificationsVM", "Failed to load hospital details: ${e.message}")
        }
    }
}
