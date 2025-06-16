package mk.ukim.finki.docappointassistbot.domain

data class Doctor(
    val id: String = "",
    val fullname: String = "",
    val image : String = "",
    val city: String = "",
    val country: String = "",
    val specialty: String = "",
    val bio: String = "",
    val patients: Int = 0,
    val experience: Double = 0.0,
    val reviews: Int = 0,
    val hospitalId: Int? = null,
    val workHourIds: List<Int>? = null,
    var hospital: Hospital? = null,
    var workHours: List<WorkHours>? = null
)
