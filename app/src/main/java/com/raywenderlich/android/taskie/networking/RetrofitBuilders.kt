package com.raywenderlich.android.taskie.networking

import okhttp3.OkHttpClient
import retrofit2.Retrofit

// build the Retrofit client and API services

fun buildClient(): OkHttpClient =
        OkHttpClient.Builder()
                .build()

fun buildRetrofit(): Retrofit{
    return Retrofit.Builder()
            .client(buildClient())
            .baseUrl(BASE_URL)
            .build()
}

// this function will create an Api Service
fun buildApiService(): RemoteApiService =
        buildRetrofit().create(RemoteApiService::class.java)