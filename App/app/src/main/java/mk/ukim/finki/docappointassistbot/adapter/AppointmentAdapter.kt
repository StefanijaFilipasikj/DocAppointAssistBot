package mk.ukim.finki.docappointassistbot.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import mk.ukim.finki.docappointassistbot.R
import mk.ukim.finki.docappointassistbot.databinding.ItemAppointmentBinding
import mk.ukim.finki.docappointassistbot.domain.Appointment
import androidx.core.view.isVisible

class AppointmentAdapter(
    private var appointments: List<Appointment>,
    private val onCancel: (Appointment) -> Unit,
    private val onClick: (Appointment) -> Unit,
    private val enableCancel: Boolean = true
) : RecyclerView.Adapter<AppointmentAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemAppointmentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemAppointmentBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = appointments[position]
        holder.itemView.context
        val binding = holder.binding

        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            val database = FirebaseDatabase.getInstance().getReference("users")

            database.child(currentUser.uid).child("role")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val role = snapshot.getValue(String::class.java)

                        when (role) {
                            "doctor" -> bindDoctorView(holder, binding, currentItem)
                            else -> bindPatientView(holder, binding, currentItem)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                })
        }

        binding.cardView.setOnClickListener { onClick(currentItem) }

        setStatusColor(holder, binding, currentItem)
    }

    override fun getItemCount(): Int = appointments.size

    fun updateAppointments(newAppointments: List<Appointment>) {
        appointments = newAppointments
        notifyDataSetChanged()
    }

    private fun bindDoctorView(holder: ViewHolder, binding: ItemAppointmentBinding, currentItem: Appointment) {
        binding.tvSpecialty.visibility = View.GONE
        binding.tvAppointmentDateTime.text = currentItem.startTime

        val hospitalId = currentItem.doctor?.hospitalId
        if (hospitalId != null) {
            getHospitalName(hospitalId) { hospitalName ->
                binding.tvHospital.text = hospitalName
            }
        } else {
            binding.tvHospital.text = "Hospital Not Specified"
        }

        getPatientInfo(currentItem.userId) { patientFullName, patientPhotoUrl ->
            binding.tvDoctorFullName.text = patientFullName
            Glide.with(holder.itemView.context)
                .load(patientPhotoUrl)
                .centerCrop()
                .placeholder(getUserPlaceholder(holder.itemView.context))
                .into(binding.doctorImage)
        }

        binding.btnCancel.visibility = View.GONE
    }

    private fun getHospitalName(hospitalId: Int, callback: (String) -> Unit) {
        val database = FirebaseDatabase.getInstance().getReference("hospitals")
        database.child(hospitalId.toString()).child("name")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val hospitalName = snapshot.getValue(String::class.java) ?: "Hospital Not Specified"
                    callback(hospitalName)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback("Hospital Not Specified")
                }
            })
    }

    private fun bindPatientView(holder: ViewHolder, binding: ItemAppointmentBinding, currentItem: Appointment) {
        binding.tvDoctorFullName.text = currentItem.doctor?.fullname ?: "Unknown Doctor"
        binding.tvSpecialty.text = currentItem.doctor?.specialty ?: "Specialty Not Specified"
        binding.tvSpecialty.visibility = View.VISIBLE
        binding.tvAppointmentDateTime.text = currentItem.startTime
        binding.tvHospital.text = currentItem.doctor?.hospital?.name ?: "Hospital Not Specified"

        Glide.with(holder.itemView.context)
            .load(currentItem.doctor?.image)
            .centerCrop()
            .placeholder(getUserPlaceholder(holder.itemView.context))
            .into(binding.doctorImage)

        binding.btnCancel.visibility = if (enableCancel && currentItem.status.equals("Upcoming", ignoreCase = true)) {
            View.VISIBLE
        } else {
            View.GONE
        }

        if (binding.btnCancel.isVisible) {
            binding.btnCancel.setOnClickListener { onCancel(currentItem) }
        }
    }

    private fun setStatusColor(holder: ViewHolder, binding: ItemAppointmentBinding, currentItem: Appointment) {
        when {
            currentItem.status.equals("Upcoming", ignoreCase = true) -> {
                holder.itemView.alpha = 1.0f
                binding.cardView.setBackgroundResource(R.drawable.bg_white_radius10)
            }
            currentItem.status.equals("Completed", ignoreCase = true) -> {
                holder.itemView.alpha = 1.0f
                binding.cardView.setBackgroundResource(R.drawable.bg_blue200_radius10)
            }
            else -> { // Canceled
                holder.itemView.alpha = 0.5f
                binding.cardView.setBackgroundResource(R.drawable.bg_gray300_radius10)
            }
        }
    }

    private fun getPatientInfo(userId: String, callback: (String, String?) -> Unit) {
        val database = FirebaseDatabase.getInstance().getReference("users")
        database.orderByChild("email").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val child = snapshot.children.firstOrNull()
                    val patientFullName = child?.child("fullName")?.getValue(String::class.java) ?: "Unknown Patient"
                    val patientPhotoUrl = child?.child("photoUrl")?.getValue(String::class.java)
                    callback(patientFullName, patientPhotoUrl)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback("Unknown Patient", null)
                }
            })
    }

    private fun getUserPlaceholder(context: Context): Int {
        val isDarkMode = (context.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES

        return if (isDarkMode) {
            R.drawable.ic_baseline_user_24_white
        } else {
            R.drawable.ic_baseline_user_24
        }
    }

}
