package mk.ukim.finki.docappointassistbot.retrofit

import mk.ukim.finki.docappointassistbot.domain.PatientReport
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("embed")
    fun embedReport(@Body report: PatientReport): Call<Boolean>
}