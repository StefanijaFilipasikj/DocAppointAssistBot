package mk.ukim.finki.docappointassistbot.retrofit

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ChatbotRetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8000/"

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}