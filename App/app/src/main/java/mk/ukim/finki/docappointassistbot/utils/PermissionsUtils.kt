package mk.ukim.finki.docappointassistbot.utils

import android.app.AlarmManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit

object PermissionsUtils {

    const val NOTIFICATION_PERMISSION = android.Manifest.permission.POST_NOTIFICATIONS
    const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001

    fun needsNotificationPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, NOTIFICATION_PERMISSION) != PackageManager.PERMISSION_GRANTED
    }

    fun needsExactAlarmPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
    }

    fun checkNotificationAndAlarmPermissions(context: Context, onShowDialog: (AlertDialog.Builder) -> Unit) {
        val sharedPref = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val askedBefore = sharedPref.getBoolean("notification_permission_asked", false)

        val needsNotification = needsNotificationPermission(context)
        val needsAlarm = needsExactAlarmPermission(context)

        if ((needsNotification || needsAlarm) && !askedBefore) {
            sharedPref.edit() { putBoolean("notification_permission_asked", true) }

            val message = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                "This app requires permission to send notifications and schedule exact alarms for your appointments. Please grant the necessary permissions in Settings."
            } else {
                "This app requires permission to send notifications for your appointments. Please grant permission in Settings."
            }

            val builder = AlertDialog.Builder(context)
                .setTitle("Permissions Required")
                .setMessage(message)
                .setPositiveButton("Grant Permissions") { _, _ ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val alarmIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        context.startActivity(alarmIntent)
                    }
                }
                .setNegativeButton("Cancel", null)

            onShowDialog(builder)
        }
    }

    fun requestNotificationPermission(activity: androidx.fragment.app.FragmentActivity) {
        if (needsNotificationPermission(activity)) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(NOTIFICATION_PERMISSION),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    fun handlePermissionResult(
        requestCode: Int,
        grantResults: IntArray,
        context: Context,
        onDenied: () -> Unit
    ) {
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            val sharedPref = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sharedPref.edit() { putBoolean("notifications_enabled", true) }
            } else {
                val alreadyHandled = sharedPref.getBoolean("notification_permission_handled", false)

                if (!alreadyHandled) {
                    sharedPref.edit() { putBoolean("notification_permission_handled", true) }

                    AlertDialog.Builder(context)
                        .setTitle("Permission Denied")
                        .setMessage("Without this permission, the app cannot send notifications or reminders.")
                        .setPositiveButton("OK", null)
                        .show()

                    onDenied()
                }
            }
        }
    }

    fun isNotificationPermissionGranted(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, NOTIFICATION_PERMISSION) == PackageManager.PERMISSION_GRANTED
    }

}
