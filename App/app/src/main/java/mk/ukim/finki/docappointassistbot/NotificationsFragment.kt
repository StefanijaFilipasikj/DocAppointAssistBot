package mk.ukim.finki.docappointassistbot

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mk.ukim.finki.docappointassistbot.adapter.NotificationsAdapter
import mk.ukim.finki.docappointassistbot.ui.viewModels.AppointmentsViewModel
import mk.ukim.finki.docappointassistbot.ui.viewModels.NotificationsViewModel
import mk.ukim.finki.docappointassistbot.ui.viewModels.NotificationsViewModelFactory
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationsFragment : Fragment() {

    private lateinit var upcomingRecyclerView: RecyclerView
    private lateinit var recentRecyclerView: RecyclerView
    private lateinit var upcomingAdapter: NotificationsAdapter
    private lateinit var recentAdapter: NotificationsAdapter
    private lateinit var viewModel: NotificationsViewModel
    private lateinit var appointmentsViewModel: AppointmentsViewModel

    private lateinit var noUpcomingNotificationsText: TextView
    private lateinit var noRecentNotificationsText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notifications, container, false)

        upcomingRecyclerView = view.findViewById(R.id.upcomingRecyclerView)
        upcomingRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        recentRecyclerView = view.findViewById(R.id.recentRecyclerView)
        recentRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        noUpcomingNotificationsText = view.findViewById(R.id.tvNoUpcomingNotifications)
        noRecentNotificationsText = view.findViewById(R.id.tvNoRecentNotifications)

        appointmentsViewModel = ViewModelProvider(requireActivity()).get(AppointmentsViewModel::class.java)
        viewModel = ViewModelProvider(this, NotificationsViewModelFactory(requireContext(), appointmentsViewModel))
            .get(NotificationsViewModel::class.java)

        upcomingAdapter = NotificationsAdapter(emptyList(), emptyMap()) { id, isChecked ->
            viewModel.updateNotificationState(id, isChecked)
        }
        recentAdapter = NotificationsAdapter(emptyList(), emptyMap()) { id, isChecked ->
            viewModel.updateNotificationState(id, isChecked)
        }

        upcomingRecyclerView.adapter = upcomingAdapter
        recentRecyclerView.adapter = recentAdapter

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm a", Locale.getDefault())

        val upcoming = viewModel.notifications.value?.filter {
            val startTime = dateFormat.parse(it.appointment.startTime)?.time ?: 0L
            startTime > System.currentTimeMillis()
        }

        val recent = viewModel.notifications.value?.filter {
            val startTime = dateFormat.parse(it.appointment.startTime)?.time ?: 0L
            startTime <= System.currentTimeMillis()
        }

        noUpcomingNotificationsText.visibility = if (upcoming.isNullOrEmpty()) View.VISIBLE else View.GONE
        noRecentNotificationsText.visibility = if (recent.isNullOrEmpty()) View.VISIBLE else View.GONE

        viewModel.notifications.observe(viewLifecycleOwner) { notifications ->
            val upcomingNotifications = notifications.filter {
                val startTime = dateFormat.parse(it.appointment.startTime)?.time ?: 0L
                startTime > System.currentTimeMillis()
            }

            val recentNotifications = notifications.filter {
                val startTime = dateFormat.parse(it.appointment.startTime)?.time ?: 0L
                startTime <= System.currentTimeMillis()
            }

            noUpcomingNotificationsText.visibility = if (upcomingNotifications.isEmpty()) View.VISIBLE else View.GONE
            noRecentNotificationsText.visibility = if (recentNotifications.isEmpty()) View.VISIBLE else View.GONE

            viewModel.notificationStates.observe(viewLifecycleOwner) { notificationStates ->
                upcomingAdapter.apply {
                    this.notifications = upcomingNotifications
                    this.notificationStates = notificationStates
                    notifyDataSetChanged()
                }

                recentAdapter.apply {
                    this.notifications = recentNotifications
                    this.notificationStates = notificationStates
                    notifyDataSetChanged()
                }
            }
        }

        return view
    }
}
