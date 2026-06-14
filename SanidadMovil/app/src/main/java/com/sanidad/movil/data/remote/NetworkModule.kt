package com.sanidad.movil.data.remote

import com.sanidad.movil.BuildConfig
import com.sanidad.movil.data.local.TokenDataStore
import com.sanidad.movil.data.repository.AuthRepository
import com.sanidad.movil.data.remote.api.ApiService
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    private var token: String? = null
    private lateinit var tokenDataStore: TokenDataStore
    private lateinit var authRepository: AuthRepository

    /** Debe llamarse en MyApplication.onCreate() */
    fun init(tokenDataStore: TokenDataStore, authRepository: AuthRepository) {
        this.tokenDataStore = tokenDataStore
        this.authRepository = authRepository
    }

    fun setToken(newToken: String?) {
        token = newToken
    }

    fun getToken(): String? = token

    private val tokenAuthenticator = object : Authenticator {
        override fun authenticate(route: Route?, response: Response): Request? {
            if (response.request.header("Authorization") != null) return null
            // No hay endpoint de refresco, así que forzamos logout
            authRepository.logout()
            return null
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                token?.let { requestBuilder.header("Authorization", "Bearer $it") }
                chain.proceed(requestBuilder.build())
            }
            .authenticator(tokenAuthenticator)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}