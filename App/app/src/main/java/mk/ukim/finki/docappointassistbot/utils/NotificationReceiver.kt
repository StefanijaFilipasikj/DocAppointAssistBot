package mk.ukim.finki.docappointassistbot.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appointmentId = intent.getStringExtra("appointmentId")
        val doctorName = intent.getStringExtra("doctorName")
        val appointmentTime = intent.getStringExtra("appointmentTime")

        // Create the notification channel
        val channelId = "appointment_reminder_channel"
        val channelName = "Appointment Reminders"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        // Build the notification
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Appointment Reminder")
            .setContentText("You have an appointment with $doctorName at $appointmentTime.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(appointmentId.hashCode(), notification)
    }
}
