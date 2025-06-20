package mk.ukim.finki.docappointassistbot.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import mk.ukim.finki.docappointassistbot.databinding.ItemDoctorRequestBinding
import mk.ukim.finki.docappointassistbot.domain.DoctorRequest
import com.google.firebase.database.FirebaseDatabase
import android.widget.Toast
import mk.ukim.finki.docappointassistbot.domain.Doctor

class AdminRequestAdapter(
    private val requests: List<DoctorRequest>,
    private val onStatusChanged: () -> Unit
) : RecyclerView.Adapter<AdminRequestAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemDoctorRequestBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDoctorRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = requests.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val request = requests[position]
        val context = holder.itemView.context
        with(holder.binding) {
            tvDoctorFullName.text = request.fullName
            tvSpecialty.text = request.specialty

            btnViewDocuments.setOnClickListener {
                Toast.makeText(context, "View Documents clicked.", Toast.LENGTH_SHORT).show()
            }

            btnApprove.setOnClickListener {
                updateStatus(request.userId, "Approved", context)
            }

            btnDeny.setOnClickListener {
                updateStatus(request.userId, "Denied", context)
            }
        }
    }

    private fun updateStatus(userId: String, newStatus: String, context: Context) {
        val db = FirebaseDatabase.getInstance()
        val doctorRequestsRef = db.getReference("doctorRequests")
        val usersRef = db.getReference("users").child(userId)
        val doctorsRef = db.getReference("doctors")

        doctorRequestsRef.get().addOnSuccessListener { dataSnapshot ->
            val matchingChild = dataSnapshot.children.firstOrNull { child ->
                val request = child.getValue(DoctorRequest::class.java)
                request?.userId == userId
            }

            matchingChild?.ref?.child("status")?.setValue(newStatus)?.addOnSuccessListener {
                onStatusChanged()

                if (newStatus == "Approved") {
                    usersRef.child("role").setValue("doctor")
                        .addOnSuccessListener {
                            val request = matchingChild.getValue(DoctorRequest::class.java)
                            if (request != null) {
                                val newDoctor = Doctor(
                                    id = userId,
                                    fullname = request.fullName,
                                    image = request.profileImageUrl,
                                    city = request.city,
                                    country = request.country,
                                    specialty = request.specialty,
                                    bio = request.bio,
                                    experience = request.experience,
                                    hospitalId = request.hospitalId,
                                    workHourIds = request.workHourIds,
                                )

                                val newDoctorRef = doctorsRef.child(userId)

                                newDoctorRef.setValue(newDoctor)
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Doctor created successfully", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(context, "Failed to create doctor", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                }
            }
        }
    }

}
