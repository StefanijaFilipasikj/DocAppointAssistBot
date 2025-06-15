package mk.ukim.finki.docappointassistbot.utils

import android.location.Location
import androidx.fragment.app.Fragment
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.FirebaseDatabase
import mk.ukim.finki.docappointassistbot.domain.Doctor
import mk.ukim.finki.docappointassistbot.domain.Hospital

object DoctorLocationUtil {

    fun fetchDoctorsWithHospitals(callback: (List<Doctor>) -> Unit) {
        val doctorsRef = FirebaseDatabase
            .getInstance("https://docappointassistbot-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("doctors")

        doctorsRef.get().addOnSuccessListener { snapshot ->
            val totalDoctors = snapshot.children.count()
            if (totalDoctors == 0) {
                callback(emptyList())
                return@addOnSuccessListener
            }

            val tempDoctors = mutableListOf<Doctor>()
            var processedCount = 0

            for (doctorSnap in snapshot.children) {
                val doctor = doctorSnap.getValue(Doctor::class.java)
                if (doctor != null) {
                    if (doctor.hospitalId != null) {
                        fetchHospital(doctor.hospitalId) { hospital ->
                            doctor.hospital = hospital
                            tempDoctors.add(doctor)
                            processedCount++
                            if (processedCount == totalDoctors) callback(tempDoctors)
                        }
                    } else {
                        tempDoctors.add(doctor)
                        processedCount++
                        if (processedCount == totalDoctors) callback(tempDoctors)
                    }
                } else {
                    processedCount++
                    if (processedCount == totalDoctors) callback(tempDoctors)
                }
            }
        }
    }

    private fun fetchHospital(hospitalId: Int, callback: (Hospital?) -> Unit) {
        val hospitalRef = FirebaseDatabase
            .getInstance("https://docappointassistbot-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("hospitals")

        hospitalRef.child(hospitalId.toString()).get()
            .addOnSuccessListener { snapshot ->
                callback(snapshot.getValue(Hospital::class.java))
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    fun isDoctorNearby(
        userLat: Double,
        userLng: Double,
        doctor: Doctor,
        radiusMeters: Float = 100000f
    ): Boolean {
        val docLat = doctor.hospital?.latitude ?: return false
        val docLng = doctor.hospital?.longitude ?: return false

        val results = FloatArray(1)
        Location.distanceBetween(userLat, userLng, docLat, docLng, results)
        return results[0] <= radiusMeters
    }

    fun getUserLocation(fragment: Fragment, onLocation: (Location?) -> Unit) {
        val context = fragment.requireContext()
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location -> onLocation(location) }
                .addOnFailureListener { onLocation(null) }
        } catch (e: SecurityException) {
            onLocation(null)
        }
    }

    fun getClosestDoctors(
        fragment: Fragment,
        maxCount: Int = 3,
        callback: (List<Doctor>) -> Unit
    ) {
        getUserLocation(fragment) { location ->
            if (location == null) {
                callback(emptyList())
                return@getUserLocation
            }

            fetchDoctorsWithHospitals { doctors ->
                val sortedDoctors = doctors
                    .filter { it.hospital?.latitude != null && it.hospital?.longitude != null }
                    .sortedBy { doctor ->
                        val docLat = doctor.hospital!!.latitude
                        val docLng = doctor.hospital!!.longitude
                        val result = FloatArray(1)
                        Location.distanceBetween(
                            location.latitude, location.longitude,
                            docLat!!, docLng!!, result
                        )
                        result[0]
                    }
                    .take(maxCount)

                callback(sortedDoctors)
            }
        }
    }

}
