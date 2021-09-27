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

import com.raywenderlich.android.taskie.model.Failure
import com.raywenderlich.android.taskie.model.Result
import com.raywenderlich.android.taskie.model.Success
import com.raywenderlich.android.taskie.model.Task
import com.raywenderlich.android.taskie.model.UserProfile
import com.raywenderlich.android.taskie.model.request.AddTaskRequest
import com.raywenderlich.android.taskie.model.request.UserDataRequest
import com.raywenderlich.android.taskie.model.response.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

/**
 * Holds decoupled logic for all the API calls.
 */

const val BASE_URL = "https://taskie-rw.herokuapp.com"

                //this is constructor
class RemoteApi (private val remoteApiService: RemoteApiService){


  fun loginUser(userDataRequest: UserDataRequest, onUserLoggedIn: (Result<String>) -> Unit) {

      remoteApiService.loginUser(userDataRequest).enqueue(object : Callback<LoginResponse> {
          override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {

              val loginResponse = response.body()

              if(loginResponse == null || loginResponse.token.isNullOrEmpty()){
                  onUserLoggedIn(Failure(NullPointerException("No response body!")))
              } else {
                  onUserLoggedIn(Success(loginResponse.token))
              }
          }

          override fun onFailure(call: Call<LoginResponse>, error: Throwable) {
              onUserLoggedIn (Failure(error))
          }
      })
  }

  fun registerUser(userDataRequest: UserDataRequest, onUserCreated: (Result<String>) -> Unit) {

      remoteApiService.registerUser(userDataRequest).enqueue(object : Callback<RegisterResponse> {
          override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {

              val message = response.body()?.message
              if(message == null){
                  onUserCreated(Failure(NullPointerException("No response body!")))
                  return
              }
              onUserCreated(Success(message))
          }

          override fun onFailure(call: Call<RegisterResponse>, error: Throwable) {
              onUserCreated (Failure(error))
          }
      })
  }

  fun getTasks(onTasksReceived: (Result<List<Task>>) -> Unit) {
     remoteApiService.getNotes().enqueue(object : Callback<GetTasksResponse> {

         override fun onResponse(call: Call<GetTasksResponse>, response: Response<GetTasksResponse>) {

             val data = response.body()
             if(data != null){
                 onTasksReceived(Success(data.notes.filter { !it.isCompleted }))
             } else {
                 onTasksReceived(Failure(NullPointerException("No data available!")))
             }
         }

         override fun onFailure(call: Call<GetTasksResponse>, error: Throwable) {
             onTasksReceived(Failure(error))
         }
     })
  }

  fun deleteTask(onTaskDeleted: (Throwable?) -> Unit) {
    onTaskDeleted(null)
  }

  fun completeTask(taskId: String, onTaskCompleted: (Throwable?) -> Unit) {
    remoteApiService.completeTask(taskId).enqueue(object : Callback<CompleteNoteResponse>{
        override fun onResponse(call: Call<CompleteNoteResponse>, response: Response<CompleteNoteResponse>) {

            val completeNoteResponse = response.body()

            if(completeNoteResponse?.message == null) {
                onTaskCompleted(NullPointerException("No response!"))
            } else {
                onTaskCompleted(null)
            }
        }

        override fun onFailure(call: Call<CompleteNoteResponse>, error: Throwable) {
            onTaskCompleted(error)
        }
    })
  }

  fun addTask(addTaskRequest: AddTaskRequest, onTaskCreated: (Result<Task>) -> Unit) {

      remoteApiService.addTask(addTaskRequest).enqueue(object : Callback<Task>{
          override fun onResponse(call: Call<Task>, response: Response<Task>) {

              val data = response.body()

              if(data == null){
                  onTaskCreated(Failure(NullPointerException("No response!")))
              } else {
                  onTaskCreated(Success(data))
              }
          }

          override fun onFailure(call: Call<Task>, error: Throwable) {
            onTaskCreated(Failure(error))
          }

      })
  }

  fun getUserProfile(onUserProfileReceived: (Result<UserProfile>) -> Unit) {
      getTasks { result ->
          if (result is Failure && result.error !is NullPointerException) {
              onUserProfileReceived(Failure(result.error))
              return@getTasks
          }


          val tasks = result as Success

              remoteApiService.getUserProfile().enqueue(object : Callback<UserProfileResponse> {
                      override fun onResponse(call: Call<UserProfileResponse>, response: Response<UserProfileResponse>) {

                          //parse response body using JSON
                          val userProfileResponse = response.body()
                          if (userProfileResponse?.email == null || userProfileResponse.name == null) {
                              onUserProfileReceived(Failure(NullPointerException("No data!")))
                          } else {
                              onUserProfileReceived(Success(UserProfile(
                                      userProfileResponse.email,
                                      userProfileResponse.name,
                                      tasks.data.size)
                              ))
                          }
                      }

                      override fun onFailure(call: Call<UserProfileResponse>, error: Throwable) {
                          onUserProfileReceived(Failure(error))
                      }
                  })
      }
   }
}