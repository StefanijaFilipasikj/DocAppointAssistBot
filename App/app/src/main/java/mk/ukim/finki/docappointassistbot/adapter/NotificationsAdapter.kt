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

class NotificationsAdapter(var notifications: List<Notification>, var notificationStates: Map<Int, Boolean>, private val onNotificationStateChanged: (Int, Boolean) -> Unit) :
    RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.notificationIcon)
        private val title: TextView = itemView.findViewById(R.id.notificationTitle)
        private val subtitle: TextView = itemView.findViewById(R.id.notificationSubtitle)
        private val switch: Switch = itemView.findViewById(R.id.notificationSwitch)

        fun bind(
            notification: Notification,
            isChecked: Boolean,
            isPast: Boolean,
            onNotificationStateChanged: (Int, Boolean) -> Unit
        ) {
            val appointment = notification.appointment
            val doctor = appointment.doctor
            title.text = appointment.startTime
            val hospitalName = doctor?.hospital?.name ?: "Unknown Hospital"
            subtitle.text = "${doctor?.fullname} | ${doctor?.specialty} | $hospitalName"

            icon.setImageResource(R.drawable.ic_baseline_bell_24)

            switch.setOnCheckedChangeListener(null)

            switch.isChecked = isChecked

            switch.isEnabled = !isPast

            switch.setOnCheckedChangeListener { _, isCheckedNow ->
                if (switch.isEnabled) {
                    onNotificationStateChanged(notification.id, isCheckedNow)
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

        val isPast = notification.appointment.status == "Completed"

        holder.bind(notification, isChecked, isPast, onNotificationStateChanged)
    }

    override fun getItemCount() = notifications.size
}
