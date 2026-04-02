package com.example.locus.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

object RetrofitClient {
    // For Android emulator, 10.0.2.2 maps to your machine's localhost 10.0.2.2:8080
    // For a real device, use your machine's local IP 10.245.247.209:8080
    private const val BASE_URL = "" + "http://10.18.247.166:8080/"

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}