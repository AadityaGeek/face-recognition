package com.example.data

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface FaceRecognitionService {
    @GET("/health")
    suspend fun checkStatus(): Response<ResponseBody>

    @GET("/check-user-id")
    suspend fun checkUserId(@Query("user_id") userId: String): Response<ResponseBody>

    @GET("/user/{user_id}")
    suspend fun getUser(@Path("user_id") userId: String): Response<ResponseBody>

    @Multipart
    @POST("/register")
    suspend fun registerUser(
        @Part("name") name: RequestBody,
        @Part("age") age: RequestBody,
        @Part("user_id") userId: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<ResponseBody>

    @Multipart
    @POST("/verify")
    suspend fun verifyUser(
        @Part("user_id") userId: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<ResponseBody>
}

object FaceRecognitionApi {
    val service: FaceRecognitionService
        get() = TODO("Implementation hidden")
}
