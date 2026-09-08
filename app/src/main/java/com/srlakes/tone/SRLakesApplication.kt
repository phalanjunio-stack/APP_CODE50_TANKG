package com.srlakes.tone

import android.app.Application
import com.srlakes.tone.di.AppContainer
import kotlinx.coroutines.launch

class SRLakesApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.appScope.launch {
            container.seedDemoDataIfNeeded()
        }
    }
}
