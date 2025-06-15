package mk.ukim.finki.docappointassistbot.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import mk.ukim.finki.docappointassistbot.R
import mk.ukim.finki.docappointassistbot.databinding.ItemAppointmentBinding
import mk.ukim.finki.docappointassistbot.domain.Appointment

class AppointmentAdapter(private var appointments: List<Appointment>,
                         private val onCancel: (Appointment) -> Unit,
                         private val onClick: (Appointment) -> Unit) :
    RecyclerView.Adapter<AppointmentAdapter.ViewHolder>() {

    class ViewHolder(val binding : ItemAppointmentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemAppointmentBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = appointments[position]
        holder.apply{
            binding.apply{
                tvDoctorFullName.text = currentItem.doctor?.fullname ?: "Unknown Doctor"
                tvSpecialty.text = currentItem.doctor?.specialty ?: "Specialty Not Specified"
                tvHospital.text = currentItem.doctor?.hospital?.name ?: "Hospital Not Specified"
                tvAppointmentDateTime.text = "${currentItem.startTime}"

                Glide.with(holder.itemView.context)
                    .load(currentItem.doctor?.image)
                    .centerCrop()
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(doctorImage)

                binding.cardView.setOnClickListener {
                    onClick(currentItem)
                }

                if (currentItem.status.equals("Upcoming", ignoreCase = true)){
                    holder.itemView.alpha = 1.0f
                    cardView.setBackgroundResource(R.drawable.bg_white_radius10)

                    binding.btnCancel.visibility = View.VISIBLE
                    binding.btnCancel.setOnClickListener {
                        onCancel(currentItem)
                    }
                }else if(currentItem.status.equals("Completed", ignoreCase = true)){
                    cardView.setBackgroundResource(R.drawable.bg_blue200_radius10)
                    binding.btnCancel.visibility = View.GONE
                }else{ //Canceled
                    holder.itemView.alpha = 0.5f
                    cardView.setBackgroundResource(R.drawable.bg_gray300_radius10)
                    binding.btnCancel.visibility = View.GONE
                }
            }
        }

    }

    override fun getItemCount(): Int {
        return appointments.size
    }

    fun updateAppointments(newAppointments: List<Appointment>) {
        appointments = newAppointments
        notifyDataSetChanged()
    }
}
