package com.raywenderlich.android.taskie.model

// sealed class - if need create a type which only has a fix set of subtype
sealed class Result<out T: Any>

data class Success<out T : Any>(val data: T) : Result<T>()

data class Failure(val error: Throwable?) : Result<Nothing>()


