package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
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

    @GET("/users")
    suspend fun getAllUsers(): Response<ResponseBody>

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
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: FaceRecognitionService
        get() = Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(FaceRecognitionService::class.java)

    suspend fun checkStatusForUrl(baseUrl: String): Response<ResponseBody> {
        var formattedUrl = baseUrl.trim()
        if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
            formattedUrl = "http://$formattedUrl"
        }
        if (!formattedUrl.endsWith("/")) {
            formattedUrl = "$formattedUrl/"
        }
        val testClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(formattedUrl)
            .client(testClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(FaceRecognitionService::class.java)
            .checkStatus()
    }
}
