package mk.ukim.finki.docappointassistbot.domain

data class Specialty(
    val key: String,         // english name
    val nameResId: Int,      // translated name
    val imageResId: Int      // image
)