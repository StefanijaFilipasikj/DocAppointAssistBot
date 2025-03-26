package mk.ukim.finki.docappointassistbot.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import mk.ukim.finki.docappointassistbot.domain.Notification
import mk.ukim.finki.docappointassistbot.R

class NotificationsAdapter(private val notifications: List<Notification>) :
    RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.notificationIcon)
        private val title: TextView = itemView.findViewById(R.id.notificationTitle)
        private val subtitle: TextView = itemView.findViewById(R.id.notificationSubtitle)

        fun bind(notification: Notification) {
            val appointment = notification.appointment
            val doctor = appointment.doctor

            // Set title (appointment date and time)
            title.text = appointment.startTime

            // Set subtitle (doctor's name, specialty, and hospital name)
            val hospitalName = doctor?.hospitals?.firstOrNull()?.name ?: "Unknown Hospital"
            subtitle.text = "${doctor?.fullname} | ${doctor?.specialty} | $hospitalName"

            // Set the notification icon
            icon.setImageResource(R.drawable.ic_baseline_bell_24)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount() = notifications.size
}
