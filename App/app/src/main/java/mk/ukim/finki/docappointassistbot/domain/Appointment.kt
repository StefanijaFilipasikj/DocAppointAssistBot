package mk.ukim.finki.docappointassistbot.domain

data class Appointment(
    val id: Int = 0,
    val doctorId: Int = 0,
    val userId: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val status: String = "",
    var doctor: Doctor? = null,
)
