package mk.ukim.finki.docappointassistbot.ui.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import mk.ukim.finki.docappointassistbot.domain.Appointment
import mk.ukim.finki.docappointassistbot.domain.repository.AppointmentsRepository

class AppointmentsViewModel : ViewModel() {

    private val repository = AppointmentsRepository()
    private val _appointments = MutableLiveData<List<Appointment>>()
    val appointments: LiveData<List<Appointment>> get() = _appointments

    init {
        fetchAppointments()
    }

    fun fetchAppointments() {
        repository.getAppointments().observeForever {
            _appointments.value = it
        }
    }

    fun filterAppointments(status: String): List<Appointment> {
        return repository.filterAppointments(status)
    }
}
