package mk.ukim.finki.docappointassistbot.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import mk.ukim.finki.docappointassistbot.R

class TimeSlotAdapter(private val onItemClick: (String) -> Unit) :
    RecyclerView.Adapter<TimeSlotAdapter.TimeSlotViewHolder>() {

    private val timeSlots = mutableListOf<String>()
    private val bookedTimeSlots = mutableListOf<String>()
    private var selectedPosition: Int = RecyclerView.NO_POSITION

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeSlotViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_time_slot, parent, false)
        return TimeSlotViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimeSlotViewHolder, position: Int) {
        val timeSlot = timeSlots[position]
        val isBooked = bookedTimeSlots.contains(timeSlot)
        holder.bind(timeSlot, position == selectedPosition, isBooked)

        holder.itemView.setOnClickListener {
            if (isBooked) return@setOnClickListener // ignore click

            val previousPosition = selectedPosition
            selectedPosition = holder.adapterPosition

            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)

            onItemClick(timeSlot)
        }
    }

    override fun getItemCount(): Int {
        return timeSlots.size
    }

    fun updateSlots(newSlots: List<String>, bookedSlots: List<String>) {
        timeSlots.clear()
        timeSlots.addAll(newSlots)
        bookedTimeSlots.clear()
        bookedTimeSlots.addAll(bookedSlots)
        notifyDataSetChanged()
    }

    class TimeSlotViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTimeSlot: TextView = view.findViewById(R.id.tvTimeSlot)

        fun bind(timeSlot: String, isSelected: Boolean, isBooked: Boolean) {
            tvTimeSlot.text = timeSlot
            itemView.isSelected = isSelected
            itemView.isEnabled = !isBooked

            when {
                isBooked -> {
                    tvTimeSlot.setTextColor(itemView.context.getColor(R.color.gray_600))
                    itemView.setBackgroundResource(R.drawable.time_slot_booked)
                }
                isSelected -> {
                    tvTimeSlot.setTextColor(itemView.context.getColor(R.color.white))
                    itemView.setBackgroundResource(R.drawable.time_slot_selected)
                }
                else -> {
                    tvTimeSlot.setTextColor(itemView.context.getColor(R.color.gray_900))
                    itemView.setBackgroundResource(R.drawable.time_slot_unselected)
                }
            }

        }
    }

}
