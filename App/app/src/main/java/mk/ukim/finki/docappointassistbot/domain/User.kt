package mk.ukim.finki.docappointassistbot.domain

data class User(
    val id: Int = 0,
    val email: String = "",
    val fullName: String = "",
    val photoUrl: String = "",
    val dateCreated: Long = 0L,
    val role: String = "patient"
)
