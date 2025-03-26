package mk.ukim.finki.docappointassistbot.ui.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import mk.ukim.finki.docappointassistbot.domain.Appointment
import mk.ukim.finki.docappointassistbot.domain.Doctor
import mk.ukim.finki.docappointassistbot.domain.Hospital
import mk.ukim.finki.docappointassistbot.domain.Notification

class NotificationsViewModel : ViewModel(){
    private val _notifications = MutableLiveData<List<Notification>>()
    val notifications: LiveData<List<Notification>> get() = _notifications

    init {
        loadNotifications()
    }

    private fun loadNotifications() {
        // Sample doctor and appointment data
        val doctor = Doctor(
            id = 1,
            fullname = "Dr. John Doe",
            specialty = "Cardiology",
            hospitals = listOf(Hospital(1, "City General Hospital"))
        )

        val appointment = Appointment(
            doctorId = 1,
            userId = "123",
            startTime = "2025-04-10 14:00",
            doctor = doctor
        )

        _notifications.value = listOf(
            Notification(1, appointment),
            Notification(2, appointment.copy(startTime = "2025-04-11 09:30")),
            Notification(3, appointment.copy(startTime = "2025-04-12 16:00"))
        )
    }
}