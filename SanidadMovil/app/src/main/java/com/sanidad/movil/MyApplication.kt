package com.sanidad.movil

import android.app.Application
import com.sanidad.movil.data.local.TokenDataStore
import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.repository.AuthRepository

class MyApplication : Application() {
    companion object {
        lateinit var instance: MyApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        val tokenDataStore = TokenDataStore(this)
        val authRepository = AuthRepository(tokenDataStore = tokenDataStore)
        NetworkModule.init(tokenDataStore, authRepository)
    }
}