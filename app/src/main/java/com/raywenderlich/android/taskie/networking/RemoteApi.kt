/*
 * Copyright (c) 2020 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * This project and source code may use libraries or frameworks that are
 * released under various Open-Source licenses. Use of those libraries and
 * frameworks are governed by their own individual licenses.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.raywenderlich.android.taskie.networking

import com.google.gson.Gson
import com.raywenderlich.android.taskie.App
import com.raywenderlich.android.taskie.model.Task
import com.raywenderlich.android.taskie.model.UserProfile
import com.raywenderlich.android.taskie.model.request.AddTaskRequest
import com.raywenderlich.android.taskie.model.request.UserDataRequest
import com.raywenderlich.android.taskie.model.response.GetTasksResponse
import com.raywenderlich.android.taskie.model.response.LoginResponse
import com.raywenderlich.android.taskie.model.response.UserProfileResponse
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

/**
 * Holds decoupled logic for all the API calls.
 */

const val BASE_URL = "https://taskie-rw.herokuapp.com"

                //this is constructor
class RemoteApi (private val remoteApiService: RemoteApiService){

    // create object Gson once
    private val gson = Gson()

  fun loginUser(userDataRequest: UserDataRequest, onUserLoggedIn: (String?, Throwable?) -> Unit) {
      val body = RequestBody.create(
              MediaType.parse("application/json"), gson.toJson(userDataRequest)
      )

      remoteApiService.loginUser(body).enqueue(object : Callback<ResponseBody> {
          override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {

              val jsonBody = response.body()?.string()
              if(jsonBody == null){
                  onUserLoggedIn(null, NullPointerException("No response body!"))
                  return
              }
              val loginResponse = gson.fromJson(jsonBody, LoginResponse::class.java)
              if(loginResponse == null || loginResponse.token.isNullOrBlank()){
                  onUserLoggedIn(null, NullPointerException("No response body!"))
              } else {
                  onUserLoggedIn(loginResponse.token, null)
              }
          }

          override fun onFailure(call: Call<ResponseBody>, error: Throwable) {
              onUserLoggedIn (null, error)
          }
      })
  }

  fun registerUser(userDataRequest: UserDataRequest, onUserCreated: (String?, Throwable?) -> Unit) {

      val body = RequestBody.create(
              MediaType.parse("application/json"), gson.toJson(userDataRequest)
      )

      // remoteApiService.registerUser(body).execute() - is blocking
      // give result, but we have own threading and error handling with try-catch
      // enqueue - nonblocking asynchronous way
      remoteApiService.registerUser(body).enqueue(object : Callback<ResponseBody> {
          override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {

              val message = response.body()?.string()
              if(message == null){
                  onUserCreated(null, NullPointerException("No response body!"))
                  return
              }
              onUserCreated(message, null)
          }

          override fun onFailure(call: Call<ResponseBody>, error: Throwable) {
              onUserCreated (null, error)
          }
      })
  }

