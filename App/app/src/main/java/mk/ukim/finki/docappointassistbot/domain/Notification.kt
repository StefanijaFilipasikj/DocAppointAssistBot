package mk.ukim.finki.docappointassistbot.domain

data class Notification(
    val id: Int = 0,
    val appointment: Appointment,
    var isEnabled: Boolean = true
)