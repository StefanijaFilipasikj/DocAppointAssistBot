package mk.ukim.finki.docappointassistbot.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import mk.ukim.finki.docappointassistbot.R
import mk.ukim.finki.docappointassistbot.domain.DayWorkHour

class WorkHoursAdapter(
    private val days: List<String>,
    private val hoursList: List<String>
) : RecyclerView.Adapter<WorkHoursAdapter.WorkHoursViewHolder>() {

    val selectedFromTimes = mutableMapOf<String, String>()
    val selectedToTimes = mutableMapOf<String, String>()

    inner class WorkHoursViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dayLabel: TextView = itemView.findViewById(R.id.dayLabel)
        val timeFromSpinner: Spinner = itemView.findViewById(R.id.timeFromSpinner)
        val timeToSpinner: Spinner = itemView.findViewById(R.id.timeToSpinner)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkHoursViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_doctor_work_hour, parent, false)
        return WorkHoursViewHolder(view)
    }

    var isUserInteraction = false

    override fun onBindViewHolder(holder: WorkHoursViewHolder, position: Int) {
        val day = days[position]
        holder.dayLabel.text = day

        val adapter = ArrayAdapter(holder.itemView.context, android.R.layout.simple_spinner_item, hoursList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        holder.timeFromSpinner.adapter = adapter
        holder.timeToSpinner.adapter = adapter

        isUserInteraction = false
        holder.timeFromSpinner.setSelection(hoursList.indexOf(selectedFromTimes[day] ?: "00:00"))
        holder.timeToSpinner.setSelection(hoursList.indexOf(selectedToTimes[day] ?: "00:00"))
        isUserInteraction = true

        holder.timeFromSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                if (!isUserInteraction) return
                selectedFromTimes[day] = hoursList[pos]
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        holder.timeToSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                if (!isUserInteraction) return
                selectedToTimes[day] = hoursList[pos]
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    override fun getItemCount() = days.size

    fun getSelectedWorkHours(): List<DayWorkHour> {
        return days.map { day ->
            val start = selectedFromTimes[day] ?: "00:00"
            val end = selectedToTimes[day] ?: "00:00"
            DayWorkHour(day, start, end)
        }
    }
}