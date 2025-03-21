package mk.ukim.finki.docappointassistbot.domain

import java.util.Date

data class WorkHours(
    val daysOfWeek: String = "",
    val startTime: Date = Date(),
    val endTime: Date = Date(),
)
