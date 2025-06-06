package mk.ukim.finki.docappointassistbot.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.gson.Gson
import mk.ukim.finki.docappointassistbot.domain.Appointment
import java.text.SimpleDateFormat
import java.util.*

object NotificationScheduler {

    private const val PREFS_NAME = "appointments"

    fun scheduleNotification(context: Context, appointment: Appointment) {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm a", Locale.getDefault())
        val appointmentTime = format.parse(appointment.startTime) ?: return

        // Check if the appointment time is in the future
        if (appointment.status != "Upcoming") {
            Log.d("NotificationScheduler", "Skipping notification for past appointment: ${appointment.id}")
            return // Don't schedule notification for past appointments
        }

        val reminderTime = Calendar.getInstance().apply {
            time = appointmentTime
            add(Calendar.HOUR_OF_DAY, -24) // 24 hours before
        }.timeInMillis

        val doctorName = appointment.doctor?.fullname ?: "Unknown Doctor"

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("appointmentId", appointment.id.toString())
            putExtra("doctorName", doctorName)
            putExtra("appointmentTime", appointment.startTime)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appointment.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                Log.w("NotificationScheduler", "Exact alarms not allowed for this app.")
                return
            }

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                pendingIntent
            )

            Log.d("NotificationScheduler", "Notification scheduled for appointment ID: ${appointment.id} at time: $reminderTime")

            saveAppointment(context, appointment)

        } catch (e: SecurityException) {
            Log.e("NotificationScheduler", "Failed to schedule exact alarm: ${e.message}")
        }
    }


    fun cancelNotification(context: Context, appointmentId: Int) {
        val intent = Intent(context, NotificationReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appointmentId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)

        Log.d("NotificationScheduler", "Notification canceled for appointment ID: $appointmentId")

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove("appointment_$appointmentId").apply()
    }

    private fun saveAppointment(context: Context, appointment: Appointment) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "appointment_${appointment.id}"
        val json = Gson().toJson(appointment)
        prefs.edit().putString(key, json).apply()
    }

    fun getSavedNotificationStates(context: Context): Map<Int, Appointment> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val gson = Gson()
        return prefs.all.mapNotNull { (key, value) ->
            if (key.startsWith("appointment_") && value is String) {
                try {
                    val appointment = gson.fromJson(value, Appointment::class.java)
                    appointment?.let { it.id to it }
                } catch (e: Exception) {
                    null
                }
            } else null
        }.toMap()
    }


    fun cancelAllNotifications(context: Context) {
        val savedAppointments = getSavedNotificationStates(context)
        for ((id, _) in savedAppointments) {
            cancelNotification(context, id)
        }

        // Clear all shared preferences
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        Log.d("NotificationScheduler", "All notifications and saved appointments cleared.")
    }

}
