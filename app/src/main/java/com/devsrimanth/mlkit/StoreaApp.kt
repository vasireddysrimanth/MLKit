package com.devsrimanth.mlkit

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class StoreApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
