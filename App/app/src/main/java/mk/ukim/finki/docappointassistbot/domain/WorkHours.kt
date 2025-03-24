package mk.ukim.finki.docappointassistbot.domain

import com.google.firebase.database.Exclude
import com.google.firebase.database.PropertyName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WorkHours(
    val daysOfWeek: String = "",

    @get:Exclude @set:Exclude
    var startTime: Date = Date(),

    @get:Exclude @set:Exclude
    var endTime: Date = Date()
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)

    @PropertyName("startTime")
    fun getStartTimeString(): String {
        return dateFormat.format(startTime)
    }

    @PropertyName("startTime")
    fun setStartTimeString(value: String) {
        startTime = dateFormat.parse(value) ?: Date()
    }

    @PropertyName("endTime")
    fun getEndTimeString(): String {
        return dateFormat.format(endTime)
    }

    @PropertyName("endTime")
    fun setEndTimeString(value: String) {
        endTime = dateFormat.parse(value) ?: Date()
    }
}