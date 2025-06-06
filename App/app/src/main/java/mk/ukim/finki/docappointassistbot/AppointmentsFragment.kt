package mk.ukim.finki.docappointassistbot

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import mk.ukim.finki.docappointassistbot.adapter.AppointmentAdapter
import mk.ukim.finki.docappointassistbot.databinding.FragmentAppointmentsBinding
import mk.ukim.finki.docappointassistbot.domain.Appointment
import mk.ukim.finki.docappointassistbot.ui.viewModels.AppointmentsViewModel

class AppointmentsFragment : Fragment() {

    private var _binding: FragmentAppointmentsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: AppointmentAdapter
    private lateinit var viewModel: AppointmentsViewModel
    private var selectedStatus: TextView? = null
    private lateinit var noAppointmentsTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppointmentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        noAppointmentsTextView = binding.noAppointmentsTextView

        viewModel = ViewModelProvider(this).get(AppointmentsViewModel::class.java)
        adapter = AppointmentAdapter(emptyList()) { appointment ->
            onCancelAppointment(appointment)
        }

        binding.appointmentsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.appointmentsRecyclerView.adapter = adapter

        noAppointmentsTextView.visibility = if (viewModel.appointments.value.isNullOrEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }

        viewModel.appointments.observe(viewLifecycleOwner) { appointments ->
            if (appointments.isEmpty()) {
                noAppointmentsTextView.visibility = View.VISIBLE
                binding.appointmentsRecyclerView.visibility = View.GONE
            } else {
                noAppointmentsTextView.visibility = View.GONE
                binding.appointmentsRecyclerView.visibility = View.VISIBLE
                adapter.updateAppointments(appointments)
            }
        }

        viewModel.fetchAppointments()

        binding.tvUpcoming.setOnClickListener {
            filterAppointments("Upcoming")
            selectButton(binding.tvUpcoming)
        }
        binding.tvCompleted.setOnClickListener {
            filterAppointments("Completed")
            selectButton(binding.tvCompleted)
        }
        binding.tvCanceled.setOnClickListener {
            filterAppointments("Canceled")
            selectButton(binding.tvCanceled)
        }
    }

    private fun selectButton(status: TextView?) {
        selectedStatus?.apply {
            setTextColor(requireContext().getColor(R.color.gray_900))
            setBackgroundResource(R.drawable.bg_white_radius05)
        }

        if (status != null && selectedStatus != status) {
            status.setTextColor(requireContext().getColor(R.color.white))
            status.setBackgroundResource(R.drawable.bg_blue500_radius05)
            selectedStatus = status
        } else {
            selectedStatus = null
        }
    }

    private fun filterAppointments(status: String) {
        val filtered = viewModel.filterAppointments(status)
        adapter.updateAppointments(filtered)
    }

    private fun onCancelAppointment(appointment: Appointment) {
        viewModel.cancelAppointment(requireContext(), appointment)
        selectButton(null)
    }
}
