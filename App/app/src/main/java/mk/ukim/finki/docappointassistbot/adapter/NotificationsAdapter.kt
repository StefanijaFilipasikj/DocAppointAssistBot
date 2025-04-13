package mk.ukim.finki.docappointassistbot.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import mk.ukim.finki.docappointassistbot.domain.Notification
import mk.ukim.finki.docappointassistbot.R
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationsAdapter(private var notifications: List<Notification>, private val notificationStates: Map<Int, Boolean>, private val onNotificationStateChanged: (Int, Boolean) -> Unit) :
    RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.notificationIcon)
        private val title: TextView = itemView.findViewById(R.id.notificationTitle)
        private val subtitle: TextView = itemView.findViewById(R.id.notificationSubtitle)
        private val switch: Switch = itemView.findViewById(R.id.notificationSwitch)

        fun bind(notification: Notification, isChecked: Boolean, isPast: Boolean, onNotificationStateChanged: (Int, Boolean) -> Unit) {
            // Set title and subtitle (appointment details)
            val appointment = notification.appointment
            val doctor = appointment.doctor
            title.text = appointment.startTime
            val hospitalName = doctor?.hospitals?.firstOrNull()?.name ?: "Unknown Hospital"
            subtitle.text = "${doctor?.fullname} | ${doctor?.specialty} | $hospitalName"

            // Set the notification icon
            icon.setImageResource(R.drawable.ic_baseline_bell_24)

            // Set the switch state
            switch.isChecked = isChecked

            // Disable switch for past notifications
            switch.isEnabled = !isPast

            // Handle switch state change
            switch.setOnCheckedChangeListener { _, isChecked ->
                if (switch.isEnabled) {
                    onNotificationStateChanged(notification.id, isChecked)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        val isChecked = notificationStates[notification.id] ?: true

        val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())

        val time = dateFormat.parse(notification.appointment.startTime)?.time ?: 0L
        val isPast = time <= System.currentTimeMillis()

        holder.bind(notification, isChecked, isPast, onNotificationStateChanged)
    }

    override fun getItemCount() = notifications.size
}
