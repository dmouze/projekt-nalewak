package com.kierman.projektnalewak

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import com.kierman.projektnalewak.di.repositoryModule
import com.kierman.projektnalewak.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidFileProperties
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class MyApplication : Application() {



    init {
        instance = this
    }

    companion object {
        lateinit var instance: MyApplication
        fun applicationContext(): Context {
            return instance.applicationContext
        }
    }

    override fun onCreate() {
        super.onCreate()

        startKoin {
            // Sprawdzanie błędów - androidLogger(Level.ERROR)
            androidLogger(Level.ERROR)
            // Przekazanie kontekstu Androida
            androidContext(this@MyApplication)
            // Pobranie właściwości z pliku assets/koin.properties
            androidFileProperties()
            // Lista modułów
            modules(listOf(repositoryModule, viewModelModule))
        }

    }

}
