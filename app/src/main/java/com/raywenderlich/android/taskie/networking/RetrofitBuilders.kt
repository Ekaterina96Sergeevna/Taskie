package com.raywenderlich.android.taskie.networking

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.logging.HttpLoggingInterceptor

// build the Retrofit client and API services

fun buildClient(): OkHttpClient =
    OkHttpClient.Builder()
        .addInterceptor (HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()


private val json = Json {
    // have a more forgiving parser
    isLenient = true
    ignoreUnknownKeys = true
}

fun buildRetrofit(): Retrofit{
    val contentType = "application/json".toMediaType()
    // toMedia - is helper to create a media type object from a string

    return Retrofit.Builder()
            .client(buildClient())
            .baseUrl(BASE_URL)
            // the JSON converter factory - parse everything automatically
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
}

// this function will create an Api Service
fun buildApiService(): RemoteApiService =
        buildRetrofit().create(RemoteApiService::class.java)