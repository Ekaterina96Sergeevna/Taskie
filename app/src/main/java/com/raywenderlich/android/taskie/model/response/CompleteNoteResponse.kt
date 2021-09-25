package com.raywenderlich.android.taskie.model.response

import com.squareup.moshi.Json

// use to parse the response from the server
class CompleteNoteResponse(
        @field:Json(name = "message") val message :  String
        )