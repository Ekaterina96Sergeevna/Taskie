package com.raywenderlich.android.taskie.networking

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.raywenderlich.android.taskie.App
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor

private const val HEADER_AUTHORIZATION = "Authorization"

// build the Retrofit client and API services

fun buildClient(): OkHttpClient =
    OkHttpClient.Builder()
        .addInterceptor (HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .addInterceptor(buildAuthorizationInterceptor())
        .build()

// we create an interceptor, which receives interceptor chain
fun buildAuthorizationInterceptor() = object : Interceptor{
    override fun intercept(chain: Interceptor.Chain): Response {

        //check - token saved in the app?
        val originalRequest = chain.request()
        if(App.getToken().isBlank()) return chain.proceed(originalRequest)

        // if not - we proceed with originalRequest
        val new = originalRequest.newBuilder()
            .addHeader(HEADER_AUTHORIZATION, App.getToken())
            .build()

        // when we'll once log in we'll be authorized for all proceeding calls
        return  chain.proceed(new)
    }

}

private var json = Json {
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