package mk.ukim.finki.docappointassistbot

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import mk.ukim.finki.docappointassistbot.adapter.WorkHoursAdapter
import mk.ukim.finki.docappointassistbot.domain.DoctorRequest
import mk.ukim.finki.docappointassistbot.domain.User
import mk.ukim.finki.docappointassistbot.domain.WorkHours
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DoctorRequestActivity : AppCompatActivity() {

    private lateinit var fullNameEditText: EditText
    private lateinit var profileImageEditText: EditText
    private lateinit var cityEditText: EditText
    private lateinit var countryEditText: EditText
    private lateinit var bioEditText: EditText
    private lateinit var experienceEditText: EditText
    private lateinit var hospitalSpinner: Spinner
    private lateinit var specialtySpinner: Spinner
    private lateinit var uploadCvButton: Button
    private lateinit var cvFileNameTextView: TextView
    private lateinit var workHoursRecyclerView: RecyclerView
    private lateinit var submitButton: Button

    private var selectedCvUri: Uri? = null
    private val selectedWorkHourIds = mutableSetOf<String>()
    private var hospitals = mutableListOf<Pair<Int, String>>()
    private var specialties = mutableListOf<String>()
    private lateinit var pdfPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var workHoursAdapter: WorkHoursAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_request)

        bindViews()
        loadHospitals()
        loadSpecialties()
        loadWorkHours()
        loadCurrentUserData()

        uploadCvButton.setOnClickListener { selectPdfFile() }
        submitButton.setOnClickListener {
            saveWorkHours()
            handleSubmit()
        }

        pdfPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                selectedCvUri = data?.data
                selectedCvUri?.let { uri ->
                    val name = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (cursor.moveToFirst()) cursor.getString(nameIndex) else "CV selected"
                    } ?: "CV selected"
                    cvFileNameTextView.text = name
                }
            }
        }
    }

    private fun bindViews() {
        fullNameEditText = findViewById(R.id.fullNameEditText)
        profileImageEditText = findViewById(R.id.profileImageEditText)
        cityEditText = findViewById(R.id.cityEditText)
        countryEditText = findViewById(R.id.countryEditText)
        specialtySpinner = findViewById(R.id.specialtySpinner)
        bioEditText = findViewById(R.id.bioEditText)
        experienceEditText = findViewById(R.id.experienceEditText)
        hospitalSpinner = findViewById(R.id.hospitalSpinner)
        uploadCvButton = findViewById(R.id.uploadCvButton)
        cvFileNameTextView = findViewById(R.id.cvFileNameTextView)
        workHoursRecyclerView = findViewById(R.id.workHoursRecyclerView)
        submitButton = findViewById(R.id.submitRequestButton)
    }

    private fun loadHospitals() {
        FirebaseDatabase.getInstance()
            .getReference("hospitals")
            .get()
            .addOnSuccessListener { snap ->
                hospitals.clear()
                hospitals.add(Pair(-1, "Select hospital"))
                for (child in snap.children) {
                    val id = child.child("id").getValue(Int::class.java) ?: continue
                    val name = child.child("name").getValue(String::class.java) ?: continue
                    hospitals.add(Pair(id, name))
                }
                val names = hospitals.map { it.second }
                hospitalSpinner.adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    names
                )
            }
            .addOnFailureListener { toast("Failed loading hospitals") }
    }

    private fun loadSpecialties() {
        FirebaseDatabase.getInstance()
            .getReference("specialties")
            .get()
            .addOnSuccessListener { snap ->
                specialties.clear()
                for (child in snap.children) {
                    val specialty = child.getValue(String::class.java) ?: continue
                    specialties.add(specialty)
                }
                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    specialties
                )
                specialtySpinner.adapter = adapter
            }
            .addOnFailureListener { toast("Failed loading specialties") }
    }

    private fun loadCurrentUserData() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance()
            .getReference("users")
            .child(uid)
            .get()
            .addOnSuccessListener { snap ->
                val data = snap.getValue(User::class.java) ?: return@addOnSuccessListener
                fullNameEditText.setText(data.fullName)
                profileImageEditText.setText(data.photoUrl)
            }
    }

    private fun selectPdfFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/pdf"
        }
        pdfPickerLauncher.launch(Intent.createChooser(intent, "Select CV PDF"))
    }

    private fun handleSubmit() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return toast("Please log in")
        val hospitalId = hospitals.getOrNull(hospitalSpinner.selectedItemPosition)?.first
            ?.takeIf { it >= 0 }
        val specialty = specialtySpinner.selectedItem as? String ?: ""

        val request = DoctorRequest(
            userId = uid,
            fullName = fullNameEditText.text.toString(),
            profileImageUrl = profileImageEditText.text.toString(),
            city = cityEditText.text.toString(),
            country = countryEditText.text.toString(),
            specialty = specialty,
            bio = bioEditText.text.toString(),
            experience = experienceEditText.text.toString().toDoubleOrNull() ?: 0.0,
            hospitalId = hospitalId,
            cvUrl = selectedCvUri?.toString() ?: "",
            workHourIds = selectedWorkHourIds.takeIf { it.isNotEmpty() }?.toList(),
            status = "Submitted"
        )

        FirebaseDatabase.getInstance()
            .getReference("doctorRequests")
            .push()
            .setValue(request)
            .addOnSuccessListener {
                val updatedUser = mapOf(
                    "fullName" to fullNameEditText.text.toString(),
                    "photoUrl" to profileImageEditText.text.toString()
                )

                FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(uid)
                    .updateChildren(updatedUser)
                    .addOnSuccessListener {
                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        intent.putExtra("requestSuccess", true)
                        startActivity(intent)
                        toast("Request sent successfully")
                    }
                    .addOnFailureListener { toast("Failed to update user data") }
            }
            .addOnFailureListener { toast("Submission failed") }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun loadWorkHours() {
        val recyclerView = findViewById<RecyclerView>(R.id.workHoursRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val hoursList = (0..23).map { String.format("%02d:00", it) }

        workHoursAdapter = WorkHoursAdapter(days, hoursList)
        recyclerView.adapter = workHoursAdapter
    }

    private fun groupWorkHours(): List<WorkHours> {
        val dayWorkHours = workHoursAdapter.getSelectedWorkHours()
            val dateFormatInput = SimpleDateFormat("yyyy-MM-dd HH:mm:ss a", Locale.ENGLISH)
        val grouped = dayWorkHours.groupBy { it.startTime + "_" + it.endTime }
        val now = Date()
        val calendar = java.util.Calendar.getInstance()

        fun parseTimeToDate(timeStr: String): Date {
            val parsedTime = dateFormatInput.parse(timeStr) ?: Date()
            calendar.time = now
            val cal2 = java.util.Calendar.getInstance()
            cal2.time = parsedTime
            calendar.set(java.util.Calendar.HOUR_OF_DAY, cal2.get(java.util.Calendar.HOUR_OF_DAY))
            calendar.set(java.util.Calendar.MINUTE, cal2.get(java.util.Calendar.MINUTE))
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            return calendar.time
        }

        return grouped.map { (timeRange, days) ->
            val (startStr, endStr) = timeRange.split("_")

            val startDate = parseTimeToDate(startStr)
            val endDate = parseTimeToDate(endStr)

            val daysOfWeekStr = days.joinToString(", ") { it.day.take(3) }

            WorkHours(
                daysOfWeek = daysOfWeekStr,
                startTime = startDate,
                endTime = endDate
            )
        }
    }

    private fun saveWorkHours() {
        val groupedWorkHours = groupWorkHours()
        val database = FirebaseDatabase
            .getInstance("https://docappointassistbot-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("workHours")

        selectedWorkHourIds.clear()
        var savedCount = 0
        if (groupedWorkHours.isEmpty()) {
            handleSubmit()
            return
        }
        groupedWorkHours.forEach { wh ->
            val ref = database.push()
            ref.setValue(wh)
                .addOnSuccessListener {
                    ref.key?.let { key ->
                        selectedWorkHourIds.add(key)
                    }
                    savedCount++
                    if (savedCount == groupedWorkHours.size) {
                        handleSubmit()
                    }
                }
                .addOnFailureListener {
                    toast("Failed to save work hours.")
                }
        }
    }
}
