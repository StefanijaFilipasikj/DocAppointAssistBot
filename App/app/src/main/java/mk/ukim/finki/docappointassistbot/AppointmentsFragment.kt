package mk.ukim.finki.docappointassistbot

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
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

    private lateinit var database: DatabaseReference
    private var role: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppointmentsBinding.inflate(inflater, container, false)

        database = FirebaseDatabase.getInstance().getReference("users")

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        noAppointmentsTextView = binding.noAppointmentsTextView

        viewModel = activityViewModels<AppointmentsViewModel>().value

        adapter = AppointmentAdapter(
            emptyList(),
            onClick = { appointment -> onClickAppointment(appointment) },
            onCancel = { appointment -> onCancelAppointment(appointment) },
            enableCancel = true
        )

        binding.appointmentsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.appointmentsRecyclerView.adapter = adapter

        val user = FirebaseAuth.getInstance().currentUser

        if (user != null) {
            checkUserRoleAndLoadAppointments(user.uid)
        } else {
            noAppointmentsTextView.visibility = View.VISIBLE
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

        onFilterClick(binding.tvUpcoming, "Upcoming")
        onFilterClick(binding.tvCompleted, "Completed")
        onFilterClick(binding.tvCanceled, "Canceled")
    }

    private fun checkUserRoleAndLoadAppointments(userId: String) {
        database.child(userId).child("role").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                role = snapshot.getValue(String::class.java)
                if (role == "doctor") {
                    viewModel.fetchAppointmentsForDoctor(userId)
                } else {
                    setupPatientAppointments()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                noAppointmentsTextView.visibility = View.VISIBLE
            }
        })
    }

    private fun setupPatientAppointments() {
        noAppointmentsTextView.visibility = if (viewModel.appointments.value.isNullOrEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun selectButton(status: TextView?) {
        selectedStatus?.apply {
            setTextColor(requireContext().getColor(R.color.gray_900))
            setBackgroundResource(android.R.color.transparent)
        }

        if (status != null && selectedStatus != status) {
            status.setTextColor(requireContext().getColor(R.color.white))
            status.setBackgroundResource(R.drawable.bg_blue500_radius05)
            selectedStatus = status
        } else {
            selectedStatus = null
        }
    }

    private fun onFilterClick(button: TextView, status: String) {
        button.setOnClickListener {
            if (selectedStatus == button) {
                showAllAppointments()
                selectButton(null)
            } else {
                filterAppointments(status)
                selectButton(button)
            }
        }
    }

    private fun showAllAppointments() {
        adapter.updateAppointments(viewModel.appointments.value ?: emptyList())
    }

    private fun filterAppointments(status: String) {
        val filtered = if (role == "doctor") {
            viewModel.filterDoctorAppointmentsByStatus(status)
        } else {
            viewModel.filterAppointments(status)
        }

        if (filtered.isEmpty()) {
            noAppointmentsTextView.visibility = View.VISIBLE
            binding.appointmentsRecyclerView.visibility = View.GONE
        } else {
            noAppointmentsTextView.visibility = View.GONE
            binding.appointmentsRecyclerView.visibility = View.VISIBLE
        }

        adapter.updateAppointments(filtered)
    }


    private fun onCancelAppointment(appointment: Appointment) {
        viewModel.cancelAppointment(requireContext(), appointment)
        selectButton(null)
    }

    private fun onClickAppointment(appointment: Appointment) {
        val bundle = bundleOf("appointmentId" to appointment.id)

        val fragment = AppointmentDetailsFragment().apply {
            arguments = bundle
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, fragment)
            .addToBackStack(null)
            .commit()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}