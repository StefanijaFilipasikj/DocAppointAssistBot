package mk.ukim.finki.docappointassistbot.ui.viewModels

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import mk.ukim.finki.docappointassistbot.domain.Appointment
import mk.ukim.finki.docappointassistbot.domain.Doctor
import mk.ukim.finki.docappointassistbot.domain.Hospital
import mk.ukim.finki.docappointassistbot.domain.Notification
import androidx.core.content.edit
import mk.ukim.finki.docappointassistbot.utils.NotificationScheduler

class NotificationsViewModel(
    private val context: Context,
    private val appointmentsViewModel: AppointmentsViewModel
) : ViewModel() {

    private val _notifications = MutableLiveData<List<Notification>>()
    val notifications: LiveData<List<Notification>> get() = _notifications
    private val _notificationStates = MutableLiveData<Map<Int, Boolean>>()
    val notificationStates: LiveData<Map<Int, Boolean>> get() = _notificationStates

    private val dbRef = FirebaseDatabase.getInstance("https://docappointassistbot-default-rtdb.europe-west1.firebasedatabase.app").reference
    private val sharedPrefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)

    init {
        _notificationStates.value = loadNotificationStates()

        appointmentsViewModel.appointments.observeForever { appointments ->
            if (appointments.isNotEmpty()) {
                viewModelScope.launch {
                    fetchDoctorsForAppointments(appointments)
                }
            } else {
                _notifications.value = emptyList()
            }
        }
    }

    private suspend fun fetchDoctorsForAppointments(appointments: List<Appointment>) {
        if (appointments.isEmpty()) {
            _notifications.value = emptyList()
            return
        }

        val notifications = mutableListOf<Notification>()
        val notificationStates = mutableMapOf<Int, Boolean>()

        for (appointment in appointments) {
            try {
                val snapshot = dbRef.child("doctors")
                    .child(appointment.doctorId.toString())
                    .get()
                    .await()

                val doctor = snapshot.getValue(Doctor::class.java)
                if (doctor != null) {
                    fetchHospitalDetailsForDoctor(doctor, appointment, notifications, notificationStates)
                }
            } catch (e: Exception) {
                Log.e("NotificationsVM", "Failed to load doctor: ${e.message}")
            }
        }

        _notifications.value = notifications
        _notificationStates.value = notificationStates
    }

    private suspend fun fetchHospitalDetailsForDoctor(
        doctor: Doctor,
        appointment: Appointment,
        notifications: MutableList<Notification>,
        notificationStates: MutableMap<Int, Boolean>
    ) {
        val hospitalId = doctor.hospitalId ?: return

        try {
            val snapshot = dbRef.child("hospitals").child(hospitalId.toString()).get().await()
            val hospital = snapshot.getValue(Hospital::class.java)
            doctor.hospital = hospital

            val updatedAppointment = appointment.copy(doctor = doctor)

            val notificationId = updatedAppointment.id

            val appointmentStatus = updatedAppointment.status
            val savedState = sharedPrefs.getBoolean(notificationId.toString(), true)

            if (appointmentStatus == "Completed" || appointmentStatus == "Canceled") {
                sharedPrefs.edit().remove(notificationId.toString()).apply()
            }

            if (appointmentStatus == "Canceled") {
                NotificationScheduler.cancelNotification(context, notificationId)
                return
            }

            val notification = Notification(
                id = notificationId,
                appointment = updatedAppointment,
                isEnabled = savedState
            )
            notifications.add(notification)
            notificationStates[notificationId] = savedState

        } catch (e: Exception) {
            Log.e("NotificationsVM", "Failed to load hospital details: ${e.message}")
        }
    }

    fun updateNotificationState(appointmentId: Int, isEnabled: Boolean) {
        val currentStates = _notificationStates.value?.toMutableMap() ?: mutableMapOf()
        currentStates[appointmentId] = isEnabled
        _notificationStates.value = currentStates
        saveNotificationState(appointmentId, isEnabled)

        val notification = _notifications.value?.find { it.id == appointmentId }
        notification?.let {
            if (isEnabled) {
                NotificationScheduler.scheduleNotification(context, it.appointment)
            } else {
                NotificationScheduler.cancelNotification(context, appointmentId)
            }
        }
    }

    private fun saveNotificationState(id: Int, isEnabled: Boolean) {
        sharedPrefs.edit() { putBoolean(id.toString(), isEnabled) }
    }

    private fun loadNotificationStates(): Map<Int, Boolean> {
        val allPrefs = sharedPrefs.all
        return allPrefs.mapNotNull {
            val key = it.key.toIntOrNull()
            val value = it.value as? Boolean
            if (key != null && value != null) key to value else null
        }.toMap()
    }
}
