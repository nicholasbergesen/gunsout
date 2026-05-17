package com.gunsout

import android.app.Application
import com.gunsout.data.seed.Seeder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class GunsoutApplication : Application() {

    @Inject lateinit var seeder: Seeder

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            seeder.seedIfNeeded()
        }
    }
}
