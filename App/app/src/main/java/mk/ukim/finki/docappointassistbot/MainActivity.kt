package mk.ukim.finki.docappointassistbot

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.auth.FirebaseAuth
import mk.ukim.finki.docappointassistbot.databinding.ActivityMainBinding
import mk.ukim.finki.docappointassistbot.domain.repository.AppointmentsRepository

class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private val NOTIFICATION_PERMISSION = android.Manifest.permission.POST_NOTIFICATIONS
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar: Toolbar = findViewById(R.id.topNavigationView)
        setSupportActionBar(toolbar)

        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            view.setPadding(0, topInset, 0, 0)
            insets
        }

        checkNotificationAndAlarmPermissions()

        AppointmentsRepository().checkAndUpdateStatusesForCurrentUser()

        // Bottom navigation bar
        replaceFragment(HomeFragment())
        binding.bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.home -> replaceFragment(HomeFragment())
                R.id.doctors -> replaceFragment(DoctorsFragment())
                R.id.chatbot -> replaceFragment(ChatbotFragment())
                R.id.appointments -> replaceFragment(AppointmentsFragment())
                R.id.settings -> replaceFragment(SettingsFragment())
                else -> {}
            }
            true
        }

        createNotificationChannel()

    }

    // Top navigation bar
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_nav, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val user = FirebaseAuth.getInstance().currentUser
        val loginItem = menu.findItem(R.id.login)

        loginItem.setIcon(R.drawable.ic_baseline_user_24)
        user?.let {
            Glide.with(this)
                .asBitmap()
                .load(it.photoUrl)
                .placeholder(R.drawable.ic_baseline_user_24)
                .circleCrop()
                .into(object : CustomTarget<Bitmap>(64, 64) {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        val drawable = BitmapDrawable(resources, resource)
                        loginItem.icon = drawable
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                    }
                })
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.login -> {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    val intent = Intent(this, UserInfoActivity::class.java)
                    startActivity(intent)
                } else {
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                }
            }

            R.id.notifications -> replaceFragment(NotificationsFragment())
            else -> {}
        }
        return true
    }

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frameLayout, fragment)
        fragmentTransaction.commit()
    }

    private fun checkNotificationAndAlarmPermissions() {
        val needsNotificationPermission =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(this, NOTIFICATION_PERMISSION) != android.content.pm.PackageManager.PERMISSION_GRANTED

        val needsExactAlarmPermission =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    !(getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()

        if (needsNotificationPermission || needsExactAlarmPermission) {
            showPermissionDialog()
        }
    }

    private fun showPermissionDialog() {
        val message = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            "This app requires permission to send notifications and schedule exact alarms for your appointments. Please grant the necessary permissions in Settings."
        } else {
            "This app requires permission to send notifications for your appointments. Please grant permission in Settings."
        }

        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage(message)
            .setPositiveButton("Grant Permissions") { _, _ ->
                // Open the App Settings page
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)

                // Guide user to the alarm settings
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val alarmIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    startActivity(alarmIntent)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    // Ask for POST_NOTIFICATIONS permission directly
    override fun onResume() {
        super.onResume()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, NOTIFICATION_PERMISSION) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(NOTIFICATION_PERMISSION),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    // Handle permission request result (for POST_NOTIFICATIONS)
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with scheduling notifications/alarms
            } else {
                // Permission denied, inform the user
                AlertDialog.Builder(this)
                    .setTitle("Permission Denied")
                    .setMessage("Without this permission, the app cannot send notifications or reminders.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "appointment_reminder_channel"
            val channelName = "Appointment Reminders"
            val channelDescription = "Notifies users about upcoming appointments"
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

}
