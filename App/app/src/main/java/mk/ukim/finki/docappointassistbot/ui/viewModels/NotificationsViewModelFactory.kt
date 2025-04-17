package mk.ukim.finki.docappointassistbot.ui.viewModels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class NotificationsViewModelFactory(private val context: Context, private val appointmentsViewModel: AppointmentsViewModel) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationsViewModel::class.java)) {
            return NotificationsViewModel(context, appointmentsViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
