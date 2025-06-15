package mk.ukim.finki.docappointassistbot.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import mk.ukim.finki.docappointassistbot.R
import mk.ukim.finki.docappointassistbot.databinding.ItemSpecialtyBinding
import mk.ukim.finki.docappointassistbot.domain.Specialty

class SpecialtyAdapter(
    private val specialties: List<Specialty>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<SpecialtyAdapter.ViewHolder>() {

    class ViewHolder(val binding : ItemSpecialtyBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): mk.ukim.finki.docappointassistbot.adapter.SpecialtyAdapter.ViewHolder {
        return ViewHolder(ItemSpecialtyBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: mk.ukim.finki.docappointassistbot.adapter.SpecialtyAdapter.ViewHolder, position: Int) {
        val specialty = specialties[position]
        val context = holder.itemView.context

        holder.apply {
            binding.apply {
                tvSpecialtyName.text = context.getString(specialty.nameResId)
                imgSpecialty.setImageResource(specialty.imageResId)

                val layoutParams = holder.itemView.layoutParams as ViewGroup.MarginLayoutParams
                val margin = (28 * holder.itemView.context.resources.displayMetrics.density).toInt()
                layoutParams.marginStart = if (position == 0) 0 else margin
                layoutParams.marginEnd = if (position == specialties.size - 1) 0 else 0

                holder.itemView.layoutParams = layoutParams

                itemView.setOnClickListener{
                    onItemClick(specialty.key)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return specialties.size
    }
}
