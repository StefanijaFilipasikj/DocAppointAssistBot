package mk.ukim.finki.docappointassistbot.domain

data class PatientReport (
    val text: String,
    val patient_id: String,
    val date: String,
)