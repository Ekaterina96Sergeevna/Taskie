package com.raywenderlich.android.taskie.networking

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

// build the Retrofit client and API services

fun buildClient(): OkHttpClient =
        OkHttpClient.Builder()
                .build()

fun buildRetrofit(): Retrofit{
    return Retrofit.Builder()
            .client(buildClient())
            .baseUrl(BASE_URL)
            // converter will automatically parse the JSON and give object of the type we need
            // asLenient() - creates more forgiving parser (don't need to parse all the JSON fields, losing data isn't problem)
            .addConverterFactory(MoshiConverterFactory.create().asLenient())
            .build()
}

// this function will create an Api Service
fun buildApiService(): RemoteApiService =
        buildRetrofit().create(RemoteApiService::class.java)