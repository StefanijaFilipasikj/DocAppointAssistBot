package mk.ukim.finki.docappointassistbot

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import mk.ukim.finki.docappointassistbot.databinding.ActivityMainBinding
import mk.ukim.finki.docappointassistbot.domain.repository.AppointmentsRepository
import mk.ukim.finki.docappointassistbot.utils.PermissionsUtils

class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private var isAdmin = false
    private var isDoctor = false

    private var lastNonNotificationFragment: Fragment? = null
    private var isOnNotificationsScreen = false
    private var isNavItemSelectedProgrammatically = false

    override fun onCreate(savedInstanceState: Bundle?) {

        val sharedPref = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val mode = sharedPref.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_NO)
        AppCompatDelegate.setDefaultNightMode(mode)

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

        checkIfAdmin { admin ->
            isAdmin = admin
            checkIfDoctor { doctor ->
                isDoctor = doctor
                setupBottomNavigation()
                invalidateOptionsMenu()
            }
        }

        createNotificationChannel()

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null){
            binding.bottomNavigationView.menu.findItem(R.id.doctors)?.isVisible = false
            binding.bottomNavigationView.menu.findItem(R.id.chatbot)?.isVisible = false
            binding.bottomNavigationView.menu.findItem(R.id.appointments)?.isVisible = false

            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        supportFragmentManager.addOnBackStackChangedListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.frameLayout)
            when (currentFragment) {
                is HomeFragment -> setSelectedNavItem(R.id.home)
                is DoctorsFragment, is PatientsFragment -> setSelectedNavItem(R.id.doctors)
                is ChatbotFragment -> setSelectedNavItem(R.id.chatbot)
                is AppointmentsFragment -> setSelectedNavItem(R.id.appointments)
                is SettingsFragment -> setSelectedNavItem(R.id.settings)
            }

            invalidateOptionsMenu()
        }
    }

    private fun setupBottomNavigation() {
        // TODO: please find better solution in future
        val sharedPref = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val lastFragment = sharedPref.getString("last_fragment", "")

        if (isAdmin) {
            binding.bottomNavigationView.menu.clear()
            binding.bottomNavigationView.inflateMenu(R.menu.bottom_nav_admin)
            if(lastFragment == "settings"){
                replaceFragment(SettingsFragment())
            }else{
                replaceFragment(AdminRequestsFragment())
            }
        } else if (isDoctor){
            binding.bottomNavigationView.menu.findItem(R.id.home)?.isVisible = false
            binding.bottomNavigationView.menu.findItem(R.id.chatbot)?.isVisible = false
            binding.bottomNavigationView.menu.findItem(R.id.doctors)?.title = getString(R.string.my_patients)
            binding.bottomNavigationView.menu.findItem(R.id.doctors)?.icon = getDrawable(R.drawable.ic_baseline_patients_24)
            if(lastFragment == "settings"){
                replaceFragment(SettingsFragment())
            }else{
                replaceFragment(PatientsFragment())
            }
        }
        else {
            if(lastFragment == "settings"){
                replaceFragment(SettingsFragment())
            }else{
                replaceFragment(HomeFragment())
            }
        }

        binding.bottomNavigationView.setOnItemSelectedListener {
            if (isNavItemSelectedProgrammatically) {
                isNavItemSelectedProgrammatically = false
                return@setOnItemSelectedListener true
            }

            isOnNotificationsScreen = false
            val user = FirebaseAuth.getInstance().currentUser
            when (it.itemId) {
                R.id.home -> replaceFragment(HomeFragment())
                R.id.doctors -> {
                    if (isDoctor)
                        replaceFragment(PatientsFragment())
                    else replaceFragment(DoctorsFragment())
                }
                R.id.chatbot -> replaceFragment(ChatbotFragment.newInstance(user?.email, "patient"))
                R.id.appointments -> replaceFragment(AppointmentsFragment())
                R.id.settings -> replaceFragment(SettingsFragment())
                R.id.requests -> replaceFragment(AdminRequestsFragment())
            }
            true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_nav, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val user = FirebaseAuth.getInstance().currentUser

        if (isAdmin || isDoctor || user == null) {
            menu.findItem(R.id.notifications)?.isVisible = false
        }

        val loginItem = menu.findItem(R.id.login)
        loginItem.setIcon(R.drawable.ic_baseline_user_24_white)

        user?.let {
            Glide.with(this)
                .asBitmap()
                .load(it.photoUrl)
                .placeholder(R.drawable.ic_baseline_user_24_white)
                .circleCrop()
                .into(object : CustomTarget<Bitmap>(64, 64) {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        loginItem.icon = BitmapDrawable(resources, resource)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}
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

            R.id.notifications -> {
                if (isOnNotificationsScreen && lastNonNotificationFragment != null) {
                    replaceFragment(lastNonNotificationFragment!!)
                } else {
                    replaceFragment(NotificationsFragment())
                }
            }
            else -> {}
        }
        return true
    }

    private fun replaceFragment(fragment: Fragment) {
        if (fragment is NotificationsFragment) {
            isOnNotificationsScreen = true
        } else {
            lastNonNotificationFragment = fragment
            isOnNotificationsScreen = false
        }

        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frameLayout, fragment)

        if(fragment !is HomeFragment)
            fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }

    private fun checkNotificationAndAlarmPermissions() {
        PermissionsUtils.checkNotificationAndAlarmPermissions(this) { builder ->
            builder.show()
        }
    }

    // Ask for POST_NOTIFICATIONS permission directly
    override fun onResume() {
        super.onResume()
        PermissionsUtils.requestNotificationPermission(this)
    }

    // Handle permission request result (for POST_NOTIFICATIONS)
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        PermissionsUtils.handlePermissionResult(
            requestCode,
            grantResults,
            this
        ) {}
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
            if (PermissionsUtils.isNotificationPermissionGranted(this)) {
                val notificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    private fun checkIfAdmin(callback: (Boolean) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val database = FirebaseDatabase.getInstance()
            val userRef = database.getReference("users").child(user.uid)

            userRef.get()
                .addOnSuccessListener { snapshot ->
                    val role = snapshot.child("role").getValue(String::class.java)
                    callback(role == "admin")
                }
                .addOnFailureListener {
                    callback(false)
                }
        } else {
            callback(false)
        }
    }

    private fun checkIfDoctor(callback: (Boolean) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val database = FirebaseDatabase.getInstance()
            val userRef = database.getReference("users").child(user.uid)

            userRef.get()
                .addOnSuccessListener { snapshot ->
                    val role = snapshot.child("role").getValue(String::class.java)
                    callback(role == "doctor")
                }
                .addOnFailureListener {
                    callback(false)
                }
        } else {
            callback(false)
        }
    }

    fun setSelectedNavItem(itemId: Int) {
        isNavItemSelectedProgrammatically = true
        binding.bottomNavigationView.selectedItemId = itemId
    }
}
