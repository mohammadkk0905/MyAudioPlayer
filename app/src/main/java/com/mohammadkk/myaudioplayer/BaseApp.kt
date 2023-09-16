package com.mohammadkk.myaudioplayer

import android.app.Application

class BaseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        BaseSettings.initialize(applicationContext)
    }
}