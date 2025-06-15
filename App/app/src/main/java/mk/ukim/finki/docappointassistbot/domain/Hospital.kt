package mk.ukim.finki.docappointassistbot.domain

data class Hospital(
    val id: Int = 0,
    val name: String = "",
    val address: String = "",
    var latitude: Double? = 0.0,
    var longitude: Double? = 0.0,
)
