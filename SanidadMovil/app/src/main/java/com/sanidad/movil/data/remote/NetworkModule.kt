package com.sanidad.movil.data.remote

import com.sanidad.movil.data.remote.api.ApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    private const val BASE_URL =
        "http://172.16.66.6:8080/"

    private var token: String? = null

    fun setToken(newToken: String?) {
        token = newToken
    }

    fun getToken(): String? = token

    private val okHttpClient: OkHttpClient by lazy {

        val logging = HttpLoggingInterceptor().apply {

            level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()

            .addInterceptor { chain ->

                val original = chain.request()

                val requestBuilder =
                    original.newBuilder()

                token?.let {

                    requestBuilder.header(
                        "Authorization",
                        "Bearer $it"
                    )
                }

                chain.proceed(
                    requestBuilder.build()
                )
            }

            .addInterceptor(logging)

            .connectTimeout(
                30,
                TimeUnit.SECONDS
            )

            .readTimeout(
                30,
                TimeUnit.SECONDS
            )

            .build()
    }

    val apiService: ApiService by lazy {

        Retrofit.Builder()

            .baseUrl(BASE_URL)

            .client(okHttpClient)

            .addConverterFactory(
                GsonConverterFactory.create()
            )

            .build()

            .create(ApiService::class.java)
    }
}