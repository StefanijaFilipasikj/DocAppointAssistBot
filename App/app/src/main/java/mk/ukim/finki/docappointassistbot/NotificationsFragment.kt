package mk.ukim.finki.docappointassistbot

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import mk.ukim.finki.docappointassistbot.adapter.NotificationsAdapter
import mk.ukim.finki.docappointassistbot.databinding.FragmentNotificationsBinding
import mk.ukim.finki.docappointassistbot.ui.viewModels.AppointmentsViewModel
import mk.ukim.finki.docappointassistbot.ui.viewModels.NotificationsViewModel
import mk.ukim.finki.docappointassistbot.ui.viewModels.NotificationsViewModelFactory

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var upcomingAdapter: NotificationsAdapter
    private lateinit var recentAdapter: NotificationsAdapter
    private lateinit var viewModel: NotificationsViewModel
    private lateinit var appointmentsViewModel: AppointmentsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)

        binding.upcomingRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recentRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        upcomingAdapter = NotificationsAdapter(emptyList(), emptyMap()) { id, isChecked ->
            viewModel.updateNotificationState(id, isChecked)
        }
        recentAdapter = NotificationsAdapter(emptyList(), emptyMap()) { id, isChecked ->
            viewModel.updateNotificationState(id, isChecked)
        }

        binding.upcomingRecyclerView.adapter = upcomingAdapter
        binding.recentRecyclerView.adapter = recentAdapter

        val sharedPref = requireContext().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
        val notificationsEnabled = sharedPref.getBoolean("notifications_enabled", false)

        if (!notificationsEnabled) {
            binding.tvNoUpcomingNotifications.text = "Notifications are disabled"
            binding.tvNoRecentNotifications.text = "Notifications are disabled"

            binding.tvNoUpcomingNotifications.visibility = View.VISIBLE
            binding.tvNoRecentNotifications.visibility = View.VISIBLE

            binding.upcomingRecyclerView.visibility = View.GONE
            binding.recentRecyclerView.visibility = View.GONE

            return binding.root
        }

        appointmentsViewModel = ViewModelProvider(requireActivity())[AppointmentsViewModel::class.java]
        viewModel = ViewModelProvider(
            this,
            NotificationsViewModelFactory(requireContext(), appointmentsViewModel)
        )[NotificationsViewModel::class.java]

        val user = FirebaseAuth.getInstance().currentUser

        viewModel.notifications.observe(viewLifecycleOwner) { notifications ->
            val upcomingNotifications = notifications.filter {
                it.appointment.status == "Upcoming"
            }
            val recentNotifications = notifications.filter {
                it.appointment.status == "Completed"
            }

            binding.tvNoUpcomingNotifications.visibility =
                if (upcomingNotifications.isEmpty() || user == null) View.VISIBLE else View.GONE
            binding.tvNoRecentNotifications.visibility =
                if (recentNotifications.isEmpty() || user == null) View.VISIBLE else View.GONE

            binding.upcomingRecyclerView.visibility =
                if (upcomingNotifications.isEmpty()) View.GONE else View.VISIBLE
            binding.recentRecyclerView.visibility =
                if (recentNotifications.isEmpty()) View.GONE else View.VISIBLE

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

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
