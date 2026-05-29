package com.nicholasbergesen.gunsout

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. Seeding now happens inside [com.nicholasbergesen.gunsout.auth.SeederController]
 * which is driven by [com.nicholasbergesen.gunsout.feature.auth.AuthGate] after a successful Google
 * Sign-In; there is no per-launch bootstrap here.
 */
@HiltAndroidApp
class GunsoutApplication : Application()
