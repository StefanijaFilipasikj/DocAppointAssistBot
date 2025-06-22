package mk.ukim.finki.docappointassistbot.domain

data class DoctorRequest(
    val userId: String = "",
    val fullName: String = "",
    val profileImageUrl: String = "",
    val city: String = "",
    val country: String = "",
    val specialty: String = "",
    val bio: String = "",
    val experience: Double = 0.0,
    val hospitalId: Int? = null,
    val cvUrl: String = "",
    val workHourIds: List<String>? = null,
    val status: String = "Submitted"
)
