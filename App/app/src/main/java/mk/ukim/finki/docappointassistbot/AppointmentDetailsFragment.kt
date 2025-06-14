package mk.ukim.finki.docappointassistbot

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import mk.ukim.finki.docappointassistbot.databinding.FragmentAppointmentDetailsBinding
import mk.ukim.finki.docappointassistbot.domain.Appointment
import mk.ukim.finki.docappointassistbot.ui.viewModels.AppointmentsViewModel

class AppointmentDetailsFragment : Fragment() {

    private var _binding: FragmentAppointmentDetailsBinding? = null
    private val binding get() = _binding!!

    private lateinit var appointment: Appointment
    private val viewModel: AppointmentsViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppointmentDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val appointmentId = requireArguments().getInt("appointmentId")
        viewModel.appointments.observe(viewLifecycleOwner) { list ->
            val appointmentFound = list.firstOrNull { it.id == appointmentId }
            if (appointmentFound == null) {
                return@observe
            }

            appointment = appointmentFound
            fun showTextMode() {
                val details = appointment.details?.trim()
                if (details.isNullOrEmpty()) {
                    binding.detailsText.text = getString(R.string.enter_appointment_details)
                } else {
                    binding.detailsText.text = details
                }
                binding.detailsText.visibility = View.VISIBLE
                binding.editButton.visibility = View.VISIBLE
                binding.detailsInput.visibility = View.GONE
                binding.saveButton.visibility = View.GONE
            }

            fun showEditMode() {
                binding.detailsInput.setText(appointment.details)
                binding.detailsText.visibility = View.GONE
                binding.editButton.visibility = View.GONE
                binding.detailsInput.visibility = View.VISIBLE
                binding.saveButton.visibility = View.VISIBLE
            }

            binding.editButton.setOnClickListener {
                showEditMode()
            }

            binding.saveButton.setOnClickListener {
                val newDetails = binding.detailsInput.text.toString().trim()
                if (newDetails.isNotBlank()) {
                    viewModel.updateAppointmentDetails(appointment.id, newDetails)
                    showTextMode()
                }
            }
            showTextMode()
        }
    }
}
