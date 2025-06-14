package mk.ukim.finki.docappointassistbot.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import mk.ukim.finki.docappointassistbot.databinding.ItemSpecialtyBinding

class SpecialtyAdapter(
    private val specialties: List<Pair<String, Int>>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<SpecialtyAdapter.ViewHolder>() {

    class ViewHolder(val binding : ItemSpecialtyBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): mk.ukim.finki.docappointassistbot.adapter.SpecialtyAdapter.ViewHolder {
        return ViewHolder(ItemSpecialtyBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: mk.ukim.finki.docappointassistbot.adapter.SpecialtyAdapter.ViewHolder, position: Int) {
        val (specialtyName, imageRes) = specialties[position]
        holder.apply {
            binding.apply {
                tvSpecialtyName.text = specialtyName
                imgSpecialty.setImageResource(imageRes)

                itemView.setOnClickListener{
                    onItemClick(specialtyName)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return specialties.size
    }
}