  fun getTasks(onTasksReceived: (List<Task>, Throwable?) -> Unit) {
     remoteApiService.getNotes(App.getToken()).enqueue(object : Callback<ResponseBody> {
         override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
             val jsonBody = response.body()?.string()

             //check response body
             if(jsonBody == null){
                 onTasksReceived(emptyList(), NullPointerException("No data available!"))
                 return
             }

             //parse response body using JSON
             val data = gson.fromJson(jsonBody, GetTasksResponse::class.java)
             if(data != null && data.notes.isNotEmpty()){
                 onTasksReceived(data.notes.filter { !it.isCompleted }, null)
             } else {
                 onTasksReceived(emptyList(), NullPointerException("No data available!"))
             }
         }

         override fun onFailure(call: Call<ResponseBody>, error: Throwable) {
             onTasksReceived(emptyList(), error)
         }
     })
  }

  fun deleteTask(onTaskDeleted: (Throwable?) -> Unit) {
    onTaskDeleted(null)
  }

  fun completeTask(taskId: String, onTaskCompleted: (Throwable?) -> Unit) {
    Thread(Runnable {
                                                        // ?id=$taskId - a query (name states) specify what we want to do
                                                        // we want - complete the note with the queried ID
                                                        // $title=$noteTitle - compare both the task ID and the note title
        val connection = URL("$BASE_URL/api/note/complete?id=$taskId").openConnection() as HttpURLConnection
        //send the requestMethod
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Authorization", App.getToken())
        connection.readTimeout = 10000
        connection.connectTimeout = 10000
        connection.doOutput = true
        connection.doInput = true

        try {
            val reader = InputStreamReader(connection.inputStream)

            reader.use { input ->
                val response = StringBuilder()
                val bufferedReader = BufferedReader(input)

                bufferedReader.useLines {lines ->
                    lines.forEach {
                        response.append(it.trim())
                    }
                }
                onTaskCompleted(null)

            }
        }catch (error: Throwable){
            onTaskCompleted(error)

        }

        connection.disconnect()
    }).start()
  }

  fun addTask(addTaskRequest: AddTaskRequest, onTaskCreated: (Task?, Throwable?) -> Unit) {
      //avoid blocking main thread move API call in new thread
      Thread(Runnable {
          //open a connection to a specific URL
          val connection = URL("$BASE_URL/api/note").openConnection() as HttpURLConnection
          //send the requestMethod
          connection.requestMethod = "POST"
          connection.setRequestProperty("Content-Type", "application/json")
          connection.setRequestProperty("Accept", "application/json")
          connection.setRequestProperty("Authorization", App.getToken())
          connection.readTimeout = 10000
          connection.connectTimeout = 10000
          connection.doOutput = true
          connection.doInput = true

          /*
          val requestJson = JSONObject()
          requestJson.put("title", addTaskRequest.title)
          requestJson.put("content", addTaskRequest.content)
          requestJson.put("taskPriority", addTaskRequest.taskPriority)
           */

          val body = gson.toJson(addTaskRequest)

          // try catch block -> don't crash from writing or receiving data
          try {
              //sending data
              connection.outputStream.use { outputStream ->
                  outputStream.write(body.toByteArray())
              }

              //read response from the connections input stream
              val reader = InputStreamReader(connection.inputStream)
              reader.use { input ->
                  //use for safely consuming(use) and storing(keep) all the lines from reader to a StringBuilder()
                  val response = StringBuilder()
                  // BufferedReader - better, avoid overwhelming the program (very much work - program is tired)
                  val bufferedReader = BufferedReader(input)

                  bufferedReader.useLines {lines ->
                      lines.forEach {
                          response.append(it.trim())
                      }
                  }

                  val jsonObject = JSONObject(response.toString())

                  //create task from jsonObject
                  val task = Task(
                      jsonObject.getString("id"),
                      jsonObject.getString("title"),
                      jsonObject.getString("content"),
                      jsonObject.getBoolean("isCompleted"),
                      jsonObject.getInt("taskPriority"),
                      )

                  onTaskCreated(task, null)
              }
          }catch (error: Throwable){
              onTaskCreated(null, error)
          }
          connection.disconnect()
      }).start()
  }

  fun getUserProfile(onUserProfileReceived: (UserProfile?, Throwable?) -> Unit) {
      getTasks { tasks, error ->
          if (error != null && error !is NullPointerException) {
              onUserProfileReceived(null, error)
              return@getTasks
          }

          remoteApiService.getUserProfile(App.getToken()).enqueue(object : Callback<ResponseBody> {
              override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                  val jsonBody = response.body()?.string()

                  //check response body
                  if (jsonBody == null) {
                      onUserProfileReceived(null, error)
                      return
                  }

                  //parse response body using JSON
                  val userProfileResponse = gson.fromJson(jsonBody, UserProfileResponse::class.java)
                  if (userProfileResponse.email == null || userProfileResponse.name == null) {
                      onUserProfileReceived(null, error)
                  } else {
                      onUserProfileReceived(UserProfile(
                              userProfileResponse.email,
                              userProfileResponse.name,
                              tasks.size
                      ), null)
                  }
              }

              override fun onFailure(call: Call<ResponseBody>, error: Throwable) {
                  onUserProfileReceived(null, error)
              }
          })
      }
  }
}