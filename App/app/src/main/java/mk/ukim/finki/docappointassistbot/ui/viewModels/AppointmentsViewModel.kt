package mk.ukim.finki.docappointassistbot.ui.viewModels

import android.content.Context
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

    fun cancelAppointment(context: Context, appointment: Appointment) {
        repository.cancelAppointment(context, appointment).observeForever { updatedAppointments ->
            _appointments.value = updatedAppointments
        }
    }

    fun updateAppointmentDetails(id: Int, newDetails: String) {
        repository.updateDetails(id, newDetails).observeForever { updatedList ->
            _appointments.value = updatedList
        }
    }
}
