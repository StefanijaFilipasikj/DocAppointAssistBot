package mk.ukim.finki.docappointassistbot.domain

data class Appointment(
    val doctorId: Int = 0,
    val userId: String = "",
    val startTime: String = "",
    val endTime: String = "",
)
