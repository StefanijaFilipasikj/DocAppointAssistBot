package mk.ukim.finki.docappointassistbot.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Restoring appointments after reboot...")

            val savedAppointments = NotificationScheduler.getSavedNotificationStates(context)

            for ((_, appointment) in savedAppointments) {
                if (appointment.status == "Upcoming") {
                    NotificationScheduler.scheduleNotification(context, appointment)
                } else {
                    // Clean up any leftover notifications just in case
                    NotificationScheduler.cancelNotification(context, appointment.id)
                }
            }

        }
    }
}
