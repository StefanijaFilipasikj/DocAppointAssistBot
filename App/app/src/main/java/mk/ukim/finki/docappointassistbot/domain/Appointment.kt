package mk.ukim.finki.docappointassistbot.domain

data class Appointment(
    val id: Int = 0,
    val doctorId: String = "",
    val userId: String = "",
    val startTime: String = "",
    val endTime: String = "",
    var status: String = "",
    var doctor: Doctor? = null,
)
