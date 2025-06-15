package mk.ukim.finki.docappointassistbot.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import mk.ukim.finki.docappointassistbot.databinding.ItemDoctorBinding
import mk.ukim.finki.docappointassistbot.domain.Doctor
import mk.ukim.finki.docappointassistbot.R

class DoctorAdapter(
    private var doctors : List<Doctor>,
    private val onDoctorClick: (Doctor) -> Unit
) : RecyclerView.Adapter<DoctorAdapter.ViewHolder>() {

    class ViewHolder(val binding : ItemDoctorBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemDoctorBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return doctors.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = doctors[position]
        holder.apply {
            binding.apply {
                tvFullname.text = currentItem.fullname
                tvCityAndCountry.text = "${currentItem.city}, ${currentItem.country}"
                tvSpecialty.text = "Specialty: ${currentItem.specialty}"

                Glide.with(holder.itemView.context)
                    .load(currentItem.image)
                    .centerCrop()
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(doctorImage)

                btnViewDoctorDetails.setOnClickListener{
                    onDoctorClick(currentItem)
                }
            }
        }
    }

    fun updateDoctors(newDoctors: List<Doctor>) {
        this.doctors = newDoctors
        notifyDataSetChanged()
    }
}
